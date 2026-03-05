package com.aquadev.journalservice.config.journal;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "journal.token")
public record JournalTokenProperties(
        @NotNull Long refreshInterval,
        @NotNull Integer refreshBeforeExpiry,
        @NotNull Integer maxRetryAttempts
) {
}
