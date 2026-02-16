package com.aquadev.ittopai.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        Long telegramId,
        String journalUsername,
        Instant createdAt,
        Instant updatedAt
) {
}
