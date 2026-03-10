package com.aquadev.journalservice.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private static String makeJwt(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".signature";
    }

    // ── decodeJwt ──────────────────────────────────────────────────────────────

    @Test
    void decodeJwt_returnsClaimsMap() {
        String jwt = makeJwt("{\"userId\":42,\"sub\":\"user\"}");
        Map<String, Object> claims = JwtUtil.decodeJwt(jwt);
        assertThat(claims).containsEntry("userId", 42);
    }

    @Test
    void decodeJwt_nullToken_throwsIllegalArgument() {
        assertThatThrownBy(() -> JwtUtil.decodeJwt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void decodeJwt_blankToken_throwsIllegalArgument() {
        assertThatThrownBy(() -> JwtUtil.decodeJwt("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void decodeJwt_missingParts_throwsIllegalArgument() {
        // BUG: error message says "expected 3 parts" but code only validates length < 2,
        // meaning a 2-part string is accepted. A true JWT needs 3 parts.
        assertThatThrownBy(() -> JwtUtil.decodeJwt("onlyonepart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JWT format");
    }

    @Test
    void decodeJwt_invalidBase64Payload_throwsRuntime() {
        // header.invalidBase64!.sig
        assertThatThrownBy(() -> JwtUtil.decodeJwt("header.!!!.sig"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decode JWT payload");
    }

    // ── getUserIdFromJwt ───────────────────────────────────────────────────────

    @Test
    void getUserIdFromJwt_returnsUserId() {
        String jwt = makeJwt("{\"userId\":100}");
        assertThat(JwtUtil.getUserIdFromJwt(jwt)).isEqualTo(100L);
    }

    @Test
    void getUserIdFromJwt_userIdAsString_returnsLong() {
        // userId can come as string value in some JWT implementations
        String jwt = makeJwt("{\"userId\":\"999\"}");
        assertThat(JwtUtil.getUserIdFromJwt(jwt)).isEqualTo(999L);
    }

    @Test
    void getUserIdFromJwt_missingUserIdClaim_throwsNPE() {
        // BUG: claims.get("userId") returns null → NPE on .toString()
        String jwt = makeJwt("{\"sub\":\"user\"}");
        assertThatThrownBy(() -> JwtUtil.getUserIdFromJwt(jwt))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getUserIdFromJwt_nonNumericUserId_throwsNumberFormat() {
        String jwt = makeJwt("{\"userId\":\"not-a-number\"}");
        assertThatThrownBy(() -> JwtUtil.getUserIdFromJwt(jwt))
                .isInstanceOf(NumberFormatException.class);
    }
}
