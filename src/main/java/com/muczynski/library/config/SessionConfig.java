/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Configuration for database-backed HTTP sessions using Spring Session JDBC.
 * This allows sessions to persist across server restarts.
 *
 * Only enabled when spring.session.store-type=jdbc is set in application.properties.
 * Disabled for tests (which use spring.session.store-type=none).
 */
@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 77 * 24 * 60 * 60) // 77 days
@ConditionalOnProperty(name = "spring.session.store-type", havingValue = "jdbc")
public class SessionConfig {

    private static final Logger logger = LoggerFactory.getLogger(SessionConfig.class);

    /**
     * Configure the session cookie to persist for 77 days (matching session timeout).
     * This ensures the browser keeps the session cookie for the full session duration.
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // Set cookie max age to 77 days in seconds (77 * 24 * 60 * 60)
        serializer.setCookieMaxAge(77 * 24 * 60 * 60);
        serializer.setCookiePath("/");
        serializer.setUseSecureCookie(false); // Allow both HTTP and HTTPS
        serializer.setSameSite("Lax");
        return serializer;
    }

    /**
     * Configure a lenient session deserializer that handles stale sessions gracefully.
     * When session attributes can't be deserialized (e.g., after a deployment changes
     * serialized lambda classes), the session is treated as empty and the user is
     * logged out cleanly rather than receiving a 500 error.
     */
    @Bean
    public SessionRepositoryCustomizer<JdbcIndexedSessionRepository> lenientSessionDeserializer() {
        return repository -> {
            GenericConversionService conversionService = new GenericConversionService();
            conversionService.addConverter(Object.class, byte[].class, new SerializingConverter());
            conversionService.addConverter(byte[].class, Object.class,
                    (Converter<byte[], Object>) source -> {
                        try {
                            return new DeserializingConverter().convert(source);
                        } catch (Exception e) {
                            // Stale session data from a previous deployment (e.g., serialized
                            // lambdas whose host class was changed). Return null so the session
                            // attribute is treated as missing; the user will be logged out cleanly.
                            logger.warn("Could not deserialize session attribute (stale session data): {}", e.getMessage());
                            return null;
                        }
                    });
            repository.setConversionService(conversionService);
        };
    }
}
