package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.dto.response.JournalTokenResponse;

public interface JournalAuthService {

    JournalTokenResponse login(String username, String password);

    JournalTokenResponse refreshToken(String refreshToken);
}
