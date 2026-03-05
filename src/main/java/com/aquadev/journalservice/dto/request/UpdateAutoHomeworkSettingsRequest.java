package com.aquadev.journalservice.dto.request;

import java.util.Set;

public record UpdateAutoHomeworkSettingsRequest(boolean enabled, Set<Long> specIds) {
}
