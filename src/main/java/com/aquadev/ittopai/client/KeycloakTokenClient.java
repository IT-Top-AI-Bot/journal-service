package com.aquadev.ittopai.client;

import com.aquadev.ittopai.config.client.KeycloakFeignConfig;
import com.aquadev.ittopai.dto.request.RefreshTokenFormRequest;
import com.aquadev.ittopai.dto.response.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "keycloak-token-client",
        url = "${keycloak.auth-server-url}",
        configuration = KeycloakFeignConfig.class
)
public interface KeycloakTokenClient {

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TokenResponse exchangeToken(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorization,
            @RequestBody MultiValueMap<String, String> formParams
    );

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TokenResponse refreshToken(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorization,
            @RequestBody RefreshTokenFormRequest request
    );
}
