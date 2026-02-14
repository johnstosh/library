/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    // Spring Session will automatically create the necessary session tables
    // and manage session persistence in the database

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
}
