package com.aquadev.journalservice.config.client;

import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class JournalClientConfig {

    private final RestClient journalRestClient;
    private final RestClient journalSystemRestClient;
    private final RateLimiterRegistry rateLimiterRegistry;

    public JournalClientConfig(
            @Qualifier("journalRestClient") RestClient journalRestClient,
            @Qualifier("journalSystemRestClient") RestClient journalSystemRestClient,
            RateLimiterRegistry rateLimiterRegistry
    ) {
        this.journalRestClient = journalRestClient;
        this.journalSystemRestClient = journalSystemRestClient;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Bean
    public JournalAuthClient journalAuthClient() {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(journalSystemRestClient))
                .build()
                .createClient(JournalAuthClient.class);
    }

    @Bean
    public JournalClient journalClient() {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("journalApi");

        RestClient client = journalRestClient.mutate()
                .defaultStatusHandler(
                        status -> status == HttpStatus.NOT_FOUND,
                        (_, _) -> {
                        }
                )
                .build();

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(LocalDate.class, String.class,
                date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        JournalClient rawClient = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .conversionService(conversionService)
                .build()
                .createClient(JournalClient.class);

        return (JournalClient) Proxy.newProxyInstance(
                JournalClient.class.getClassLoader(),
                new Class[]{JournalClient.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(rawClient, args);
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    rateLimiter.acquirePermission();
                    try {
                        return method.invoke(rawClient, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }
}
