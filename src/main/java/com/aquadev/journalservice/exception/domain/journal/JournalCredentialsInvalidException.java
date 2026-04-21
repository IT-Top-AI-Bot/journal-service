package com.aquadev.journalservice.exception.domain.journal;

import com.aquadev.journalservice.exception.base.ForbiddenException;

public class JournalCredentialsInvalidException extends ForbiddenException {
    public JournalCredentialsInvalidException() {
        super("Journal credentials are invalid. Please update your credentials to continue.");
    }
}
