/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookLabels;
import com.muczynski.library.dto.IllegalGenresMaintenanceDto;
import com.muczynski.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    /**
     * Counts books that have at least one illegal or plural-mismatched genre tag.
     * Does NOT mutate data. Used for the "Recalc" button on Data Management page.
     */
    @Transactional(readOnly = true)
    public IllegalGenresMaintenanceDto countIllegalGenres() {
        List<Book> allBooks = bookRepository.findAll();
        long booksAffected = 0;

        for (Book book : allBooks) {
            if (hasIllegalOrMismatchedGenres(book.getTagsList())) {
                booksAffected++;
            }
        }

        String message = booksAffected == 0
                ? "All genres are clean. No books have illegal or mismatched tags."
                : booksAffected + " book(s) have illegal or mismatched genre tags.";

        return IllegalGenresMaintenanceDto.builder()
                .booksAffected(booksAffected)
                .booksScanned(allBooks.size())
                .message(message)
                .build();
    }

    /**
     * Cleans illegal genres from all books:
     * 1. Normalizes tags using BookLabels (plurals, variants, case, separators).
     * 2. Removes any tags that don't map to canonical labels.
     * 3. Deduplicates preserving first-occurrence order.
     * 4. Only persists if tags actually changed.
     *
     * Returns summary with counts for UI Results column.
     */
    @Transactional
    public IllegalGenresMaintenanceDto cleanupIllegalGenres() {
        List<Book> allBooks = bookRepository.findAll();
        AtomicLong booksUpdated = new AtomicLong(0);
        AtomicLong pluralCorrections = new AtomicLong(0);
        AtomicLong illegalRemoved = new AtomicLong(0);

        for (Book book : allBooks) {
            List<String> originalTags = book.getTagsList();
            List<String> cleanedTags = BookLabels.cleanupTags(originalTags);

            // Count corrections for reporting (approximate by comparing sizes and content)
            long correctionsForBook = countCorrections(originalTags, cleanedTags);
            pluralCorrections.addAndGet(correctionsForBook);

            long removedForBook = originalTags.size() - cleanedTags.size();
            illegalRemoved.addAndGet(Math.max(0, removedForBook));

            if (!originalTags.equals(cleanedTags)) {
                book.setTagsList(cleanedTags);
                booksUpdated.incrementAndGet();
                // Repository save is handled by transactional flush at end
            }
        }

        long booksScanned = allBooks.size();
        long affected = booksUpdated.get() + (pluralCorrections.get() > 0 || illegalRemoved.get() > 0 ? 1 : 0); // conservative

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
