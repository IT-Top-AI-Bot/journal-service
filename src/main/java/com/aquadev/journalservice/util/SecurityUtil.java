package com.aquadev.journalservice.util;

import com.aquadev.journalservice.config.telegram.TelegramUserContext;

public class SecurityUtil {

    public static long getCurrentTelegramUserId() {
        Long telegramUserId = TelegramUserContext.get();
        if (telegramUserId != null) {
            return telegramUserId;
        }
        throw new RuntimeException("No authenticated telegram user found");
    }
}
