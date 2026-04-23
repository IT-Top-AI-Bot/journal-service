package com.aquadev.journalservice.service.autohomework;

import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;

public interface AutoHomeworkSettingsService {

    AutoHomeworkSettingsResponse getSettings(Long telegramId);

    AutoHomeworkSettingsResponse updateSettings(Long telegramId, UpdateAutoHomeworkSettingsRequest request);
}
