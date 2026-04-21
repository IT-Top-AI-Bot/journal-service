package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.s3.S3BucketProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.exception.domain.homeworkexecution.HomeworkExecutionNotFoundException;
import com.aquadev.journalservice.exception.domain.journal.JournalCredentialsInvalidException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.service.execution.HomeworkExecutionPersistenceService.ExecutionWithTelegramId;
import com.aquadev.journalservice.service.journal.credential.JournalCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExecutionResultServiceImpl implements HomeworkExecutionResultService {

    private final S3Client s3Client;
    private final JournalClient journalClient;
    private final S3BucketProperties s3BucketProperties;
    private final JournalCredentialService journalCredentialService;
    private final HomeworkExecutionPersistenceService persistenceService;

    @Override
    public void handleEvent(HomeworkExecutionResultEvent event) {
        ExecutionWithTelegramId result;
        try {
            result = persistenceService.updateExecutionBaseInfo(event);
        } catch (HomeworkExecutionNotFoundException _) {
            log.warn("Skipping homework-result event: execution {} not found, possibly a stale message", event.executionId());
            return;
        }

        if (event.status() == HomeworkExecutionStatus.FAILED) {
            log.warn("Execution {} completed with FAILED status, skipping journal upload", event.executionId());
            return;
        }

        try {
            ScopedValue.where(TelegramUserContext.TG_USER_ID, result.telegramId()).call(() -> {
                processJournalUpload(result.execution());
                return null;
            });
        } catch (S3Exception e) {
            if (e.statusCode() >= 500) {
                log.warn("Transient S3 error (status={}) for execution {}, rethrowing for Kafka retry",
                        e.statusCode(), event.executionId());
                throw e;
            }
            log.error("Permanent S3 error for execution {}. Setting status to FAILED.", event.executionId(), e);
            persistenceService.updateStatusToFailed(event.executionId());
        } catch (Exception e) {
            if (isInvalidCredentialsError(e)) {
                log.warn("Invalid credentials when uploading homework for telegramId={}, marking invalid", result.telegramId());
                journalCredentialService.markCredentialsInvalid(result.telegramId());
            }
            log.error("Failed to process journal upload for execution {}. Setting status to FAILED.", event.executionId(), e);
            persistenceService.updateStatusToFailed(event.executionId());
        }
    }

    @Override
    public void updateStatusToFailed(UUID executionId) {
        persistenceService.updateStatusToFailed(executionId);
    }

    private static boolean isInvalidCredentialsError(Exception e) {
        if (e instanceof JournalCredentialsInvalidException) {
            return true;
        }
        if (e instanceof HttpClientErrorException.UnprocessableContent uce) {
            return uce.getResponseBodyAsString().contains("Неверный логин или пароль");
        }
        return false;
    }

    private void processJournalUpload(@NonNull HomeworkExecution execution) throws IOException {
        if (execution.getResultText() != null && !execution.getResultText().isBlank()) {
            journalClient.uploadHomeworkText(execution.getHomeworkId(), execution.getResultText());
            log.info("Text result for homework {} submitted to journal", execution.getHomeworkId());
            return;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3BucketProperties.bucket())
                .key(execution.getResultS3Key())
                .build();

        String s3Key = execution.getResultS3Key();
        int uuidStart = s3Key.indexOf('-') + 1;
        String filename = s3Key.substring(uuidStart + 36 + 1);

        try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getObjectRequest)) {
            Long fileSize = s3Stream.response().contentLength();
            if (fileSize == null) {
                throw new IllegalStateException("Missing content length for S3 key=" + execution.getResultS3Key());
            }

            journalClient.uploadHomework(execution.getHomeworkId(), s3Stream, fileSize, filename);
            log.info("File for homework {} successfully streamed to journal: filename={}, size={} bytes",
                    execution.getHomeworkId(), filename, fileSize);
        }
    }
}
