/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class AppConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(60000); // 60 seconds
        restTemplate.setRequestFactory(factory);

        // Add User-Agent header for all requests to comply with sites like Wikimedia
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            request.getHeaders().set("User-Agent", "library.muczynskifamily.com");
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(interceptor));

        return restTemplate;
    }

    /**
     * RestTemplate for free text providers with shorter timeouts.
     * Uses 10-second read timeout to fail fast on slow/unresponsive sites.
     */
    @Bean("providerRestTemplate")
    public RestTemplate providerRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(10000); // 10 seconds
        restTemplate.setRequestFactory(factory);

        // Add User-Agent header for all requests
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            request.getHeaders().set("User-Agent", "library.muczynskifamily.com");
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(interceptor));

        return restTemplate;
    }

    /**
     * Configure ObjectMapper to serialize all datetime types as ISO-8601 strings
     * instead of arrays. This ensures consistent datetime serialization across
     * the application and makes frontend caching comparisons reliable.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8+ date/time types (LocalDateTime, etc.)
        // Add custom deserializer to handle date-only strings ("2026-01-01") for LocalDateTime fields,
        // since HTML date inputs don't include a time component.
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
        mapper.registerModule(timeModule);

        // Disable writing dates as timestamps (arrays)
        // This makes LocalDateTime serialize as "2025-12-06T14:30:00" instead of [2025,12,6,14,30,0]
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
