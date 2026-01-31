// (c) Copyright 2025 by Muczynski
package com.muczynski.library;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Auto-configuration that provides an embedded PostgreSQL database for all Spring Boot
 * tests via Testcontainers. This replaces the previous H2 in-memory database to ensure
 * tests run against the same database engine as production.
 */
@AutoConfiguration
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
