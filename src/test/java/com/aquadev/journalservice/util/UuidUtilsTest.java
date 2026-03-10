package com.aquadev.journalservice.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidUtilsTest {

    @Test
    void randomV7_generatesValidUuid() {
        UUID uuid = UuidUtils.randomV7();
        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(7);
    }

    @Test
    void randomBytes_returnsCorrectLength() {
        byte[] bytes = UuidUtils.randomBytes();
        assertThat(bytes).hasSize(16);
    }
}
