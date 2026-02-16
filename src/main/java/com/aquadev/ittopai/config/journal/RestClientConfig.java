package com.aquadev.ittopai.config.journal;

import com.aquadev.ittopai.service.journal.JournalUserIdResolver;
import com.aquadev.ittopai.service.journal.token.JournalTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final JournalApiProperties journalApiProperties;
    private final ObjectProvider<JournalTokenManager> tokenManagerProvider;
    private final ObjectProvider<JournalUserIdResolver> journalUserIdResolverProvider;

    private static HttpMessageConverter<Object> snakeCaseJsonConverter() {
        SimpleModule journalModule = new SimpleModule()
                .addDeserializer(Instant.class, new JournalInstantDeserializer(ZoneId.systemDefault()));

        JsonMapper snakeCaseMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .addModule(journalModule)
                .build();

        return new JacksonJsonHttpMessageConverter(snakeCaseMapper);
    }

    private static RestClient.Builder applyCommon(RestClient.Builder builder, JournalApiProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Origin", properties.journalUrl())
                .defaultHeader("Referer", properties.journalUrl())
                .defaultHeader("User-Agent", properties.userAgent());
    }

    private static RestClient.Builder applySnakeCaseJson(RestClient.Builder builder) {
        var jsonConverter = snakeCaseJsonConverter();

        return builder.configureMessageConverters(converters -> {
            converters.registerDefaults();

            converters.withJsonConverter(jsonConverter);
        });
    }

    @Bean
    @Primary
    public RestClient journalRestClient(RestClient.Builder builder) {
        ClientHttpRequestFactory bufferingFactory = createBufferingFactory();

        return applySnakeCaseJson(applyCommon(builder, journalApiProperties))
                .requestInterceptor(journalAuthInterceptor())
                .requestFactory(bufferingFactory)
                .build();
    }

    @Bean("journalSystemRestClient")
    public RestClient journalSystemRestClient(RestClient.Builder builder) {
        ClientHttpRequestFactory bufferingFactory = createBufferingFactory();

        return applySnakeCaseJson(applyCommon(builder, journalApiProperties))
                .requestFactory(bufferingFactory)
                .build();
    }

    private ClientHttpRequestFactory createBufferingFactory() {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(journalApiProperties.timeout().connect()))
                .withReadTimeout(Duration.ofSeconds(journalApiProperties.timeout().read()));

        ClientHttpRequestFactory baseFactory =
                ClientHttpRequestFactoryBuilder.detect().build(settings);

        ClientHttpRequestFactory bufferingFactory =
                new BufferingClientHttpRequestFactory(baseFactory);
        return bufferingFactory;
    }

    @Bean
    public ClientHttpRequestInterceptor journalAuthInterceptor() {
        return new JournalAuthInterceptor(tokenManagerProvider, journalUserIdResolverProvider);
    }
}
