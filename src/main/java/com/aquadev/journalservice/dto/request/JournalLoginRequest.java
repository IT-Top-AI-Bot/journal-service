package com.aquadev.journalservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalLoginRequest {
    private String applicationKey;
    private String idCity;
    private String username;
    private String password;
}
