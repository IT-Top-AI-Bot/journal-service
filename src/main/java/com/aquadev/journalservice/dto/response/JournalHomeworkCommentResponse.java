package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JournalHomeworkCommentResponse(
        String textComment,
        Instant dateUpdated
) {
}
