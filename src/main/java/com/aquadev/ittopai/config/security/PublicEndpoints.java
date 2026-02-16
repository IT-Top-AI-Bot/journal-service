package com.aquadev.ittopai.config.security;

import java.util.List;

public final class PublicEndpoints {

    static final String[] SECURITY_MATCHERS = {
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/auth/**"
    };

    private static final List<String> FILTER_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/v1/auth",
            "/error"
    );

    private PublicEndpoints() {
    }

    public static boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }

        return FILTER_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
