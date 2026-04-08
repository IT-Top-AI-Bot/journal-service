package com.aquadev.journalservice.service.journal;

import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Long getCurrentGroupId() {
        Long telegramId = TelegramUserContext.get();
        User user = userRepository.findByTelegramIdWithGroups(telegramId)
                .orElseThrow(UserNotFoundException::new);

        JournalUser journalUser = Optional.ofNullable(user.getJournalUser())
                .orElseThrow(UserNotFoundException::new);

        return journalUser.getJournalGroups().stream()
                .map(JournalGroup::getJournalGroupId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Group not found for user: " + telegramId));
    }
}
