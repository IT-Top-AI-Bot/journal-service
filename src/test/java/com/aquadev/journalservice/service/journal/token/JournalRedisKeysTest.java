package com.aquadev.journalservice.service.journal.token;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JournalRedisKeysTest {

    @Test
    void accessToken_containsUserId() {
        String key = JournalRedisKeys.accessToken(42L);
        assertThat(key).startsWith("jt:acc:").contains("42");
    }

    @Test
    void lock_containsUserId() {
        String key = JournalRedisKeys.lock(99L);
        assertThat(key).startsWith("jt:lock:").contains("99");
    }

    @Test
    void telegramToJournalMap_containsTelegramId() {
        String key = JournalRedisKeys.telegramToJournalMap(123456789L);
        assertThat(key).startsWith("jt:tg:map:").contains("123456789");
    }

    @Test
    void differentUserIds_produceDifferentKeys() {
        assertThat(JournalRedisKeys.accessToken(1L))
                .isNotEqualTo(JournalRedisKeys.accessToken(2L));
    }

    @Test
    void accessToken_and_lock_haveDistinctPrefixes() {
        long userId = 10L;
        assertThat(JournalRedisKeys.accessToken(userId))
                .isNotEqualTo(JournalRedisKeys.lock(userId));
    }
}
