package com.aquadev.journalservice.service.journal.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class TokenCryptoTest {

    private TokenCrypto crypto;

    @BeforeEach
    void setUp() {
        // AES-128 requires 16-byte key
        byte[] keyBytes = Arrays.copyOf("test-secret-key-for-testing".getBytes(StandardCharsets.UTF_8), 16);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        crypto = new TokenCrypto(secretKey);
    }

    @Test
    void encrypt_thenDecrypt_returnsOriginal() {
        String original = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEyfQ.signature";
        String encrypted = crypto.encrypt(original);
        String decrypted = crypto.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_null_returnsNull() {
        assertThat(crypto.encrypt(null)).isNull();
    }

    @Test
    void decrypt_null_returnsNull() {
        assertThat(crypto.decrypt(null)).isNull();
    }

    @Test
    void encrypt_producesNonNullNonBlankResult() {
        String result = crypto.encrypt("some-token");
        assertThat(result).isNotBlank();
    }

    @Test
    void encrypt_sameInputProducesDifferentCiphertexts() {
        // GCM uses random IV each time — same plaintext should give different ciphertext
        String plain = "same-token";
        String enc1 = crypto.encrypt(plain);
        String enc2 = crypto.encrypt(plain);
        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void decrypt_tamperedData_throwsRuntime() {
        String encrypted = crypto.encrypt("valid-token");
        // Flip one byte in the middle to cause GCM authentication failure
        byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);
        decoded[12] ^= 0xFF;
        String tampered = java.util.Base64.getEncoder().encodeToString(decoded);
        assertThatThrownBy(() -> crypto.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token decryption error");
    }

    @Test
    void encrypt_emptyString_roundtripsCorrectly() {
        String encrypted = crypto.encrypt("");
        assertThat(crypto.decrypt(encrypted)).isEqualTo("");
    }

    @Test
    void encrypt_longToken_roundtripsCorrectly() {
        String longToken = "a".repeat(2048);
        assertThat(crypto.decrypt(crypto.encrypt(longToken))).isEqualTo(longToken);
    }
}
