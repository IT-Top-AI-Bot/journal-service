package com.aquadev.journalservice.service.user;

import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.exception.domain.user.UserAlreadyExistException;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.mapper.JournalUserMapper;
import com.aquadev.journalservice.mapper.UserMapper;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.journal.auth.JournalAuthService;
import com.aquadev.journalservice.service.journal.token.JournalTokenManager;
import com.aquadev.journalservice.service.journal.token.JournalUserIdResolver;
import com.aquadev.journalservice.util.JwtUtil;
import com.aquadev.journalservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JournalClient journalClient;
    private final UserRepository userRepository;
    private final JournalUserMapper journalUserMapper;
    private final JournalAuthService journalAuthService;
    private final JournalTokenManager journalTokenManager;
    private final JournalUserIdResolver journalUserIdResolver;

    @Override
    @Transactional(readOnly = true)
    public User getMe() {
        long telegramId = SecurityUtil.getCurrentTelegramUserId();
        User user = getUserByTelegramId(telegramId);
        Hibernate.initialize(user.getJournalUser());
        return user;
    }

    @Override
    public User getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User with telegramId " + telegramId + " not found"));
    }

    @Override
    @Transactional
    public User updateCredentials(CreateUserRequest request) {
        long telegramId = SecurityUtil.getCurrentTelegramUserId();
        User user = getUserByTelegramId(telegramId);

        JournalTokenResponse token = journalAuthService.login(request.getJournalUsername(), request.getJournalPassword());
        long journalUserId = JwtUtil.getUserIdFromJwt(token.accessToken());

        var cred = user.getJournalCredential();
        cred.setUsername(request.getJournalUsername());
        cred.setPassword(request.getJournalPassword());
        cred.setJournalUserId(journalUserId);
        if (user.getJournalUser() != null) {
            user.getJournalUser().setCredentialsInvalid(false);
        }
        journalTokenManager.storeTokens(journalUserId, token);
        journalUserIdResolver.put(telegramId, journalUserId);
        userRepository.saveAndFlush(user);

        JournalUserResponse currentUser = journalClient.getCurrentUser();
        user.setJournalUser(journalUserMapper.toEntity(currentUser, journalUserId));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        long telegramId = SecurityUtil.getCurrentTelegramUserId();
        if (userRepository.existsByTelegramId(telegramId)) {
            throw new UserAlreadyExistException("User with telegramId " + telegramId + " already exists");
        }

        JournalTokenResponse token = journalAuthService.login(request.getJournalUsername(), request.getJournalPassword());
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
