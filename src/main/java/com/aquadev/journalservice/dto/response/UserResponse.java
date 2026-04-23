package com.aquadev.journalservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        Long telegramId,
        String journalUsername,
        String fullName,
        boolean credentialsInvalid,
        Instant createdAt,
        Instant updatedAt
) {
}
