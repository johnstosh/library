/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.DuplicateTitleDismissal;
import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.DuplicateTitleDismissalRepository;
import com.muczynski.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuplicateTitleServiceTest {

    private BookRepository bookRepository;
    private DuplicateTitleDismissalRepository dismissalRepository;
    private DuplicateTitleService service;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        dismissalRepository = mock(DuplicateTitleDismissalRepository.class);
        AuthorRepository authorRepository = mock(AuthorRepository.class);
        when(dismissalRepository.findAll()).thenReturn(List.of());
        when(dismissalRepository.findByPairKey(anyString())).thenReturn(Optional.empty());
        when(bookRepository.findDuplicateTitleFreeText(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(authorRepository.findDuplicateTitleAuthorNames(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        BookService bookService = mock(BookService.class);
        lenient().when(bookService.allocateFixTitles(nullable(String.class), anyLong(), nullable(String.class), anyLong()))
                .thenAnswer(invocation -> new BookService.FixCopyTitles(
                        invocation.getArgument(0), invocation.getArgument(2)));
        service = new DuplicateTitleService(bookRepository, dismissalRepository, authorRepository, bookService);
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

        assertEquals(1, result.getBooksScanned());
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

        assertEquals(2, result.getBooksScanned());
        assertEquals(1, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty());
    }

    @Test
    void withdrawnBooksAreExcludedFromScanAndPairs() {
        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(1L, "Gather Comprehensive", null, "Author A", BookStatus.WITHDRAWN),
                proj(2L, "Gather Comprehensive, c. 2", null, "Author A", BookStatus.ACTIVE),
                proj(3L, "The Confessions of Augustine", null, "Augustine", BookStatus.ACTIVE),
                proj(4L, "Confession of Augustine", null, "Augustine", BookStatus.WITHDRAWN),
                proj(5L, "City of God", null, "Augustine", BookStatus.WITHDRAWN),
                proj(6L, "The City of God", null, "Augustine", BookStatus.WITHDRAWN)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(2, result.getBooksScanned());
        assertEquals(2, result.getRepresentativesCompared());
        assertTrue(result.getPairs().isEmpty(),
                "a withdrawn near-duplicate must not pair with an active book");
        assertTrue(result.getPairs().stream().noneMatch(p ->
                p.getBookAStatus().equals("WITHDRAWN") || p.getBookBStatus().equals("WITHDRAWN")));
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
    void dismissedCopyIsHiddenAndADifferentAuthorStays() {
        DuplicateTitlePairKey key = DuplicateTitlePairKey.fromRaw(
                "The Confessions of Augustine, c. 2", "Augustine",
                "Confession of Augustine", "Augustine");
        when(dismissalRepository.findAll()).thenReturn(List.of(stored(key)));

        List<BookRepository.DuplicateTitleProjection> books = List.of(
                proj(50L, "The Confessions of Augustine, c. 3", null, "Augustine", BookStatus.ACTIVE),
                proj(60L, "Confession of Augustine", null, "Augustine", BookStatus.LOST),
                proj(70L, "The Confessions of Augustine", null, "Jean-Jacques Rousseau", BookStatus.ACTIVE),
                proj(80L, "Confession of Augustine", null, "Jean-Jacques Rousseau", BookStatus.ACTIVE)
        );
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(1, result.getPairs().size());
        DuplicateTitlePairDto pair = result.getPairs().get(0);
        assertEquals(Set.of(70L, 80L), Set.of(pair.getBookAId(), pair.getBookBId()));
    }

    @Test
    void dismissedPairDoesNotConsumeTheResultCap() {
        List<BookRepository.DuplicateTitleProjection> books = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            books.add(proj((long) i, "Novella Number " + i, null, "Author", BookStatus.ACTIVE));
            books.add(proj(1000L + i, "Novella Numbr " + i, null, "Author", BookStatus.ACTIVE));
        }
        when(bookRepository.findAllForDuplicateTitleScan()).thenReturn(books);

        DuplicateTitlesResultDto full = service.findDuplicates();
        assertEquals(DuplicateTitleService.MAX_RESULTS, full.getPairs().size());
        DuplicateTitlePairDto hidden = full.getPairs().get(0);
        when(dismissalRepository.findAll()).thenReturn(List.of(stored(DuplicateTitlePairKey.fromRaw(
                hidden.getBookATitle(), hidden.getBookAAuthorName(),
                hidden.getBookBTitle(), hidden.getBookBAuthorName()))));

        DuplicateTitlesResultDto result = service.findDuplicates();

        assertEquals(DuplicateTitleService.MAX_RESULTS, result.getPairs().size(),
                "a hidden pair must be dropped before the top-100 cap so another pair fills the slot");
        assertTrue(result.getPairs().stream().noneMatch(pair ->
                Set.of(pair.getBookAId(), pair.getBookBId())
                        .equals(Set.of(hidden.getBookAId(), hidden.getBookBId()))));
    }

    @Test
    void dismissStoresFoldedTitlesInCanonicalOrder() {
        service.dismiss("Confession of Augustine", "Augustine",
                "The Confessions of Augustine, c. 2", "Augustine");

        ArgumentCaptor<DuplicateTitleDismissal> saved = ArgumentCaptor.forClass(DuplicateTitleDismissal.class);
        verify(dismissalRepository).save(saved.capture());
        DuplicateTitleDismissal row = saved.getValue();
        assertEquals("confession of augustine", row.getTitleA());
        assertEquals("augustine", row.getAuthorA());
        assertEquals("the confessions of augustine", row.getTitleB());
        assertEquals("augustine", row.getAuthorB());
        assertEquals(DuplicateTitlePairKey.fromRaw(
                "The Confessions of Augustine", "Augustine",
                "Confession of Augustine", "Augustine").pairKey(), row.getPairKey());
    }

    @Test
    void dismissOfAnExistingMarkDoesNotSaveAgain() {
        DuplicateTitlePairKey key = DuplicateTitlePairKey.fromRaw(
                "Confession of Augustine", "Augustine",
                "The Confessions of Augustine", "Augustine");
        when(dismissalRepository.findByPairKey(key.pairKey())).thenReturn(Optional.of(stored(key)));

        service.dismiss("The Confessions of Augustine, c. 4", "Augustine",
                "Confession of Augustine", "Augustine");

        verify(dismissalRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void restoreDeletesTheStoredMark() {
        DuplicateTitlePairKey key = DuplicateTitlePairKey.fromRaw(
                "Confession of Augustine", "Augustine",
                "The Confessions of Augustine, vol. 2", "Augustine");
        DuplicateTitleDismissal row = stored(key);
        when(dismissalRepository.findByPairKey(key.pairKey())).thenReturn(Optional.of(row));

        service.restore("The Confessions of Augustine", "Augustine", "Confession of Augustine", "Augustine");

        verify(dismissalRepository).delete(row);
    }

    @Test
    void blankTitleCannotBeMarked() {
        LibraryException ex = assertThrows(LibraryException.class,
                () -> service.dismiss("   ", "Augustine", "Confession", "Augustine"));
        assertEquals("Both books need a title before this pair can be marked.", ex.getMessage());
    }

    @Test
    void levenshteinSimilarityIdenticalIsOne() {
        assertEquals(1.0, DuplicateTitleService.levenshteinSimilarity("abc", "abc"));
        assertEquals(0.0, DuplicateTitleService.levenshteinSimilarity("", "abc"));
        assertTrue(DuplicateTitleService.levenshteinSimilarity("kitten", "sitting") < 1.0);
    }

    private static DuplicateTitleDismissal stored(DuplicateTitlePairKey key) {
        DuplicateTitleDismissal row = new DuplicateTitleDismissal();
        row.setPairKey(key.pairKey());
        row.setTitleA(key.titleA());
        row.setAuthorA(key.authorA());
        row.setTitleB(key.titleB());
        row.setAuthorB(key.authorB());
        return row;
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

            @Override
            public Long getAuthorId() {
                return null;
            }

            @Override
            public String getLocNumber() {
                return null;
            }

            @Override
            public Boolean getElectronicResource() {
                return null;
            }
        };
    }
}
