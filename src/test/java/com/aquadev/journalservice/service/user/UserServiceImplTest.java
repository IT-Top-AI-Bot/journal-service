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
import com.aquadev.journalservice.model.JournalCredential;
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.journal.token.JournalTokenManager;
import com.aquadev.journalservice.service.journal.token.JournalUserIdResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserMapper userMapper;
    @Mock JournalClient journalClient;
    @Mock UserRepository userRepository;
    @Mock JournalUserMapper journalUserMapper;
    @Mock JournalAuthClient journalAuthClient;
    @Mock JournalTokenManager journalTokenManager;
    @Mock JournalUserIdResolver journalUserIdResolver;

    @InjectMocks
    UserServiceImpl userService;

    private static final long TELEGRAM_ID = 12345L;

    // ── getMe ─────────────────────────────────────────────────────────────────

    @Test
    void getMe_returnUserFromContext() throws Exception {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        User result = ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> userService.getMe());

        assertThat(result).isSameAs(user);
    }

    // ── getUserByTelegramId ───────────────────────────────────────────────────

    @Test
    void getUserByTelegramId_found_returnsUser() {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        assertThat(userService.getUserByTelegramId(TELEGRAM_ID)).isSameAs(user);
    }

    @Test
    void getUserByTelegramId_notFound_throwsUserNotFoundException() {
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByTelegramId(TELEGRAM_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(TELEGRAM_ID));
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    void createUser_success_savesAndReturnsUser() throws Exception {
        when(userRepository.existsByTelegramId(TELEGRAM_ID)).thenReturn(false);

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                makeJwt(777L), "refresh", 86400L, 3600L, null, null, null);
        when(journalAuthClient.login("juser", "jpass")).thenReturn(tokenResponse);

        User user = buildUser();
        user.setJournalCredential(new JournalCredential());
        when(userMapper.toEntity(any())).thenReturn(user);

        JournalUserResponse journalUserResponse = mock(JournalUserResponse.class);
        when(journalClient.getCurrentUser()).thenReturn(journalUserResponse);
        when(journalUserMapper.toEntity(eq(journalUserResponse), eq(777L))).thenReturn(new JournalUser());

        when(userRepository.save(user)).thenReturn(user);

        CreateUserRequest request = new CreateUserRequest("juser", "jpass");

        User result = ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> userService.createUser(request));

        assertThat(result).isSameAs(user);
        verify(journalTokenManager).storeTokens(eq(777L), eq(tokenResponse));
        verify(journalUserIdResolver).put(TELEGRAM_ID, 777L);
        verify(userRepository).save(user);
    }

    @Test
    void createUser_alreadyExists_throwsUserAlreadyExistException() throws Exception {
        when(userRepository.existsByTelegramId(TELEGRAM_ID)).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest("juser", "jpass");

        assertThatThrownBy(() ->
                ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                        .call(() -> userService.createUser(request))
        ).isInstanceOf(UserAlreadyExistException.class)
                .hasMessageContaining(String.valueOf(TELEGRAM_ID));

        verifyNoInteractions(journalAuthClient);
    }

    @Test
    void createUser_journalLoginFails_exceptionPropagates() throws Exception {
        when(userRepository.existsByTelegramId(TELEGRAM_ID)).thenReturn(false);
        when(journalAuthClient.login(any(), any()))
                .thenThrow(new RuntimeException("Unauthorized"));

        CreateUserRequest request = new CreateUserRequest("bad", "creds");

        assertThatThrownBy(() ->
                ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                        .call(() -> userService.createUser(request))
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(journalClient, journalTokenManager, journalUserIdResolver);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setTelegramId(TELEGRAM_ID);
        return u;
    }

    private static String makeJwt(long userId) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"userId\":" + userId + "}").getBytes());
        return header + "." + payload + ".sig";
    }
}
