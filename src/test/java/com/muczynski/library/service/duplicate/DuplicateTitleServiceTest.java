/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateTitleServiceTest {

    private BookRepository bookRepository;
    private DuplicateTitleService service;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        service = new DuplicateTitleService(bookRepository);
    }

    @Test
    void copySuffixDuplicatesAreNotReportedAsPairs() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Gather Comprehensive", null, "Author A"),
                proj(2L, "Gather Comprehensive, c. 2", null, "Author A")
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(2, result.getBooksScanned());
        assertEquals(1, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty(),
                "exact copy-suffix duplicates must collapse to one representative");
    }

    @Test
    void nearDuplicateTitlesWithMisspellingAreReported() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(10L, "The Confessions of Augustine", null, "Augustine"),
                proj(20L, "Confession of Augustine", null, "Augustine"),
                proj(30L, "Moby Dick", null, "Melville")
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertFalse(result.getPairs().isEmpty());
        DuplicateTitlePairDto pair = result.getPairs().get(0);
        assertTrue(
                (pair.getBookAId() == 10L && pair.getBookBId() == 20L)
                        || (pair.getBookAId() == 20L && pair.getBookBId() == 10L));
        assertTrue(pair.getScore() >= DuplicateTitleService.SIMILARITY_THRESHOLD);
        // Unrelated Moby Dick should not pair with confessions
        assertTrue(result.getPairs().stream()
                .noneMatch(p -> p.getBookAId() == 30L || p.getBookBId() == 30L));
    }

    @Test
    void usesAlternateTitlesForMatching() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Primary Unique Name XYZ", "City of God", "Augustine"),
                proj(2L, "Completely Different Primary", "City of God Treatise", "Augustine")
        );
        // After subtitle truncate: "City of God" vs "City of God" — should match via alt titles
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getPairs().size());
        assertTrue(result.getPairs().get(0).getScore() >= DuplicateTitleService.SIMILARITY_THRESHOLD);
    }

    @Test
    void resultsSortedByScoreDescendingAndCappedAt100() {
        List<BookRepository.DuplicateTitleProjection> books = new ArrayList<>();
        // Create 120 near-identical pairs via numbered titles that share keyword "novella"
        for (int i = 1; i <= 120; i++) {
            books.add(proj((long) i, "Novella Number " + i, null, "Author"));
            books.add(proj(1000L + i, "Novella Numbr " + i, null, "Author")); // misspelling
        }
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertTrue(result.getPairs().size() <= DuplicateTitleService.MAX_RESULTS);
        List<DuplicateTitlePairDto> pairs = result.getPairs();
        for (int i = 1; i < pairs.size(); i++) {
            assertTrue(pairs.get(i - 1).getScore() >= pairs.get(i).getScore(),
                    "pairs must be sorted by score descending");
        }
    }

    private static BookRepository.DuplicateTitleProjection proj(
            Long id, String title, String alt, String author) {
        return new BookRepository.DuplicateTitleProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getAlternateTitle() {
                return alt;
            }

            @Override
            public String getAuthorName() {
                return author;
            }
        };
    }
}
