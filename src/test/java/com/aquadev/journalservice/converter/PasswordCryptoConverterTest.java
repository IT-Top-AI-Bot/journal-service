package com.aquadev.journalservice.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCryptoConverterTest {

    private PasswordCryptoConverter converter;
    // 32 random bytes, Base64-encoded → valid AES-256 key
    private static final String SECRET_KEY = java.util.Base64.getEncoder()
            .encodeToString("12345678901234567890123456789012".getBytes());

    @BeforeEach
    void setUp() {
        converter = new PasswordCryptoConverter();
        ReflectionTestUtils.setField(converter, "secretKeyString", SECRET_KEY);
        converter.init();
    }

    @Test
    void convertToDatabaseColumn_success() {
        String password = "my-secret-password";
        String encrypted = converter.convertToDatabaseColumn(password);

        assertThat(encrypted)
                .isNotNull()
                .isNotEqualTo(password);
    }

    @Test
    void convertToEntityAttribute_success() {
        String password = "my-secret-password";
        String encrypted = converter.convertToDatabaseColumn(password);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(password);
    }

    @Test
    void convertToDatabaseColumn_null_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_null_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
