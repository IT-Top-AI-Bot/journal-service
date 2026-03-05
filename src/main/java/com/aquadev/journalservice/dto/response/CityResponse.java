package com.aquadev.journalservice.dto.response;

public record CityResponse(
        Long idCity,
        String prefix,
        String translateKey,
        String timezoneName,
        String countryCode,
        String marketStatus,
        String name
) {
}
