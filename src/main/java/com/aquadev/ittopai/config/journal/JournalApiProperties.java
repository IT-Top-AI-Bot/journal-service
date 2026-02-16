package com.aquadev.ittopai.config.journal;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "journal.api")
public record JournalApiProperties(
        @NotNull String baseUrl,
        @NotNull String applicationKey,
        @NotNull Timeout timeout,
        @NotNull String userAgent,
        @NotNull String journalUrl
) {

    public record Timeout(
            @NotNull int connect,
            @NotNull int read
    ) {
    }
}
