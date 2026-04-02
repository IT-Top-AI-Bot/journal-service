package com.aquadev.journalservice.service.journal.token;

import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import com.aquadev.journalservice.config.journal.JournalTokenProperties;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import com.aquadev.journalservice.model.JournalCredential;
import com.aquadev.journalservice.model.JournalToken;
import com.aquadev.journalservice.repository.JournalCredentialRepository;
import com.aquadev.journalservice.repository.JournalTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Service
@RequiredArgsConstructor
public class JournalTokenManager {

    private static final Duration SKEW = Duration.ofSeconds(30);
    private static final Duration LOCK_TTL = Duration.ofSeconds(20);

    private final JournalTokenRepository repo;
    private final JournalCredentialRepository credentialRepository;
    private final JournalAuthClient journalAuthClient;
    private final JournalTokenProperties tokenProperties;
    private final TokenCrypto crypto;
    private final StringRedisTemplate redis;

    public String getValidAccessToken(long journalUserId) {
        String cached = redis.opsForValue().get(JournalRedisKeys.accessToken(journalUserId));
        if (cached != null) return cached;

        JournalToken t = repo.findById(journalUserId).orElse(null);
        if (t == null) {
            return refreshUnderLock(journalUserId, false);
        }

        if (t.isReauthRequired()) {
            throw new IllegalStateException("Reauth required for journalUserId=" + journalUserId);
        }

        Instant now = Instant.now();
        if (t.getAccessExpiresAt().isAfter(now.plus(refreshWindow()))) {
            String access = crypto.decrypt(t.getAccessTokenEnc());
            cacheAccess(journalUserId, access, t.getAccessExpiresAt());
            return access;
        }

        return refreshUnderLock(journalUserId, false);
    }

    public String forceRefreshAccessToken(long journalUserId) {
        redis.delete(JournalRedisKeys.accessToken(journalUserId));
        return refreshUnderLock(journalUserId, true);
    }

    private String refreshUnderLock(long journalUserId, boolean forceRefresh) {
        String lockKey = JournalRedisKeys.lock(journalUserId);
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);

        if (!Boolean.TRUE.equals(locked)) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(120));
            String cached = redis.opsForValue().get(JournalRedisKeys.accessToken(journalUserId));
            if (cached != null) return cached;

            return getValidAccessToken(journalUserId);
        }

        try {
            JournalToken t = repo.findById(journalUserId).orElse(null);
            Instant now = Instant.now();

            if (t == null) {
                return reauthAndStore(journalUserId);
            }

            if (t.isReauthRequired()) {
                return reauthAndStore(journalUserId);
            }

            if (!forceRefresh && t.getAccessExpiresAt().isAfter(now.plus(refreshWindow()))) {
                String access = crypto.decrypt(t.getAccessTokenEnc());
                cacheAccess(journalUserId, access, t.getAccessExpiresAt());
                return access;
            }

            if (t.getRefreshExpiresAt().isBefore(now.plus(SKEW))) {
                return reauthAndStore(journalUserId);
            }

            String refresh = crypto.decrypt(t.getRefreshTokenEnc());
            JournalTokenResponse refreshed;
            try {
                refreshed = journalAuthClient.refreshToken(refresh);
            } catch (RuntimeException _) {
                return reauthAndStore(journalUserId);
            }

            storeTokens(journalUserId, refreshed);
            return refreshed.accessToken();
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

        Instant now = Instant.now();
        applyTokens(existing, token, now);
        existing.setReauthRequired(false);
        repo.save(existing);

        cacheAccess(journalUserId, token.accessToken(), existing.getAccessExpiresAt());
    }

    private void applyTokens(JournalToken target, JournalTokenResponse token, Instant now) {
        target.setAccessTokenEnc(crypto.encrypt(token.accessToken()));

        String refreshToken = token.refreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            target.setRefreshTokenEnc(crypto.encrypt(refreshToken));
        }

        target.setAccessExpiresAt(resolveExpiry(now, token.expiresInAccess()));
        target.setRefreshExpiresAt(resolveExpiry(now, token.expiresInRefresh()));
    }

    private Instant resolveExpiry(Instant now, Long expiresInSeconds) {
        if (expiresInSeconds == null || expiresInSeconds <= 0) {
            return now.plus(defaultTokenTtl());
        }

        if (expiresInSeconds > 604800L) {
            return now.plusMillis(expiresInSeconds);
        }

        return now.plusSeconds(expiresInSeconds);
    }

    private Duration defaultTokenTtl() {
        return Duration.ofMillis(tokenProperties.refreshInterval());
    }

    private Duration refreshWindow() {
        return Duration.ofSeconds(tokenProperties.refreshBeforeExpiry());
    }

    private String reauthAndStore(long journalUserId) {
        JournalCredential credential = credentialRepository.findByJournalUserId(journalUserId)
                .orElseThrow(() -> {
                    markReauthRequired(journalUserId);
                    return new IllegalStateException("Missing credentials for journalUserId=" + journalUserId);
                });

        JournalTokenResponse token;
        try {
            token = journalAuthClient.login(credential.getUsername(), credential.getPassword());
        } catch (RuntimeException ex) {
            markReauthRequired(journalUserId);
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

    private void cacheAccess(long journalUserId, String access, Instant accessExp) {
        Duration ttl = Duration.between(Instant.now(), accessExp.minus(SKEW));
        if (!ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(JournalRedisKeys.accessToken(journalUserId), access, ttl);
        }
    }
}
