package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record JournalHomeworkUploadResponse(
        Long id,
        String fileName,
        String filePath,
        String tmpFile,
        Integer mark,
        LocalDate creationTime,
        @JsonProperty("stud_answer")
        String studentAnswer,
        Boolean autoMark
) {
}
