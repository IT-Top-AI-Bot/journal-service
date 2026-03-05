package com.aquadev.journalservice.config.journal;

import com.aquadev.journalservice.service.journal.token.JournalTokenManager;
import com.aquadev.journalservice.service.journal.token.JournalUserIdResolver;
import com.aquadev.journalservice.util.SecurityUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

public class JournalAuthInterceptor implements ClientHttpRequestInterceptor {

    private static final String RETRY_HEADER = "X-Journal-Retry";

    private final ObjectProvider<JournalTokenManager> tokenManagerProvider;
    private final ObjectProvider<JournalUserIdResolver> journalUserIdResolverProvider;

    public JournalAuthInterceptor(
            ObjectProvider<JournalTokenManager> tokenManagerProvider,
            ObjectProvider<JournalUserIdResolver> journalUserIdResolverProvider
    ) {
        this.tokenManagerProvider = tokenManagerProvider;
        this.journalUserIdResolverProvider = journalUserIdResolverProvider;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        if (isAuthEndpoint(request.getURI())) {
            return execution.execute(request, body);
        }

        long telegramUserId = SecurityUtil.getCurrentTelegramUserId();
        long journalUserId = journalUserIdResolverProvider.getObject().resolve(telegramUserId);
        String accessToken = tokenManagerProvider.getObject().getValidAccessToken(journalUserId);
        request.getHeaders().setBearerAuth(accessToken);

        var response = execution.execute(request, body);
        if (response.getStatusCode().value() != 401 || request.getHeaders().getFirst(RETRY_HEADER) != null) {
            return response;
        }

        response.close();
        String refreshed = tokenManagerProvider.getObject().forceRefreshAccessToken(journalUserId);
        request.getHeaders().setBearerAuth(refreshed);
        request.getHeaders().add(RETRY_HEADER, "1");
        return execution.execute(request, body);
    }

    private boolean isAuthEndpoint(URI uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }

        return path.endsWith("/auth/login") || path.endsWith("/auth/refresh");
    }
}
