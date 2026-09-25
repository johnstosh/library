/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.BookStatus;
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
                proj(1L, "Gather Comprehensive", null, "Author A", BookStatus.ACTIVE),
                proj(2L, "Gather Comprehensive, c. 2", null, "Author A", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(2, result.getBooksScanned());
        assertEquals(1, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty(),
                "exact copy-suffix duplicates must collapse to one representative");
    }

    @Test
    void copySuffixVariantsWithCDotNDoNotPair() {
        // "101 Things…" and "101 Things…, c. 1" must NOT pair — collapse only
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(5L, "101 Things to Do with a Baby", null, "Author", BookStatus.ACTIVE),
                proj(6L, "101 Things to Do with a Baby, c. 1", null, "Author", BookStatus.WITHDRAWN)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty());
    }

    @Test
    void volumeTwinsSameAuthorCollapseToOneRepresentative() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Suma domestica Volume 1", null, "Author", BookStatus.ACTIVE),
                proj(2L, "Suma domestica Volume 2", null, "Author", BookStatus.ACTIVE),
                proj(3L, "Suma domestica Volume 3", null, "Author", BookStatus.WITHDRAWN)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty());
    }

    @Test
    void spyXFamilyVolumeFormsCollapse() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(10L, "Spy x Family, v. 4", null, "Endo", BookStatus.ACTIVE),
                proj(11L, "Spy x Family, v.4", null, "Endo", BookStatus.ACTIVE),
                proj(12L, "Spy x Family, v 4", null, "Endo", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty());
    }

    @Test
    void differentAuthorsSameTitleDoNotPairHigh() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "The Confessions", null, "Augustine", BookStatus.ACTIVE),
                proj(2L, "The Confessions", null, "Rousseau", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        // Different authors → separate representatives, but author gate blocks the pair
        assertEquals(2, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty(),
                "same title with different authors must not produce a high pair");
    }

    @Test
    void nearDuplicateTitlesWithMisspellingAreReported() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(10L, "The Confessions of Augustine", null, "Augustine", BookStatus.ACTIVE),
                proj(20L, "Confession of Augustine", null, "Augustine", BookStatus.ACTIVE),
                proj(30L, "Moby Dick", null, "Melville", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertFalse(result.getPairs().isEmpty());
        DuplicateTitlePairDto pair = result.getPairs().get(0);
        assertTrue(
                (pair.getBookAId() == 10L && pair.getBookBId() == 20L)
                        || (pair.getBookAId() == 20L && pair.getBookBId() == 10L));
        assertTrue(pair.getScore() >= DuplicateTitleService.SIMILARITY_THRESHOLD);
        assertEquals("ACTIVE", pair.getBookAStatus());
        assertEquals("ACTIVE", pair.getBookBStatus());
        // Unrelated Moby Dick should not pair with confessions
        assertTrue(result.getPairs().stream()
                .noneMatch(p -> p.getBookAId() == 30L || p.getBookBId() == 30L));
    }

    @Test
    void usesAlternateTitlesForMatching() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Primary Unique Name XYZ", "City of God", "Augustine", BookStatus.ACTIVE),
                proj(2L, "Completely Different Primary", "City of God Treatise", "Augustine", BookStatus.LOST)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getPairs().size());
        assertTrue(result.getPairs().get(0).getScore() >= DuplicateTitleService.SIMILARITY_THRESHOLD);
        assertEquals("ACTIVE", result.getPairs().get(0).getBookAStatus());
        assertEquals("LOST", result.getPairs().get(0).getBookBStatus());
    }

    @Test
    void resultsSortedByScoreDescendingAndCappedAt100() {
        List<BookRepository.DuplicateTitleProjection> books = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            books.add(proj((long) i, "Novella Number " + i, null, "Author", BookStatus.ACTIVE));
            books.add(proj(1000L + i, "Novella Numbr " + i, null, "Author", BookStatus.ACTIVE));
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
            Long id, String title, String alt, String author, BookStatus status) {
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

            @Override
            public BookStatus getStatus() {
                return status;
            }
        };
    }
}
