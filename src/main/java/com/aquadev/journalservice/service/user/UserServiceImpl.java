package com.aquadev.journalservice.service.user;

import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.exception.domain.user.UserAlreadyExistException;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.mapper.JournalUserMapper;
import com.aquadev.journalservice.mapper.UserMapper;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.journal.token.JournalTokenManager;
import com.aquadev.journalservice.service.journal.token.JournalUserIdResolver;
import com.aquadev.journalservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JournalClient journalClient;
    private final UserRepository userRepository;
    private final JournalUserMapper journalUserMapper;
    private final JournalAuthClient journalAuthClient;
    private final JournalTokenManager journalTokenManager;
    private final JournalUserIdResolver journalUserIdResolver;

    @Override
    public User getMe() {
        Long telegramId = TelegramUserContext.get();
        return getUserByTelegramId(telegramId);
    }

    @Override
    public User getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User with telegramId " + telegramId + " not found"));
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        Long telegramId = TelegramUserContext.get();
        if (userRepository.existsByTelegramId(telegramId)) {
            throw new UserAlreadyExistException("User with telegramId " + telegramId + " already exists");
        }

        JournalTokenResponse token = journalAuthClient.login(request.getJournalUsername(), request.getJournalPassword());
        long journalUserId = JwtUtil.getUserIdFromJwt(token.accessToken());

        User user = userMapper.toEntity(request);
        user.setTelegramId(telegramId);
        user.getJournalCredential().setJournalUserId(journalUserId);
        journalTokenManager.storeTokens(journalUserId, token);
        journalUserIdResolver.put(telegramId, journalUserId);

        JournalUserResponse currentUser = journalClient.getCurrentUser();
        user.setJournalUser(journalUserMapper.toEntity(currentUser, journalUserId));
        return userRepository.save(user);
    }
}
