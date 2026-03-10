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
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        // Token expires in 2h, refreshWindow is 60s → valid, no refresh path entered
        JournalToken token = makeValidToken(accessToken, Instant.now().plusSeconds(7200));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(token));

        String result = manager.getValidAccessToken(USER_ID);

        assertThat(result).isEqualTo(accessToken);
        // Result is cached via cacheAccess()
        verify(valueOps).set(eq(JournalRedisKeys.accessToken(USER_ID)), eq(accessToken), any());
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

    // ── getValidAccessToken: no token in DB → reauth ──────────────────────

    @Test
    void getValidAccessToken_noTokenInDb_reauthsAndReturnsToken() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.setIfAbsent(eq(JournalRedisKeys.lock(USER_ID)), any(), any()))
                .thenReturn(Boolean.TRUE);
        // All findById calls (initial check + inside refreshUnderLock + storeTokens) return empty
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());

        JournalCredential cred = new JournalCredential();
        cred.setUsername("user");
        cred.setPassword("pass");
        when(credentialRepository.findByJournalUserId(USER_ID)).thenReturn(Optional.of(cred));

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "new-access-token", "new-refresh-token", 86400L, 3600L, null, null, null);
        when(journalAuthClient.login("user", "pass")).thenReturn(tokenResponse);

        String result = manager.getValidAccessToken(USER_ID);

        assertThat(result).isEqualTo("new-access-token");
        verify(redis).delete(JournalRedisKeys.lock(USER_ID));
    }

    // ── storeTokens ───────────────────────────────────────────────────────

    @Test
    void storeTokens_createsNewToken_whenNoneExists() {
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "access123", "refresh123", 604800L, 3600L, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        verify(repo).save(any(JournalToken.class));
        verify(valueOps).set(eq(JournalRedisKeys.accessToken(USER_ID)), eq("access123"), any());
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

    @Test
    void storeTokens_nullRefreshToken_doesNotUpdateRefreshEnc() {
        JournalToken existing = makeValidToken("old-access", Instant.now().plusSeconds(3600));
        String originalRefreshEnc = existing.getRefreshTokenEnc();
        when(repo.findById(USER_ID)).thenReturn(Optional.of(existing));

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "new-access", null, 604800L, 3600L, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        assertThat(existing.getRefreshTokenEnc()).isEqualTo(originalRefreshEnc);
    }

    @Test
    void storeTokens_blankRefreshToken_doesNotUpdateRefreshEnc() {
        JournalToken existing = makeValidToken("old-access", Instant.now().plusSeconds(3600));
        String originalRefreshEnc = existing.getRefreshTokenEnc();
        when(repo.findById(USER_ID)).thenReturn(Optional.of(existing));

        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "new-access", "   ", 604800L, 3600L, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        assertThat(existing.getRefreshTokenEnc()).isEqualTo(originalRefreshEnc);
    }

    // ── forceRefreshAccessToken ───────────────────────────────────────────

    @Test
    void forceRefreshAccessToken_deletesAccessCacheAndRefreshes() {
        when(valueOps.setIfAbsent(eq(JournalRedisKeys.lock(USER_ID)), any(), any()))
                .thenReturn(Boolean.TRUE);

        // Token is valid, but force=true → refresh token is used
        JournalToken token = makeValidToken("old-access", Instant.now().plusSeconds(7200));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(token));

        JournalTokenResponse refreshResponse = new JournalTokenResponse(
                "forced-new-access", "new-refresh", 86400L, 3600L, null, null, null);
        when(journalAuthClient.refreshToken(anyString())).thenReturn(refreshResponse);

        String result = manager.forceRefreshAccessToken(USER_ID);

        assertThat(result).isEqualTo("forced-new-access");
        verify(redis).delete(JournalRedisKeys.accessToken(USER_ID));
    }

    // ── resolveExpiry edge case ───────────────────────────────────────────

    @Test
    void storeTokens_expiresInLargerThan604800_treatedAsMillis() {
        // BUG: resolveExpiry treats values > 604800 as millis.
        // expiresInAccess=604801 → now + 604801ms ≈ 10 minutes, NOT ~7 days.
        JournalToken existing = new JournalToken();
        existing.setJournalUserId(USER_ID);
        existing.setRefreshTokenEnc(realCrypto.encrypt("refresh"));
        existing.setRefreshExpiresAt(Instant.now().plusSeconds(86400));
        when(repo.findById(USER_ID)).thenReturn(Optional.of(existing));

        long largeValue = 604_801L;
        JournalTokenResponse tokenResponse = new JournalTokenResponse(
                "access", "refresh", 604800L, largeValue, null, null, null);

        manager.storeTokens(USER_ID, tokenResponse);

        Instant accessExpiry = existing.getAccessExpiresAt();
        Instant tenMinutesFromNow = Instant.now().plusSeconds(700);
        assertThat(accessExpiry).isBefore(tenMinutesFromNow);
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
