package com.aquadev.journalservice.service.autohomework;

import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import com.aquadev.journalservice.repository.JournalUserRepository;
import com.aquadev.journalservice.repository.UserAutoHomeworkSettingsRepository;
import com.aquadev.journalservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AutoHomeworkSettingsServiceImpl implements AutoHomeworkSettingsService {

    private final UserRepository userRepository;
    private final JournalUserRepository journalUserRepository;
    private final UserAutoHomeworkSettingsRepository settingsRepository;

    @Override
    @Transactional(readOnly = true)
    public AutoHomeworkSettingsResponse getSettings(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + telegramId));

        return settingsRepository.findByUserId(user.getId())
                .map(settings -> new AutoHomeworkSettingsResponse(
                        settings.isEnabled(),
                        settings.getLastCheckedAt(),
                        settings.getSpecIds()
                ))
                .orElse(new AutoHomeworkSettingsResponse(false, null, Set.of()));
    }

    @Override
    @Transactional
    public AutoHomeworkSettingsResponse updateSettings(Long telegramId, UpdateAutoHomeworkSettingsRequest request) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + telegramId));

        UserAutoHomeworkSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> UserAutoHomeworkSettings.builder().user(user).build());

        settings.setEnabled(request.enabled());
        if (request.enabled() && user.getJournalUser() != null) {
            user.getJournalUser().setCredentialsInvalid(false);
            journalUserRepository.save(user.getJournalUser());
        }

        settings.getSpecIds().clear();
        if (request.specIds() != null) {
            settings.getSpecIds().addAll(request.specIds());
        }

        UserAutoHomeworkSettings saved = settingsRepository.save(settings);
        return new AutoHomeworkSettingsResponse(saved.isEnabled(), saved.getLastCheckedAt(), saved.getSpecIds());
    }
}
