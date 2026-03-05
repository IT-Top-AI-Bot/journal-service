package com.aquadev.journalservice.dto.response;

import lombok.Builder;

@Builder
public record TelegramAuthTokenResponse(
        Long telegramId,
        String accessToken,
        String refreshToken
) {
}
