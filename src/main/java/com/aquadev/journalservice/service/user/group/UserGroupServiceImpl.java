package com.aquadev.journalservice.service.user.group;

import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Long getCurrentGroupId() {
        long telegramId = SecurityUtil.getCurrentTelegramUserId();
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
