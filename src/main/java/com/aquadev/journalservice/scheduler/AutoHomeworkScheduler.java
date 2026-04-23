package com.aquadev.journalservice.scheduler;

import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import com.aquadev.journalservice.repository.UserAutoHomeworkSettingsRepository;
import com.aquadev.journalservice.service.autohomework.AutoHomeworkDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoHomeworkScheduler {

    private final AutoHomeworkDispatchService autoHomeworkDispatchService;
    private final UserAutoHomeworkSettingsRepository settingsRepository;

    @Scheduled(fixedRateString = "${auto-homework.check-interval-ms:21600000}")
    public void run() {
        List<UserAutoHomeworkSettings> enabled = settingsRepository.findAllEnabledWithUserData();
        log.info("Auto homework scheduler: processing {} enabled user(s)", enabled.size());
        for (UserAutoHomeworkSettings settings : enabled) {
            autoHomeworkDispatchService.checkAndDispatch(settings);
        }
    }
}
