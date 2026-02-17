package com.aquadev.ittopai.service.user;

import com.aquadev.ittopai.client.journal.JournalClient;
import com.aquadev.ittopai.client.journal.auth.JournalAuthClient;
import com.aquadev.ittopai.dto.request.CreateUserRequest;
import com.aquadev.ittopai.dto.response.JournalTokenResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;
import com.aquadev.ittopai.exception.domain.user.UserAlreadyExistException;
import com.aquadev.ittopai.exception.domain.user.UserNotFoundException;
import com.aquadev.ittopai.mapper.JournalUserMapper;
import com.aquadev.ittopai.mapper.UserMapper;
import com.aquadev.ittopai.model.User;
import com.aquadev.ittopai.repository.UserRepository;
import com.aquadev.ittopai.service.journal.token.JournalTokenManager;
import com.aquadev.ittopai.service.journal.token.JournalUserIdResolver;
import com.aquadev.ittopai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JournalAuthClient journalAuthClient;
    private final UserRepository userRepository;
    private final JournalTokenManager journalTokenManager;
    private final JournalUserIdResolver journalUserIdResolver;
    private final UserMapper userMapper;
    private final JournalUserMapper journalUserMapper;
    private final JournalClient journalClient;

    @Override
    public User getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User with telegramId " + telegramId + " not found"));
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByTelegramId(request.getTelegramId())) {
            throw new UserAlreadyExistException("User with telegramId " + request.getTelegramId() + " already exists");
        }

        JournalTokenResponse token = journalAuthClient.login(request.getJournalUsername(), request.getJournalPassword());
        long journalUserId = JwtUtil.getUserIdFromJwt(token.accessToken());

        User user = userMapper.toEntity(request);
        user.setTelegramId(request.getTelegramId());
        user.getJournalCredential().setJournalUserId(journalUserId);
        journalTokenManager.storeTokens(journalUserId, token);
        journalUserIdResolver.put(request.getTelegramId(), journalUserId);

        JournalUserResponse currentUser = journalClient.getCurrentUser();
        user.setJournalUser(journalUserMapper.toEntity(currentUser, journalUserId));
        return userRepository.save(user);
    }
}
