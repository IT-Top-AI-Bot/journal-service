package com.aquadev.journalservice.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserToken(
        Long telegramUserId,
        Long journalUserId,
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        Boolean isExpired,
        Boolean needsRefresh,
        Instant lastRefreshed
) {
}
