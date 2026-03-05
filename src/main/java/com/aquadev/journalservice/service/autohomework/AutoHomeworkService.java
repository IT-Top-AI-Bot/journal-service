package com.aquadev.journalservice.service.autohomework;

import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;
import com.aquadev.journalservice.model.UserAutoHomeworkSettings;

public interface AutoHomeworkService {

    AutoHomeworkSettingsResponse getSettings(Long telegramId);

    AutoHomeworkSettingsResponse updateSettings(Long telegramId, UpdateAutoHomeworkSettingsRequest request);

    void checkAndDispatch(UserAutoHomeworkSettings settings);
}
