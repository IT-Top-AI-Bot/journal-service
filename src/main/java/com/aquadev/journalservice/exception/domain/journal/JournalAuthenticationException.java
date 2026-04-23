package com.aquadev.journalservice.exception.domain.journal;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class JournalAuthenticationException extends RuntimeException {

    private final Reason reason;

    private JournalAuthenticationException(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

    public static JournalAuthenticationException invalidCredentials() {
        return new JournalAuthenticationException(Reason.INVALID_CREDENTIALS);
    }

    public static JournalAuthenticationException missingCredentials() {
        return new JournalAuthenticationException(Reason.MISSING_CREDENTIALS);
    }

    public static JournalAuthenticationException reauthRequired() {
        return new JournalAuthenticationException(Reason.REAUTH_REQUIRED);
    }

    public enum Reason {
        INVALID_CREDENTIALS("Journal credentials are invalid. Please update your credentials to continue."),
        MISSING_CREDENTIALS("Journal credentials are missing. Please update your credentials to continue."),
        REAUTH_REQUIRED("Journal session requires re-authentication. Please update your credentials to continue.");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }
}
