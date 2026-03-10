package com.aquadev.journalservice.exception.handler;

import com.aquadev.journalservice.dto.response.ErrorResponse;
import com.aquadev.journalservice.exception.base.ConflictException;
import com.aquadev.journalservice.exception.base.NotFoundException;
import com.aquadev.journalservice.mapper.ExceptionMapper;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ExceptionMapper exceptionMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    void handleConflictException_returnsMappedResponse() {
        ConflictException ex = new ConflictException("Conflict") {
        };
        ErrorResponse expected = new ErrorResponse(Instant.now(), 409, "Conflict", "Conflict", "/uri", List.of());
        when(request.getRequestURI()).thenReturn("/uri");
        when(exceptionMapper.toErrorResponse(ex, "/uri")).thenReturn(expected);

        ErrorResponse result = exceptionHandler.handleConflictException(ex, request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void handleUserNotFoundException_returnsMappedResponse() {
        NotFoundException ex = new NotFoundException("Not Found") {
        };
        ErrorResponse expected = new ErrorResponse(Instant.now(), 404, "Not Found", "Not Found", "/uri", List.of());
        when(request.getRequestURI()).thenReturn("/uri");
        when(exceptionMapper.toErrorResponse(ex, "/uri")).thenReturn(expected);

        ErrorResponse result = exceptionHandler.handleUserNotFoundException(ex, request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void handleRequestNotPermitted_returnsErrorResponse() {
        RequestNotPermitted ex = mock(RequestNotPermitted.class);
        when(request.getRequestURI()).thenReturn("/uri");

        ErrorResponse result = exceptionHandler.handleRequestNotPermitted(ex, request);

        assertThat(result.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(result.message()).contains("rate limit exceeded");
        assertThat(result.path()).isEqualTo("/uri");
    }

    @Test
    void handleMethodArgumentNotValidException_returnsErrorResponseWithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "message");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(request.getRequestURI()).thenReturn("/uri");

        ErrorResponse result = exceptionHandler.handleMethodArgumentNotValidException(ex, request);

        assertThat(result.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).field()).isEqualTo("field");
        assertThat(result.errors().get(0).message()).isEqualTo("message");
    }
}
