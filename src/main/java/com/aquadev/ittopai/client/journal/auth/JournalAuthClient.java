package com.aquadev.ittopai.client.journal.auth;

import com.aquadev.ittopai.dto.response.JournalTokenResponse;

public interface JournalAuthClient {

    JournalTokenResponse login(String username, String password);

    JournalTokenResponse refreshToken(String refreshToken);
}
