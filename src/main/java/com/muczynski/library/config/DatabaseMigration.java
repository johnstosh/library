/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles one-time database migrations that run at application startup.
 * Each migration is gated by a property (default false) so it only runs when explicitly enabled,
 * avoiding expensive full-table scans on every startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigration {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Value("${app.migration.backfill-user-identifiers:false}")
    private boolean backfillUserIdentifiers;

    @Value("${app.migration.backfill-book-last-modified:false}")
    private boolean backfillBookLastModified;

    /**
     * Generate UUIDs for existing users that don't have a userIdentifier.
     * Enable with app.migration.backfill-user-identifiers=true
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateUserIdentifiers() {
        if (!backfillUserIdentifiers) {
            log.debug("Skipping user identifier migration (app.migration.backfill-user-identifiers=false)");
            return;
        }
        log.info("Checking for users without userIdentifier...");

        int migratedCount = 0;
        for (com.muczynski.library.domain.User user : userRepository.findAll()) {
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

    /**
     * Populate lastModified for existing books that don't have this field set.
     * Uses a single bulk UPDATE rather than a per-row loop to avoid a slow full-table scan.
     * Enable with app.migration.backfill-book-last-modified=true
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateBookLastModified() {
        if (!backfillBookLastModified) {
            log.debug("Skipping book lastModified migration (app.migration.backfill-book-last-modified=false)");
            return;
        }
        log.info("Backfilling lastModified for books where it is null...");
        int count = bookRepository.backfillLastModified(LocalDateTime.now());
        if (count > 0) {
            log.info("Migration complete: Set lastModified for {} books", count);
        } else {
            log.info("No books needed lastModified backfill");
        }
    }
}
