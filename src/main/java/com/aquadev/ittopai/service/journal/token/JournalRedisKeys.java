package com.aquadev.ittopai.service.journal.token;

public final class JournalRedisKeys {

    private static final String ACCESS_TOKEN_CACHE_PREFIX = "jt:acc:";
    private static final String LOCK_PREFIX = "jt:lock:";
    private static final String TELEGRAM_TO_JOURNAL_MAP_PREFIX = "jt:tg:map:";

    private JournalRedisKeys() {
    }

    public static String accessToken(long journalUserId) {
        return ACCESS_TOKEN_CACHE_PREFIX + journalUserId;
    }

    public static String lock(long journalUserId) {
        return LOCK_PREFIX + journalUserId;
    }

    public static String telegramToJournalMap(long telegramUserId) {
        return TELEGRAM_TO_JOURNAL_MAP_PREFIX + telegramUserId;
    }
}
