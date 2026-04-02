package com.aquadev.journalservice.exception.handler;

import com.aquadev.journalservice.dto.response.ErrorResponse;
import com.aquadev.journalservice.dto.response.ValidationErrorResponse;
import com.aquadev.journalservice.exception.base.ConflictException;
import com.aquadev.journalservice.exception.base.NotFoundException;
import com.aquadev.journalservice.mapper.ExceptionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ExceptionMapper exceptionMapper;
    private final ObjectMapper objectMapper;

    private String extractJournalErrorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.isArray() && !root.isEmpty()) {
                JsonNode msg = root.get(0).get("message");
                if (msg != null) return msg.asText();
            }
        } catch (Exception _) {
            // fallback: body is not parseable JSON
        }
        return "Invalid Journal credentials";
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("[DB] Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Resource already exists",
                request.getRequestURI(),
                List.of()
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public ErrorResponse handleConflictException(ConflictException ex, HttpServletRequest request) {
        return exceptionMapper.toErrorResponse(ex, request.getRequestURI());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleUserNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return exceptionMapper.toErrorResponse(ex, request.getRequestURI());
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleJournalClientError(HttpClientErrorException ex, HttpServletRequest request) {
        int code = ex.getStatusCode().value();
        return switch (code) {
            case 401, 403, 422 -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                    Instant.now(), HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    extractJournalErrorMessage(ex.getResponseBodyAsString()), request.getRequestURI(), List.of()
            ));
            default -> {
                log.error("[JournalAPI] Unexpected {} response: {}", code, ex.getResponseBodyAsString());
                yield ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
                        Instant.now(), HttpStatus.BAD_GATEWAY.value(), HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                        "Unexpected error from Journal API", request.getRequestURI(), List.of()
                ));
            }
        };
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleJournalServerError(HttpServerErrorException ex, HttpServletRequest request) {
        log.error("[JournalAPI] Server error {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Journal API is temporarily unavailable", request.getRequestURI(), List.of()
        ));
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(RequestNotPermitted.class)
    public ErrorResponse handleRequestNotPermitted(RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("[RateLimiter] Journal API rate limit exceeded on {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Journal API rate limit exceeded, please retry later",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationErrorResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Error validating request data",
                request.getRequestURI(),
                errors
        );
    }
}
