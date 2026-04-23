package com.aquadev.journalservice.service.journal.auth;

import com.aquadev.journalservice.exception.domain.journal.JournalAuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@RequiredArgsConstructor
public class JournalAuthExceptionTranslator {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Неверный логин или пароль";
    private static final String MESSAGE_FIELD = "message";

    private final ObjectMapper objectMapper;

    public RuntimeException translateApiException(Throwable throwable) {
        if (throwable instanceof JournalAuthenticationException authException) {
            return authException;
        }
        if (throwable instanceof HttpClientErrorException exception) {
            if (isInvalidCredentials(exception)) {
                return JournalAuthenticationException.invalidCredentials();
            }
            if (isAuthStatus(exception.getStatusCode().value())) {
                return JournalAuthenticationException.reauthRequired();
            }
            return exception;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Unexpected checked exception from Journal client", throwable);
    }

    public RuntimeException translateLoginException(HttpClientErrorException exception) {
        if (isInvalidCredentials(exception) || isAuthStatus(exception.getStatusCode().value())) {
            return JournalAuthenticationException.invalidCredentials();
        }
        return exception;
    }

    private boolean isInvalidCredentials(HttpClientErrorException exception) {
        int status = exception.getStatusCode().value();
        if (status != HttpStatus.UNPROCESSABLE_CONTENT.value()) {
            return false;
        }
        return extractMessage(exception.getResponseBodyAsString()).contains(INVALID_CREDENTIALS_MESSAGE);
    }

    private boolean isAuthStatus(int statusCode) {
        return statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value();
    }

    private String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.isArray() && !root.isEmpty()) {
                String message = readMessage(root.get(0));
                if (message != null) {
                    return message;
                }
            }
            String message = readMessage(root);
            if (message != null) {
                return message;
            }
        } catch (Exception _) {
            return body;
        }
        return body;
    }

    private String readMessage(JsonNode node) {
        if (node != null && node.hasNonNull(MESSAGE_FIELD)) {
            return node.get(MESSAGE_FIELD).asText("");
        }
        return null;
    }
}
