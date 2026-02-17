package com.aquadev.ittopai.config.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingClientHttpRequestInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        if (log.isDebugEnabled()) {
            String bodyText = body.length > 0 ? new String(body, StandardCharsets.UTF_8) : "<empty>";
            log.debug("[RestClient] {} {} | body: {}",
                    request.getMethod(),
                    request.getURI(),
                    bodyText);
        }

        return execution.execute(request, body);
    }
}
