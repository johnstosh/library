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

    @Test
    void exactSameFoldedTitleAndAuthorScoreIsOne() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Jesus of Nazareth", null, "Joseph Ratzinger", BookStatus.ACTIVE),
                // Different primary but shared alternate that matches exactly after fold
                proj(2L, "Completely Unrelated Primary Alpha", "Jesus of Nazareth",
                        "Joseph Ratzinger", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getPairs().size());
        double score = result.getPairs().get(0).getScore();
        assertEquals(1.0, score, 1e-9,
                "exact identical folded title+author must score 1.0, got " + score);
    }

    @Test
    void authorPrefixDifferenceScoresBelowOneButAboveThreshold() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Jesus of Nazareth", null, "Joseph Ratzinger", BookStatus.ACTIVE),
                proj(2L, "Jesus of Nazareth", null,
                        "Joseph Aloisius Ratzinger", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getPairs().size(),
                "near-identical title with author middle-name difference should still match");
        double score = result.getPairs().get(0).getScore();
        assertTrue(score < 1.0,
                "author prefix difference must pull blended score below 1.0, got " + score);
        assertTrue(score >= DuplicateTitleService.SIMILARITY_THRESHOLD,
                "should remain above threshold, got " + score);
        // Must expose finer-than-4dp usefulness (not a flat 1.0000)
        String formatted = String.format(java.util.Locale.US, "%.6f", score);
        assertNotEquals("1.000000", formatted);
        assertTrue(formatted.length() >= 8, "formatted 6dp score should be useful: " + formatted);
    }

    @Test
    void round6PreservesFinerThanFourDecimalPlaces() {
        // 0.85*0.999999 + 0.15*0.999990 ≈ value that round4 would crush
        double blended = DuplicateTitleService.blendScore(0.999999, 0.999990);
        double r6 = DuplicateTitleService.round6(blended);
        double r4 = Math.round(blended * 10000.0) / 10000.0;
        assertNotEquals(r4, r6, 0.0);
        // 6dp string must not collapse to 4 meaningful digits only
        String s = String.format(java.util.Locale.US, "%.6f", r6);
        assertTrue(s.matches("0\\.\\d{6}"), "expected 6 fractional digits, got " + s);
    }

    @Test
    void levenshteinSimilarityIdenticalIsOne() {
        assertEquals(1.0, DuplicateTitleService.levenshteinSimilarity("abc", "abc"));
        assertEquals(0.0, DuplicateTitleService.levenshteinSimilarity("", "abc"));
        assertTrue(DuplicateTitleService.levenshteinSimilarity("kitten", "sitting") < 1.0);
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
