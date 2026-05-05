package com.aquadev.journalservice.service.journal.token;

import com.aquadev.journalservice.config.journal.JournalTokenProperties;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import com.aquadev.journalservice.exception.domain.journal.JournalAuthenticationException;
import com.aquadev.journalservice.model.JournalCredential;
import com.aquadev.journalservice.model.JournalToken;
import com.aquadev.journalservice.repository.JournalCredentialRepository;
import com.aquadev.journalservice.repository.JournalTokenRepository;
import com.aquadev.journalservice.service.journal.auth.JournalAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalTokenManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Duration SKEW = Duration.ofSeconds(30);
    private static final Duration LOCK_TTL = Duration.ofSeconds(20);

    private final TokenCrypto crypto;
    private final StringRedisTemplate redis;
    private final JournalTokenRepository repo;
    private final JournalAuthService journalAuthService;
    private final JournalTokenProperties tokenProperties;
    private final JournalCredentialRepository credentialRepository;

    public String getValidAccessToken(long journalUserId) {
        String cached = redis.opsForValue().get(JournalRedisKeys.accessToken(journalUserId));
        if (cached != null) return cached;

        JournalToken t = repo.findById(journalUserId).orElse(null);
        if (t == null) {
            return loginUnderLock(journalUserId, false);
        }

        if (t.isReauthRequired()) {
            throw JournalAuthenticationException.reauthRequired();
        }

        if (t.getAccessExpiresAt().isAfter(Instant.now().plus(refreshWindow()))) {
            String access = crypto.decrypt(t.getAccessTokenEnc());
            cacheAccess(journalUserId, access, t.getAccessExpiresAt());
            return access;
        }

        return loginUnderLock(journalUserId, false);
    }

    public String forceRefreshAccessToken(long journalUserId) {
        redis.delete(JournalRedisKeys.accessToken(journalUserId));
        return loginUnderLock(journalUserId, true);
    }

    private String loginUnderLock(long journalUserId, boolean force) {
        String lockKey = JournalRedisKeys.lock(journalUserId);
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(120));
            String cached = redis.opsForValue().get(JournalRedisKeys.accessToken(journalUserId));
            if (cached != null) return cached;
            return getValidAccessToken(journalUserId);
        }

        try {
            if (!force) {
                JournalToken t = repo.findById(journalUserId).orElse(null);
                if (t != null && !t.isReauthRequired()
                        && t.getAccessExpiresAt().isAfter(Instant.now().plus(refreshWindow()))) {
                    String access = crypto.decrypt(t.getAccessTokenEnc());
                    cacheAccess(journalUserId, access, t.getAccessExpiresAt());
                    return access;
                }
            }
            return reauthAndStore(journalUserId);
        } finally {
            redis.delete(lockKey);
        }
    }

    public void storeTokens(long journalUserId, JournalTokenResponse token) {
        JournalToken existing = repo.findById(journalUserId).orElseGet(() -> {
            JournalToken created = new JournalToken();
            created.setJournalUserId(journalUserId);
            return created;
        });

        applyTokens(existing, token);
        existing.setReauthRequired(false);
        repo.save(existing);

        cacheAccess(journalUserId, token.accessToken(), existing.getAccessExpiresAt());
    }

    private void applyTokens(JournalToken target, JournalTokenResponse token) {
        target.setAccessTokenEnc(crypto.encrypt(token.accessToken()));

        Instant accessExpiry = parseJwtExpiry(token.accessToken());
        if (accessExpiry == null) {
            accessExpiry = Instant.now().plus(defaultTokenTtl());
        }
        target.setAccessExpiresAt(accessExpiry);

        // refresh columns are NOT NULL in DB — store access token as placeholder
        String refreshToken = token.refreshToken();
        target.setRefreshTokenEnc(crypto.encrypt(refreshToken != null ? refreshToken : token.accessToken()));
        target.setRefreshExpiresAt(accessExpiry);
    }

    private Instant parseJwtExpiry(String jwt) {
        if (jwt == null || jwt.isBlank()) return null;
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return null;
        try {
            String padded = parts[1];
            if (padded.length() % 4 != 0) {
                padded = padded + "=".repeat(4 - padded.length() % 4);
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(padded);
            JsonNode payload = MAPPER.readTree(payloadBytes);
            JsonNode exp = payload.get("exp");
            if (exp == null || !exp.isNumber()) return null;
            return Instant.ofEpochSecond(exp.longValue());
        } catch (IOException | IllegalArgumentException e) {
            log.debug("Could not parse JWT expiry: {}", e.getMessage());
            return null;
        }
    }

    private String reauthAndStore(long journalUserId) {
        JournalCredential credential = credentialRepository.findByJournalUserId(journalUserId)
                .orElseThrow(() -> {
                    markReauthRequired(journalUserId);
                    return JournalAuthenticationException.missingCredentials();
                });

        JournalTokenResponse token;
        try {
            token = journalAuthService.login(credential.getUsername(), credential.getPassword());
        } catch (JournalAuthenticationException ex) {
            if (requiresCredentialUpdate(ex)) {
                markReauthRequired(journalUserId);
            }
            throw ex;
        }

        storeTokens(journalUserId, token);
        return token.accessToken();
    }

    private void markReauthRequired(long journalUserId) {
        repo.findById(journalUserId).ifPresent(token -> {
            token.setReauthRequired(true);
            repo.save(token);
        });
    }

    private boolean requiresCredentialUpdate(JournalAuthenticationException ex) {
        return ex.getReason() == JournalAuthenticationException.Reason.INVALID_CREDENTIALS
                || ex.getReason() == JournalAuthenticationException.Reason.MISSING_CREDENTIALS;
    }

    private void cacheAccess(long journalUserId, String access, Instant accessExp) {
        Duration ttl = Duration.between(Instant.now(), accessExp.minus(SKEW));
        if (!ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(JournalRedisKeys.accessToken(journalUserId), access, ttl);
        }
    }

    private Duration defaultTokenTtl() {
        return Duration.ofMillis(tokenProperties.refreshInterval());
    }

    private Duration refreshWindow() {
        return Duration.ofSeconds(tokenProperties.refreshBeforeExpiry());
    }
}
