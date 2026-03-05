package com.aquadev.journalservice.client.journal.auth;

import com.aquadev.journalservice.config.journal.JournalApiProperties;
import com.aquadev.journalservice.dto.request.JournalLoginRequest;
import com.aquadev.journalservice.dto.request.RefreshTokenRequest;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class JournalAuthClientImpl implements JournalAuthClient {

    private final RestClient restClient;
    private final JournalApiProperties journalApiProperties;

    @Override
    public JournalTokenResponse login(String username, String password) {
        return restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JournalLoginRequest(journalApiProperties.applicationKey(), null, username, password))
                .retrieve()
                .body(JournalTokenResponse.class);
    }

    @Override
    public JournalTokenResponse refreshToken(String refreshToken) {
        return restClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshTokenRequest(refreshToken))
                .retrieve()
                .body(JournalTokenResponse.class);
    }
}
