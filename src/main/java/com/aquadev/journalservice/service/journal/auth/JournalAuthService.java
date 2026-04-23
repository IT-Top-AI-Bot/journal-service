package com.aquadev.journalservice.service.journal.auth;

import com.aquadev.journalservice.dto.response.JournalTokenResponse;

public interface JournalAuthService {

    JournalTokenResponse login(String username, String password);
}
