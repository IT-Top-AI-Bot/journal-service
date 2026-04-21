package com.aquadev.journalservice.client.journal.auth;

import com.aquadev.journalservice.dto.request.JournalLoginRequest;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/auth")
public interface JournalAuthClient {

    @PostExchange("/login")
    JournalTokenResponse login(@RequestBody JournalLoginRequest request);
}
