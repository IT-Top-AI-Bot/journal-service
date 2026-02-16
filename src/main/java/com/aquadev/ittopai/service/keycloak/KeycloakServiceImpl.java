package com.aquadev.ittopai.service.keycloak;

import com.aquadev.ittopai.client.KeycloakTokenClient;
import com.aquadev.ittopai.config.keycloak.KeycloakConfig;
import com.aquadev.ittopai.config.keycloak.KeycloakRole;
import com.aquadev.ittopai.dto.request.RefreshTokenFormRequest;
import com.aquadev.ittopai.dto.response.TokenResponse;
import com.aquadev.ittopai.exception.domain.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.encodeBasicAuth;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_TYPE_ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";
    private static final String TOKEN_TYPE_REFRESH_TOKEN = "urn:ietf:params:oauth:token-type:refresh_token";
    private static final String USER_ID_ATTRIBUTE = "user_id";

    private final Keycloak keycloakAdmin;
    private final KeycloakConfig keycloakConfig;
    private final KeycloakTokenClient keycloakTokenClient;

    @Override
    public void createKeycloakUser(Long telegramId, UUID userId) {
        UserRepresentation user = buildUserRepresentation(telegramId, userId);
        keycloakAdmin.realm(keycloakConfig.getRealm()).users().create(user).close();

        log.info("Created user with telegram ID: {} and user ID: {}", telegramId, userId);

        assignRoleToUser(telegramId, KeycloakRole.USER.getRoleName());
    }

    @Override
    public void assignRoleToUser(Long telegramId, String role) {
        UserRepresentation user = getUserByTelegramId(telegramId);
        keycloakAdmin.realm(keycloakConfig.getRealm())
                .users()
                .get(user.getId())
                .roles()
                .realmLevel()
                .add(Collections.singletonList(findRoleByName(role)));

        log.info("Assigned role {} to user with telegram ID: {}", role, telegramId);
    }

    @Override
    public UserRepresentation getUserByTelegramId(Long telegramId) {
        return keycloakAdmin.realm(keycloakConfig.getRealm())
                .users()
                .searchByUsername(telegramId.toString(), true)
                .stream()
                .findFirst()
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public TokenResponse getAccessToken(Long telegramId) {
        return executeTokenOperation(() -> {
            String userId = getUserByTelegramId(telegramId).getId();
            log.info("Exchanging token for user id: {}", userId);

            keycloakAdmin.tokenManager().grantToken();
            String authHeader = createBasicAuthHeader();
            MultiValueMap<String, String> formRequest = buildTokenExchangeRequest(userId);

            TokenResponse tokenResponse = keycloakTokenClient.exchangeToken(
                    keycloakConfig.getRealm(),
                    authHeader,
                    formRequest
            );

            log.info("Token exchange successful for user: {}", telegramId);
            return tokenResponse;
        }, "Failed to get access token for user: " + telegramId);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        return executeTokenOperation(() -> {
            log.info("Refreshing token");

            String authHeader = createBasicAuthHeader();
            RefreshTokenFormRequest formRequest = buildRefreshTokenRequest(refreshToken);

            TokenResponse tokenResponse = keycloakTokenClient.refreshToken(
                    keycloakConfig.getRealm(),
                    authHeader,
                    formRequest
            );

            log.info("Token refresh successful");
            return tokenResponse;
        }, "Failed to refresh token");
    }

    private UserRepresentation buildUserRepresentation(Long telegramId, UUID userId) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(telegramId.toString());
        user.setEmailVerified(true);
        user.setEnabled(true);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(USER_ID_ATTRIBUTE, Collections.singletonList(userId.toString()));
        user.setAttributes(attributes);

        return user;
    }

    private RoleRepresentation findRoleByName(String roleName) {
        return keycloakAdmin.realm(keycloakConfig.getRealm())
                .roles()
                .get(roleName)
                .toRepresentation();
    }

    private String createBasicAuthHeader() {
        return "Basic " + encodeBasicAuth(keycloakConfig.getClientId(), keycloakConfig.getClientSecret(), null);
    }

    private MultiValueMap<String, String> buildTokenExchangeRequest(String userId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE_TOKEN_EXCHANGE);
        form.add("client_id", keycloakConfig.getClientId());
        form.add("subject_token", keycloakAdmin.tokenManager().getAccessTokenString());
        form.add("subject_token_type", TOKEN_TYPE_ACCESS_TOKEN);
        form.add("audience", keycloakConfig.getClientId());
        form.add("requested_token_type", TOKEN_TYPE_REFRESH_TOKEN);
        form.add("requested_subject", userId);
        return form;
    }

    private RefreshTokenFormRequest buildRefreshTokenRequest(String refreshToken) {
        return RefreshTokenFormRequest.builder()
                .grantType(GRANT_TYPE_REFRESH_TOKEN)
                .clientId(keycloakConfig.getClientId())
                .refreshToken(refreshToken)
                .build();
    }

    private TokenResponse executeTokenOperation(Supplier<TokenResponse> operation, String errorMessage) {
        try {
            return operation.get();
        } catch (Exception e) {
            log.error("{}: {}", errorMessage, e.getMessage(), e);
            throw new IllegalStateException(errorMessage + ": " + e.getMessage(), e);
        }
    }
}
