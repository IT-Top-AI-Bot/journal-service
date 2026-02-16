package com.aquadev.ittopai.dto.response;

public record JournalGroupResponse(
        Integer groupStatus,
        Boolean isPrimary,
        Long id,
        String name
) {
}
