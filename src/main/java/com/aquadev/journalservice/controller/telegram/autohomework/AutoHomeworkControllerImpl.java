package com.aquadev.journalservice.controller.telegram.autohomework;

import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;
import com.aquadev.journalservice.service.autohomework.AutoHomeworkService;
import com.aquadev.journalservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram/auto-homework")
public class AutoHomeworkControllerImpl implements AutoHomeworkController {

    private final AutoHomeworkService autoHomeworkService;

    @Override
    @GetMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    public AutoHomeworkSettingsResponse getSettings() {
        return autoHomeworkService.getSettings(SecurityUtil.getCurrentTelegramUserId());
    }

    @Override
    @PutMapping("/settings")
    @ResponseStatus(HttpStatus.OK)
    public AutoHomeworkSettingsResponse updateSettings(@RequestBody UpdateAutoHomeworkSettingsRequest request) {
        Long telegramId = TelegramUserContext.get();
        return autoHomeworkService.updateSettings(telegramId, request);
    }
}
