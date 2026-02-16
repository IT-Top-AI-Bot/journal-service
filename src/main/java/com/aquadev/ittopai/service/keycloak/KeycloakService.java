package com.aquadev.ittopai.service.keycloak;

import com.aquadev.ittopai.dto.response.TokenResponse;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.UUID;

public interface KeycloakService {

    void createKeycloakUser(Long telegramId, UUID userId);

    void assignRoleToUser(Long telegramId, String role);

    UserRepresentation getUserByTelegramId(Long telegramId);

    TokenResponse getAccessToken(Long telegramId);

    TokenResponse refreshToken(String refreshToken);
}
