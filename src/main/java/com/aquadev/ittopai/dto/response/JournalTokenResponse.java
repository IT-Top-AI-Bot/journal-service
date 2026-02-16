package com.aquadev.ittopai.dto.response;

public record JournalTokenResponse(
        String accessToken,
        String refreshToken,
        Long expiresInRefresh,
        Long expiresInAccess,
        Integer userType,
        CityResponse cityData,
        String userRole) {
}
