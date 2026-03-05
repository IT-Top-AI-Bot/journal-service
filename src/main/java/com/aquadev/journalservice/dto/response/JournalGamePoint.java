package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JournalGamePoint(
        @JsonProperty("new_gaming_point_types__id")
        Integer newGamingPointTypesId,
        Integer points
) {
}
