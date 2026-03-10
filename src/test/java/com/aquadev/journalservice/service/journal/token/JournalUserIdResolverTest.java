package com.aquadev.journalservice.service.journal.token;

import com.aquadev.journalservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalUserIdResolverTest {

    @Mock StringRedisTemplate redis;
    @Mock UserRepository userRepository;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks
    JournalUserIdResolver resolver;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Test
    void resolve_cacheHit_returnsFromRedis() {
        when(valueOps.get(JournalRedisKeys.telegramToJournalMap(100L))).thenReturn("42");

        long result = resolver.resolve(100L);

        assertThat(result).isEqualTo(42L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void resolve_cacheMiss_queriesDbAndCaches() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(userRepository.findJournalUserIdByTelegramId(100L)).thenReturn(Optional.of(99L));

        long result = resolver.resolve(100L);

        assertThat(result).isEqualTo(99L);
        verify(valueOps).set(eq(JournalRedisKeys.telegramToJournalMap(100L)), eq("99"), any());
    }

    @Test
    void resolve_cacheMiss_userNotInDb_throwsIllegalState() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(userRepository.findJournalUserIdByTelegramId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found");
    }

    // ── put ───────────────────────────────────────────────────────────────────

    @Test
    void put_storesInRedis() {
        resolver.put(100L, 42L);
        verify(valueOps).set(eq(JournalRedisKeys.telegramToJournalMap(100L)), eq("42"), any());
    }

    // ── evict ─────────────────────────────────────────────────────────────────

    @Test
    void evict_deletesFromRedis() {
        resolver.evict(100L);
        verify(redis).delete(JournalRedisKeys.telegramToJournalMap(100L));
    }
}
