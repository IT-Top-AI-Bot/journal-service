package com.aquadev.journalservice.service.journal.token;

import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import com.aquadev.journalservice.config.journal.JournalTokenProperties;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import com.aquadev.journalservice.model.JournalCredential;
import com.aquadev.journalservice.model.JournalToken;
import com.aquadev.journalservice.repository.JournalCredentialRepository;
import com.aquadev.journalservice.repository.JournalTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalTokenManagerTest {

    @Mock JournalTokenRepository repo;
    @Mock JournalCredentialRepository credentialRepository;
    @Mock JournalAuthClient journalAuthClient;
    @Mock JournalTokenProperties tokenProperties;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    JournalTokenManager manager;
    TokenCrypto realCrypto;

    private static final long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Arrays.copyOf("test-secret-key!".getBytes(StandardCharsets.UTF_8), 16);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        realCrypto = new TokenCrypto(secretKey);

        manager = new JournalTokenManager(repo, credentialRepository, journalAuthClient,
                tokenProperties, realCrypto, redis);

        when(redis.opsForValue()).thenReturn(valueOps);
        when(tokenProperties.refreshInterval()).thenReturn(3_600_000L);
        when(tokenProperties.refreshBeforeExpiry()).thenReturn(60);
    }

    // ── getValidAccessToken: cache hit ─────────────────────────────────────

    @Test
    void getValidAccessToken_cacheHit_returnsCached() {
        when(valueOps.get(JournalRedisKeys.accessToken(USER_ID))).thenReturn("cached-token");

        String result = manager.getValidAccessToken(USER_ID);

        assertThat(result).isEqualTo("cached-token");
        verifyNoInteractions(repo);
    }

    // ── getValidAccessToken: token valid in DB — no lock needed ───────────

    @Test
    void getValidAccessToken_tokenValidInDb_returnsDecryptedAndCaches() {
        when(valueOps.get(JournalRedisKeys.accessToken(USER_ID))).thenReturn(null);

        String accessToken = "valid-access-token";
        JournalToken token = makeValidToken(accessToken, Instant.now().plusSeconds(7200));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(token));

        String result = manager.getValidAccessToken(USER_ID);

        assertThat(result).isEqualTo(accessToken);
        verify(valueOps).set(eq(JournalRedisKeys.accessToken(USER_ID)), eq(accessToken), any(Duration.class));
    }

    // ── getValidAccessToken: reauthRequired flag ──────────────────────────

    @Test
    void getValidAccessToken_reauthRequired_throwsIllegalState() {
        when(valueOps.get(JournalRedisKeys.accessToken(USER_ID))).thenReturn(null);

        JournalToken token = new JournalToken();
        token.setJournalUserId(USER_ID);
        token.setReauthRequired(true);
        token.setAccessExpiresAt(Instant.now().plusSeconds(3600));
        token.setRefreshExpiresAt(Instant.now().plusSeconds(86400));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> manager.getValidAccessToken(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reauth required");
    }

    // ── refreshUnderLock: lock contention ────────────────────────────────

    @Test
    void refreshUnderLock_waitPath_returnsCachedIfAvailable() {
        when(valueOps.get(JournalRedisKeys.accessToken(USER_ID)))
                .thenReturn(null) // First call in getValidAccessToken
                .thenReturn("newly-cached-token"); // After sleep

        when(valueOps.setIfAbsent(eq(JournalRedisKeys.lock(USER_ID)), any(), any()))
                .thenReturn(Boolean.FALSE);

        String result = manager.getValidAccessToken(USER_ID);

        assertThat(result).isEqualTo("newly-cached-token");
    }

    // ── refreshUnderLock: refresh token failure ──────────────────────────

    @Test
    void refreshUnderLock_refreshTokenFails_reauths() {
        when(valueOps.setIfAbsent(anyString(), any(), any())).thenReturn(Boolean.TRUE);

        JournalToken token = makeValidToken("old", Instant.now().minusSeconds(10)); // Access expired
        when(repo.findById(USER_ID)).thenReturn(Optional.of(token));

        when(journalAuthClient.refreshToken(anyString())).thenThrow(new RuntimeException("Refresh failed"));
        
        JournalCredential cred = new JournalCredential();
        cred.setUsername("u");
        cred.setPassword("p");
        when(credentialRepository.findByJournalUserId(USER_ID)).thenReturn(Optional.of(cred));

        JournalTokenResponse loginResp = new JournalTokenResponse("login-acc", "login-ref", 100L, 100L, null, null, null);
        when(journalAuthClient.login(anyString(), anyString())).thenReturn(loginResp);

        String result = manager.getValidAccessToken(USER_ID);

        assertThat(result).isEqualTo("login-acc");
        verify(journalAuthClient).login("u", "p");
    }

    // ── reauthAndStore: failure ──────────────────────────────────────────

    @Test
    void reauthAndStore_missingCredentials_throwsException() {
        when(valueOps.setIfAbsent(anyString(), any(), any())).thenReturn(Boolean.TRUE);
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());
        when(credentialRepository.findByJournalUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manager.getValidAccessToken(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing credentials");
    }

    @Test
    void reauthAndStore_loginFails_marksReauthRequiredAndThrows() {
        when(valueOps.setIfAbsent(anyString(), any(), any())).thenReturn(Boolean.TRUE);
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());

        JournalCredential cred = new JournalCredential();
        cred.setUsername("u");
        cred.setPassword("p");
        when(credentialRepository.findByJournalUserId(USER_ID)).thenReturn(Optional.of(cred));

        when(journalAuthClient.login(anyString(), anyString())).thenThrow(new RuntimeException("Login failed"));

        assertThatThrownBy(() -> manager.getValidAccessToken(USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Login failed");
    }

    // ── storeTokens ───────────────────────────────────────────────────────

    @Test
    void storeTokens_createsNewToken_whenNoneExists() {
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "access123", "refresh123", 604800L, 3600L, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        verify(repo).save(any(JournalToken.class));
        verify(valueOps).set(eq(JournalRedisKeys.accessToken(USER_ID)), eq("access123"), any(Duration.class));
    }

    @Test
    void storeTokens_updatesExistingToken() {
        JournalToken existing = makeValidToken("old-access", Instant.now().plusSeconds(3600));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(existing));

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "new-access", "new-refresh", 604800L, 3600L, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        assertThat(existing.isReauthRequired()).isFalse();
        verify(repo).save(existing);
    }

    // ── forceRefreshAccessToken ───────────────────────────────────────────

    @Test
    void forceRefreshAccessToken_deletesAccessCacheAndRefreshes() {
        when(valueOps.setIfAbsent(eq(JournalRedisKeys.lock(USER_ID)), any(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        JournalToken token = makeValidToken("old-access", Instant.now().plusSeconds(7200));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(token));

        JournalTokenResponse refreshResponse = new JournalTokenResponse(
                "forced-new-access", "new-refresh", 86400L, 3600L, null, null, null);
        when(journalAuthClient.refreshToken(anyString())).thenReturn(refreshResponse);

        String result = manager.forceRefreshAccessToken(USER_ID);

        assertThat(result).isEqualTo("forced-new-access");
        verify(redis).delete(JournalRedisKeys.accessToken(USER_ID));
    }

    // ── resolveExpiry ───────────────────────────────────────────────────

    @Test
    void storeTokens_expiresInNull_usesDefaultTtl() {
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());
        when(tokenProperties.refreshInterval()).thenReturn(1000L);

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "acc", "ref", null, null, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        verify(repo).save(argThat(t -> t.getAccessExpiresAt() != null));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private JournalToken makeValidToken(String accessToken, Instant accessExpiresAt) {
        JournalToken token = new JournalToken();
        token.setJournalUserId(USER_ID);
        token.setAccessTokenEnc(realCrypto.encrypt(accessToken));
        token.setRefreshTokenEnc(realCrypto.encrypt("refresh-token"));
        token.setAccessExpiresAt(accessExpiresAt);
        token.setRefreshExpiresAt(Instant.now().plusSeconds(86400));
        token.setReauthRequired(false);
        return token;
    }
}
