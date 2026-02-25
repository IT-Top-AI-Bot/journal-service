package com.aquadev.ittopai.dto.kafka;

import com.aquadev.ittopai.dto.response.HomeworkExecutionStatus;
import com.aquadev.ittopai.model.HomeworkExecution;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HomeworkExecutionEvent(
        UUID id,
        String theme,
        Long specId,
        HomeworkExecutionStatus status,
        String comment,
        Long groupId,
        Long teachId,
        String nameSpec,
        Instant createdAt,
        Long homeworkId,
        String teacherFio,
        String homeworkUrl,
        LocalDate overdueTime,
        LocalDate completionTime
) {
    public static HomeworkExecutionEvent from(HomeworkExecution e) {
        return new HomeworkExecutionEvent(
                e.getId(),
                e.getTheme(),
                e.getSpecId(),
                e.getStatus(),
                e.getComment(),
                e.getGroupId(),
                e.getTeachId(),
                e.getNameSpec(),
                e.getCreatedAt(),
                e.getHomeworkId(),
                e.getTeacherFio(),
                e.getHomeworkUrl(),
                e.getOverdueTime(),
                e.getCompletionTime()
        );
    }
}
