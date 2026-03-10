package com.aquadev.journalservice.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCryptoConverterTest {

    private PasswordCryptoConverter converter;
    private static final String SECRET_KEY = "12345678901234567890123456789012"; // 32 chars for AES-256

    @BeforeEach
    void setUp() {
        converter = new PasswordCryptoConverter();
        ReflectionTestUtils.setField(converter, "secretKey", SECRET_KEY);
    }

    @Test
    void convertToDatabaseColumn_success() {
        String password = "my-secret-password";
        String encrypted = converter.convertToDatabaseColumn(password);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(password);
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
