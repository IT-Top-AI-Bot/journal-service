package com.aquadev.journalservice.config.journal;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingClientHttpRequestInterceptor.class);

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            byte @NonNull [] body,
            @NonNull ClientHttpRequestExecution execution
    ) throws IOException {
        if (log.isDebugEnabled() && body.length > 0) {
            log.debug("[Journal] --> {} {} | body: {}",
                    request.getMethod(),
                    request.getURI(),
                    new String(body, StandardCharsets.UTF_8));
        }

        long start = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long duration = System.currentTimeMillis() - start;

        HttpStatusCode status = response.getStatusCode();
        log.info("[Journal] {} {} -> {} ({}ms)",
                request.getMethod(),
                request.getURI(),
                status.value(),
                duration);

        if (log.isDebugEnabled()) {
            byte[] responseBody = response.getBody().readAllBytes();
            log.debug("[Journal] <-- {} body: {}",
                    status.value(),
                    new String(responseBody, StandardCharsets.UTF_8));
        }

        return response;
    }
}
