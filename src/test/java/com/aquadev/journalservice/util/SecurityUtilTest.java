package com.aquadev.journalservice.util;

import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilTest {

    @Test
    void getCurrentTelegramUserId_success() throws Exception {
        ScopedValue.where(TelegramUserContext.TG_USER_ID, 123L).call(() -> {
            assertThat(SecurityUtil.getCurrentTelegramUserId()).isEqualTo(123L);
            return null;
        });
    }

    @Test
    void getCurrentTelegramUserId_noUser_throwsException() {
        assertThatThrownBy(SecurityUtil::getCurrentTelegramUserId)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No authenticated telegram user found");
    }
}
