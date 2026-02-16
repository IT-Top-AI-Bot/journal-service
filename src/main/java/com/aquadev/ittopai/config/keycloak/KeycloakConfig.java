package com.aquadev.ittopai.config.keycloak;

import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.Getter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm:actimate}")
    private String realm;

    @Value("${keycloak.client.id}")
    private String clientId;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    @Getter(AccessLevel.PRIVATE)
    private Keycloak keycloak;

    @Bean
    public Keycloak keycloak() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
        return this.keycloak;
    }

    @PreDestroy
    public void close() {
        if (this.keycloak != null) {
            this.keycloak.close();
        }
    }
}
