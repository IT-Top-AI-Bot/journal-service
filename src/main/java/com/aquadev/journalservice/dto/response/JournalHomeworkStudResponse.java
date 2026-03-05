package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JournalHomeworkStudResponse(
        Long id,
        String filename,
        String filePath,
        String tmpFile,
        Integer mark,
        LocalDate creationTime,
        String studAnswer,
        boolean autoMark
) {
}
