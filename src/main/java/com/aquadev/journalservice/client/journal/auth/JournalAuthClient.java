package com.aquadev.journalservice.client.journal.auth;

import com.aquadev.journalservice.dto.response.JournalTokenResponse;

public interface JournalAuthClient {

    JournalTokenResponse login(String username, String password);

    JournalTokenResponse refreshToken(String refreshToken);
}
