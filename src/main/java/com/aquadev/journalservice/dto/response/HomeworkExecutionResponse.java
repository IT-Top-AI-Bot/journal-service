package com.aquadev.journalservice.dto.response;

import com.aquadev.commonlibs.HomeworkExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HomeworkExecutionResponse(
        UUID id,
        Long homeworkId,
        Long specId,
        Long teachId,
        Long groupId,
        String teacherFio,
        String theme,
        LocalDate completionTime,
        LocalDate overdueTime,
        String comment,
        String nameSpec,
        String homeworkUrl,
        HomeworkExecutionStatus status,
        Instant createdAt,
        String resultS3Key,
        Instant completedAt
) {
}
