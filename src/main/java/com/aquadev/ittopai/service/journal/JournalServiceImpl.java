package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.client.journal.JournalClient;
import com.aquadev.ittopai.config.telegram.TelegramUserContext;
import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;
import com.aquadev.ittopai.exception.domain.user.UserNotFoundException;
import com.aquadev.ittopai.model.JournalGroup;
import com.aquadev.ittopai.model.User;
import com.aquadev.ittopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalClient journalClient;
    private final UserRepository userRepository;

    @Override
    public List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type) {
        Long telegramId = TelegramUserContext.get();
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(UserNotFoundException::new);

        Long groupIdToUse = user.getJournalUser().getJournalGroups().stream()
                .map(JournalGroup::getJournalGroupId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return journalClient.getHomeworks(page, status, type, groupIdToUse.intValue());
    }
}
