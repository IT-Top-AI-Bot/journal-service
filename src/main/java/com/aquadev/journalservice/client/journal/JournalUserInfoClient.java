package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalUserResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface JournalUserInfoClient {

    @GetExchange("/settings/user-info")
    JournalUserResponse getCurrentUser();
}
