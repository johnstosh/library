/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles database migrations that need to run at application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigration {

    private final UserRepository userRepository;

    /**
     * Generate UUIDs for existing users that don't have a userIdentifier.
     * This migration runs once at application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateUserIdentifiers() {
        log.info("Checking for users without userIdentifier...");

        int migratedCount = 0;
        for (User user : userRepository.findAll()) {
            if (user.getUserIdentifier() == null || user.getUserIdentifier().isEmpty()) {
                user.setUserIdentifier(UUID.randomUUID().toString());
                userRepository.save(user);
                migratedCount++;
                log.debug("Generated userIdentifier for user: {} (ID: {})", user.getUsername(), user.getId());
            }
        }

        if (migratedCount > 0) {
            log.info("Migration complete: Generated userIdentifier for {} users", migratedCount);
        } else {
            log.info("No users need userIdentifier migration");
        }
    }
}
