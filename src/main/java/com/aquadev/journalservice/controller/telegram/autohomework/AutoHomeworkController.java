package com.aquadev.journalservice.controller.telegram.autohomework;

import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;

public interface AutoHomeworkController {

    AutoHomeworkSettingsResponse getSettings();

    AutoHomeworkSettingsResponse updateSettings(UpdateAutoHomeworkSettingsRequest request);
}
