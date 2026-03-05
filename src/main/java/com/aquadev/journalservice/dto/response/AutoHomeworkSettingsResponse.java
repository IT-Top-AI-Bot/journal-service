package com.aquadev.journalservice.dto.response;

import java.time.Instant;
import java.util.Set;

public record AutoHomeworkSettingsResponse(boolean enabled, Instant lastCheckedAt, Set<Long> specIds) {
}
