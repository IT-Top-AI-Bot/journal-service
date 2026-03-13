package com.aquadev.journalservice.converter;

import com.aquadev.journalservice.exception.domain.crypto.CryptoException;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class PasswordCryptoConverter implements AttributeConverter<String, String> {

    @Value("${crypto.secret-key}")
    private String secretKeyString;

    private SecretKey key;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        this.key = new SecretKeySpec(decodedKey, "AES");
    }

    private Cipher getInitializedCipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = getInitializedCipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new CryptoException("Encryption error", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            Cipher cipher = getInitializedCipher(Cipher.DECRYPT_MODE, iv);

            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Decryption error", e);
        }
    }
}
