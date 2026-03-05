package com.aquadev.journalservice.config.autohomework;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auto-homework")
public record AutoHomeworkProperties(long checkIntervalMs) {
}
