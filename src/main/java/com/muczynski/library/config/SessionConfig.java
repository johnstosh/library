/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

/**
 * Configuration for database-backed HTTP sessions using Spring Session JDBC.
 * This allows sessions to persist across server restarts.
 *
 * Only enabled when spring.session.store-type=jdbc is set in application.properties.
 * Disabled for tests (which use spring.session.store-type=none).
 */
@Configuration
@EnableJdbcHttpSession
@ConditionalOnProperty(name = "spring.session.store-type", havingValue = "jdbc")
public class SessionConfig {
    // Spring Session will automatically create the necessary session tables
    // and manage session persistence in the database
}
