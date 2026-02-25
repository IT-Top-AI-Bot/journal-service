package com.aquadev.ittopai.config.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        int batchSize,
        int maxAttempts,
        long retryDelaySeconds,
        long relayIntervalMs
) {
}
