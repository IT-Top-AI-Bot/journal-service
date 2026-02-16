package com.aquadev.ittopai.config.telegram;

public class TelegramUserContext {

    private static final ThreadLocal<Long> TG_USER_ID = new ThreadLocal<>();

    public static void set(long id) {
        TG_USER_ID.set(id);
    }

    public static Long get() {
        return TG_USER_ID.get();
    }

    public static void clear() {
        TG_USER_ID.remove();
    }

    private TelegramUserContext() {
    }
}
