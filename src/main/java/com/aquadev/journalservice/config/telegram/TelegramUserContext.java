package com.aquadev.journalservice.config.telegram;

public class TelegramUserContext {

    public static final ScopedValue<Long> TG_USER_ID = ScopedValue.newInstance();

    public static Long get() {
        return TG_USER_ID.isBound() ? TG_USER_ID.get() : null;
    }

    private TelegramUserContext() {
    }
}
