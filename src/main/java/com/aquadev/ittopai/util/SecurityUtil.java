package com.aquadev.ittopai.util;

import com.aquadev.ittopai.config.telegram.TelegramUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static long getCurrentTelegramUserId() {
        Long fromContext = TelegramUserContext.get();
        if (fromContext != null) {
            return fromContext;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long id) {
                return id;
            }
            if (principal instanceof String raw) {
                return Long.parseLong(raw);
            }
        }

        throw new RuntimeException("No authenticated telegram user found");
    }
}
