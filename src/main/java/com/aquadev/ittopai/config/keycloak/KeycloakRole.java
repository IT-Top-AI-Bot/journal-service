package com.aquadev.ittopai.config.keycloak;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeycloakRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String roleName;
}


