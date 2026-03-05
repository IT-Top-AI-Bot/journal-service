package com.aquadev.journalservice.dto.kafka;

import com.aquadev.journalservice.dto.response.HomeworkExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record HomeworkExecutionResultEvent(
        UUID executionId,
        HomeworkExecutionStatus status,
        String resultS3Key,
        String errorMessage,
        Instant createdAt
) {
    public static HomeworkExecutionResultEvent completed(
            UUID executionId,
            String s3Key
    ) {
        return new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.DONE,
                s3Key,
                null,
                Instant.now()
        );
    }

    public static HomeworkExecutionResultEvent failed(UUID executionId, String errorMessage) {
        return new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.FAILED,
                null,
                errorMessage,
                Instant.now()
        );
    }
}
