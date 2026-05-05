package com.aquadev.journalservice.service.journal.auth;

import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import com.aquadev.journalservice.config.journal.JournalApiProperties;
import com.aquadev.journalservice.config.journal.JournalTokenProperties;
import com.aquadev.journalservice.dto.request.JournalLoginRequest;
import com.aquadev.journalservice.dto.response.JournalTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAuthServiceImpl implements JournalAuthService {

    private static final Duration LOGIN_RETRY_BACKOFF = Duration.ofMillis(250);

    private final JournalAuthClient journalAuthClient;
    private final JournalApiProperties journalApiProperties;
    private final JournalTokenProperties journalTokenProperties;
    private final JournalAuthExceptionTranslator authExceptionTranslator;

    @Override
    public JournalTokenResponse login(String username, String password) {
        JournalLoginRequest request = JournalLoginRequest.builder()
                .applicationKey(journalApiProperties.applicationKey())
                .username(username)
                .password(password)
                .build();

        int maxAttempts = Math.max(1, journalTokenProperties.maxRetryAttempts());
        for (int attempt = 1; ; attempt++) {
            try {
                return journalAuthClient.login(request);
            } catch (HttpClientErrorException exception) {
                if (!isRetryableLoginFailure(exception) || attempt >= maxAttempts) {
                    throw authExceptionTranslator.translateLoginException(exception);
                }
                log.warn("Journal login returned {} on attempt {}/{}. Retrying.",
                        exception.getStatusCode().value(), attempt, maxAttempts);
                LockSupport.parkNanos(LOGIN_RETRY_BACKOFF.toNanos());
            }
        }
    }

    private boolean isRetryableLoginFailure(HttpClientErrorException exception) {
        int status = exception.getStatusCode().value();
        return status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value();
    }
}
