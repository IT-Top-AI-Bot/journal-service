package com.aquadev.journalservice.config.journal;

import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.service.journal.token.JournalTokenManager;
import com.aquadev.journalservice.service.journal.token.JournalUserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalAuthInterceptorTest {

    @Mock
    private ObjectProvider<JournalTokenManager> tokenManagerProvider;
    @Mock
    private ObjectProvider<JournalUserIdResolver> journalUserIdResolverProvider;
    @Mock
    private JournalTokenManager tokenManager;
    @Mock
    private JournalUserIdResolver journalUserIdResolver;
    @Mock
    private HttpRequest request;
    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse response;

    private JournalAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JournalAuthInterceptor(tokenManagerProvider, journalUserIdResolverProvider);
        when(tokenManagerProvider.getObject()).thenReturn(tokenManager);
        when(journalUserIdResolverProvider.getObject()).thenReturn(journalUserIdResolver);
    }

    @Test
    void intercept_authEndpoint_doesNotAddAuth() throws IOException {
        when(request.getURI()).thenReturn(URI.create("http://api/auth/login"));
        when(execution.execute(any(), any())).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        verify(tokenManager, never()).getValidAccessToken(anyLong());
    }

    @Test
    void intercept_regularEndpoint_addsAuth() throws Exception {
        when(request.getURI()).thenReturn(URI.create("http://api/homework"));
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(journalUserIdResolver.resolve(123L)).thenReturn(456L);
        when(tokenManager.getValidAccessToken(456L)).thenReturn("token");
        when(execution.execute(any(), any())).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        ScopedValue.where(TelegramUserContext.TG_USER_ID, 123L).call(() -> {
            interceptor.intercept(request, new byte[0], execution);
            return null;
        });

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }

    @Test
    void intercept_401Response_retriesWithRefreshedToken() throws Exception {
        when(request.getURI()).thenReturn(URI.create("http://api/homework"));
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(journalUserIdResolver.resolve(123L)).thenReturn(456L);
        when(tokenManager.getValidAccessToken(456L)).thenReturn("old-token");
        when(tokenManager.forceRefreshAccessToken(456L)).thenReturn("new-token");

        ClientHttpResponse response401 = mock(ClientHttpResponse.class);
        when(response401.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        when(execution.execute(any(), any()))
                .thenReturn(response401) // First call
                .thenReturn(response);    // Second call (retry)

        ScopedValue.where(TelegramUserContext.TG_USER_ID, 123L).call(() -> {
            interceptor.intercept(request, new byte[0], execution);
            return null;
        });

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer new-token");
        verify(execution, times(2)).execute(any(), any());
    }
}
