package com.aquadev.journalservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

public class JwtUtil {

    private JwtUtil() {
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> decodeJwt(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format - expected 3 parts separated by '.'");
        }

        String payloadBase64 = parts[1];

        try {
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(payloadBase64)
            );

            return mapper.readValue(payloadJson, Map.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT payload", e);
        }
    }

    public static Long getUserIdFromJwt(String token) {
        Map<String, Object> claims = decodeJwt(token);
        return Long.parseLong(claims.get("userId").toString());
    }
}
