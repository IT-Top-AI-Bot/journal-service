package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JournalSpecResponse(
        Integer id,
        String name,
        String shortName
) {
}
