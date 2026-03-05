package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JournalHomeworkResponse(
        Integer id,
        Integer idSpec,
        Integer idTeach,
        Integer idGroup,
        String fioTeach,
        String theme,
        LocalDate completionTime,
        LocalDate creationTime,
        LocalDate overdueTime,
        String filename,
        String filePath,
        String comment,
        String nameSpec,
        Integer status,
        Integer commonStatus,
        JournalHomeworkStudResponse homeworkStud,
        JournalHomeworkCommentResponse homeworkComment,
        String coverImage
) {
}
