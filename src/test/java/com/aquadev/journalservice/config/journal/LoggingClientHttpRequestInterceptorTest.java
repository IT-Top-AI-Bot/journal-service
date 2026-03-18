package com.aquadev.journalservice.config.journal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoggingClientHttpRequestInterceptorTest {

    @Mock
    private HttpRequest request;
    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse response;

    private final LoggingClientHttpRequestInterceptor interceptor = new LoggingClientHttpRequestInterceptor();

    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUpCommonStubs() throws IOException {
        when(request.getURI()).thenReturn(URI.create("http://api/test"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(execution.execute(any(), any())).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        logger = (Logger) LoggerFactory.getLogger(LoggingClientHttpRequestInterceptor.class);
        originalLevel = logger.getEffectiveLevel();
    }

    @AfterEach
    void restoreLogLevel() {
        logger.setLevel(originalLevel);
    }

    @Test
    void intercept_callsExecution() throws IOException {
        interceptor.intercept(request, new byte[0], execution);

        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void intercept_debugEnabled_logsRequestBodyAndResponseBody() throws IOException {
        logger.setLevel(Level.DEBUG);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("resp".getBytes()));

        interceptor.intercept(request, "body".getBytes(), execution);

        verify(execution).execute(request, "body".getBytes());
    }

    @Test
    void intercept_debugEnabled_emptyBody_skipsRequestBodyLog() throws IOException {
        logger.setLevel(Level.DEBUG);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

        interceptor.intercept(request, new byte[0], execution);

        verify(execution).execute(request, new byte[0]);
    }
}
