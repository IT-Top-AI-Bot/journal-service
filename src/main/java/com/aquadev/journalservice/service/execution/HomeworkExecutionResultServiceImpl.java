package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.s3.S3BucketProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.service.execution.HomeworkExecutionPersistenceService.ExecutionWithTelegramId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExecutionResultServiceImpl implements HomeworkExecutionResultService {

    private final S3Client s3Client;
    private final JournalClient journalClient;
    private final S3BucketProperties s3BucketProperties;
    private final HomeworkExecutionPersistenceService persistenceService;

    @Override
    public void handleEvent(HomeworkExecutionResultEvent event) {
        ExecutionWithTelegramId result = persistenceService.updateExecutionBaseInfo(event);

        if (event.status() == HomeworkExecutionStatus.FAILED) {
            log.warn("Execution {} completed with FAILED status, skipping journal upload", event.executionId());
            return;
        }

        try {
            ScopedValue.where(TelegramUserContext.TG_USER_ID, result.telegramId()).call(() -> {
                processJournalUpload(result.execution());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to process journal upload for execution {}. Setting status to FAILED.", event.executionId(), e);
            persistenceService.updateStatusToFailed(event.executionId());
        }
    }

    private void processJournalUpload(@NonNull HomeworkExecution execution) {
        if (execution.getResultText() != null && !execution.getResultText().isBlank()) {
            journalClient.uploadHomeworkText(execution.getHomeworkId(), execution.getResultText());
            log.info("Text result for homework {} submitted to journal", execution.getHomeworkId());
            return;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3BucketProperties.bucket())
                .key(execution.getResultS3Key())
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
        byte[] content = responseBytes.asByteArray();
        long fileSize = content.length;

        journalClient.uploadHomework(execution.getHomeworkId(), new ByteArrayInputStream(content), fileSize);
        log.info("File for homework {} successfully streamed to journal", execution.getHomeworkId());
    }
}
