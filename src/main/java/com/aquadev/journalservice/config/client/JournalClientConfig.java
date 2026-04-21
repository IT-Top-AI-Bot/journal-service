package com.aquadev.journalservice.config.client;

import com.aquadev.journalservice.client.journal.JournalHomeworkQueryClient;
import com.aquadev.journalservice.client.journal.JournalHomeworkSubmissionClient;
import com.aquadev.journalservice.client.journal.JournalReferenceClient;
import com.aquadev.journalservice.client.journal.JournalUserInfoClient;
import com.aquadev.journalservice.client.journal.auth.JournalAuthClient;
import com.aquadev.journalservice.service.journal.auth.JournalAuthExceptionTranslator;
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
    private final JournalAuthExceptionTranslator authExceptionTranslator;

    public JournalClientConfig(
            @Qualifier("journalRestClient") RestClient journalRestClient,
            @Qualifier("journalSystemRestClient") RestClient journalSystemRestClient,
            RateLimiterRegistry rateLimiterRegistry,
            JournalAuthExceptionTranslator authExceptionTranslator
    ) {
        this.journalRestClient = journalRestClient;
        this.journalSystemRestClient = journalSystemRestClient;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.authExceptionTranslator = authExceptionTranslator;
    }

    @Bean
    public JournalAuthClient journalAuthClient() {
        return createRawClient(JournalAuthClient.class, journalSystemRestClient);
    }

    @Bean
    public JournalUserInfoClient journalUserInfoClient() {
        return createRateLimitedClient(JournalUserInfoClient.class);
    }

    @Bean
    public JournalHomeworkQueryClient journalHomeworkQueryClient() {
        return createRateLimitedClient(JournalHomeworkQueryClient.class);
    }

    @Bean
    public JournalReferenceClient journalReferenceClient() {
        return createRateLimitedClient(JournalReferenceClient.class);
    }

    @Bean
    public JournalHomeworkSubmissionClient journalHomeworkSubmissionClient() {
        return createRateLimitedClient(JournalHomeworkSubmissionClient.class);
    }

    private <T> T createRateLimitedClient(Class<T> clientType) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("journalApi");
        RestClient client = journalRestClient.mutate()
                .defaultStatusHandler(
                        status -> status == HttpStatus.NOT_FOUND,
                        (_, _) -> {
                        }
                )
                .build();
        T rawClient = createRawClient(clientType, client);
        return clientType.cast(Proxy.newProxyInstance(
                clientType.getClassLoader(),
                new Class[]{clientType},
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
                        throw authExceptionTranslator.translateApiException(e.getCause());
                    }
                }
        ));
    }

    private <T> T createRawClient(Class<T> clientType, RestClient client) {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(LocalDate.class, String.class,
                date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .conversionService(conversionService)
                .build()
                .createClient(clientType);
    }
}
