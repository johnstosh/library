/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookLabels;
import com.muczynski.library.dto.IllegalGenresMaintenanceDto;
import com.muczynski.library.repository.BookRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for data maintenance operations, starting with illegal genre cleanup.
 * Librarian-only. Called from MaintenanceController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int DEFAULT_BATCH_SIZE = 50;

    /**
     * Counts books that have at least one illegal or plural-mismatched genre tag.
     * Uses lightweight projection to avoid loading @Lob fields or full entities.
     * Does NOT mutate data. Used for the "Recalc" button on Data Management page.
     */
    @Transactional(readOnly = true)
    public IllegalGenresMaintenanceDto countIllegalGenres() {
        List<BookRepository.IllegalGenreTagsProjection> projections =
                bookRepository.findAllForGenreMaintenance();

        long booksAffected = 0;
        for (var proj : projections) {
            if (hasIllegalOrMismatchedGenres(proj.getTagsList())) {
                booksAffected++;
            }
        }

        String message = booksAffected == 0
                ? "All genres are clean. No books have illegal or mismatched tags."
                : booksAffected + " book(s) have illegal or mismatched genre tags.";

        return IllegalGenresMaintenanceDto.builder()
                .booksAffected(booksAffected)
                .booksScanned(projections.size())
                .message(message)
                .build();
    }

    /**
     * Cleans illegal genres from all books in batches of ~50:
     * 1. Uses lightweight projection (id + tagsList only, no LOBs).
     * 2. Normalizes tags using BookLabels.cleanupTags.
     * 3. Updates only changed books via findById + set + flush.
     * 4. Clears persistence context after each batch to prevent memory growth.
     *
     * Preserves exact semantics of prior implementation for reporting.
     */
    @Transactional
    public IllegalGenresMaintenanceDto cleanupIllegalGenres() {
        List<BookRepository.IllegalGenreTagsProjection> projections =
                bookRepository.findAllForGenreMaintenance();

        AtomicLong booksUpdated = new AtomicLong(0);
        AtomicLong pluralCorrections = new AtomicLong(0);
        AtomicLong illegalRemoved = new AtomicLong(0);

        int batchSize = DEFAULT_BATCH_SIZE;
        for (int i = 0; i < projections.size(); i += batchSize) {
            int end = Math.min(i + batchSize, projections.size());
            List<BookRepository.IllegalGenreTagsProjection> batch = projections.subList(i, end);

            for (var proj : batch) {
                List<String> originalTags = proj.getTagsList() != null ? proj.getTagsList() : List.of();
                List<String> cleanedTags = BookLabels.cleanupTags(originalTags);

                long correctionsForBook = countCorrections(originalTags, cleanedTags);
                pluralCorrections.addAndGet(correctionsForBook);

                long removedForBook = originalTags.size() - cleanedTags.size();
                illegalRemoved.addAndGet(Math.max(0, removedForBook));

                if (!originalTags.equals(cleanedTags)) {
                    // Load managed entity only for the books that need update (rare)
                    Book book = bookRepository.findById(proj.getId()).orElse(null);
                    if (book != null) {
                        book.setTagsList(cleanedTags);
                        booksUpdated.incrementAndGet();
                    }
                }
            }

            // Persistence hygiene: flush changes and clear context to release memory
            entityManager.flush();
            entityManager.clear();
            log.debug("Processed batch {}-{} of {} books", i, end - 1, projections.size());
        }

        long booksScanned = projections.size();
        String message = String.format(
                "Looked at %d books. Updated %d. Corrected %d plural or spelling variants. Removed %d tags that were not on the standard list.",
                booksScanned, booksUpdated.get(), pluralCorrections.get(), illegalRemoved.get());

        log.info("Genre maintenance completed: {}", message);

        return IllegalGenresMaintenanceDto.builder()
                .booksAffected(0) // After cleanup, should be 0
                .booksScanned(booksScanned)
                .booksUpdated(booksUpdated.get())
                .pluralCorrections(pluralCorrections.get())
                .illegalRemoved(illegalRemoved.get())
                .message(message)
                .build();
    }

    private boolean hasIllegalOrMismatchedGenres(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        for (String tag : tags) {
            if (!BookLabels.isValidLabel(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts how many tags were changed (for reporting purposes).
     * Simple heuristic: size difference + any that mapped differently.
     * Matches behavior from #347.
     */
    private long countCorrections(List<String> original, List<String> cleaned) {
        if (original == null || cleaned == null) return 0;
        long corrections = 0;
        int minSize = Math.min(original.size(), cleaned.size());
        for (int i = 0; i < minSize; i++) {
            String origNorm = BookLabels.normalizeTag(original.get(i));
            String cleanNorm = cleaned.get(i);
            if (!origNorm.equals(cleanNorm)) {
                corrections++;
            }
        }
        corrections += Math.abs(original.size() - cleaned.size());
        return corrections;
    }
}
