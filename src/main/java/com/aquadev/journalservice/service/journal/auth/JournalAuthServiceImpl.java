package com.aquadev.journalservice.service.journal.auth;

import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import com.aquadev.journalservice.config.journal.JournalApiProperties;
import com.aquadev.journalservice.dto.request.JournalLoginRequest;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JournalAuthServiceImpl implements JournalAuthService {

    private final JournalAuthClient journalAuthClient;
    private final JournalApiProperties journalApiProperties;

    @Override
    public JournalTokenResponse login(String username, String password) {
        return journalAuthClient.login(JournalLoginRequest.builder()
                .applicationKey(journalApiProperties.applicationKey())
                .username(username)
                .password(password)
                .build());
    }
}
