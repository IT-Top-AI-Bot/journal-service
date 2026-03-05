package com.aquadev.journalservice.service.execution;

import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.s3.S3BucketProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.kafka.HomeworkExecutionResultEvent;
import com.aquadev.journalservice.dto.response.HomeworkExecutionStatus;
import com.aquadev.journalservice.exception.domain.homeworkexecution.HomeworkExecutionNotFoundException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExecutionResultServiceImpl implements HomeworkExecutionResultService {

    private final S3Client s3Client;
    private final JournalClient journalClient;
    private final ApplicationContext applicationContext;
    private final S3BucketProperties s3BucketProperties;
    private final HomeworkExecutionRepository homeworkExecutionRepository;

    private HomeworkExecutionResultServiceImpl self() {
        return applicationContext.getBean(HomeworkExecutionResultServiceImpl.class);
    }

    record ExecutionWithTelegramId(HomeworkExecution execution, Long telegramId) {}

    @Override
    public void handleEvent(HomeworkExecutionResultEvent event) {
        ExecutionWithTelegramId result = self().updateExecutionBaseInfo(event);

        try {
            TelegramUserContext.set(result.telegramId());

            processJournalUpload(result.execution());
        } catch (Exception e) {
            log.error("Failed to process journal upload for execution {}. Setting status to FAILED.", event.executionId(), e);
            self().updateStatusToFailed(event.executionId());
        } finally {
            TelegramUserContext.clear();
        }
    }

    @Transactional
    public ExecutionWithTelegramId updateExecutionBaseInfo(@NonNull HomeworkExecutionResultEvent event) {
        HomeworkExecution execution = homeworkExecutionRepository.findById(event.executionId())
                .orElseThrow(HomeworkExecutionNotFoundException::new);

        execution.setStatus(event.status());
        execution.setResultS3Key(event.resultS3Key());
        execution.setCompletedAt(event.createdAt());

        HomeworkExecution saved = homeworkExecutionRepository.save(execution);
        Long telegramId = saved.getUser().getTelegramId();

        log.info("HomeworkExecution {} basic info updated", event.executionId());
        return new ExecutionWithTelegramId(saved, telegramId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusToFailed(UUID executionId) {
        homeworkExecutionRepository.findById(executionId).ifPresent(ex -> {
            ex.setStatus(HomeworkExecutionStatus.FAILED);
            homeworkExecutionRepository.save(ex);
        });
    }

    private void processJournalUpload(@NonNull HomeworkExecution execution) {
//        if (journalClient.getHomeworkEvaluation(execution.getHomeworkId()).isPresent()) {
//            return;
//        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3BucketProperties.bucket())
                    .key(execution.getResultS3Key())
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
            byte[] content = responseBytes.asByteArray();
            long fileSize = content.length;

            try (InputStream inputStream = new ByteArrayInputStream(content)) {
                journalClient.uploadHomework(execution.getHomeworkId(), inputStream, fileSize);
                log.info("File for homework {} successfully streamed to journal", execution.getHomeworkId());
            }
        } catch (IOException e) {
            log.error("Streaming upload failed for execution {}: {}", execution.getId(), e.getMessage());
        }
    }
}
