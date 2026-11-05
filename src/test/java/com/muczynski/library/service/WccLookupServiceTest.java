/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.WccLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WccLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate wccRestTemplate;

    @InjectMocks
    private WccLookupService wccLookupService;

    private static final String RESPONSE_WITH_ALL_FORMATS = """
            {"info":{"totalResultsLocal":3},"docs":[
              {"context":"L","pnx":{"display":{"type":["book"],"title":["Test Book"],"format":["300 pages"]},
               "addata":{"au":["Author, Test"]}}},
              {"context":"L","pnx":{"display":{"type":["audio"],"title":["Test Book"],"format":["1 online resource (5 hours)"]},
               "addata":{"au":["Author, Test"]}}},
              {"context":"L","pnx":{"display":{"type":["book"],"title":["Test Book"],"format":["1 online resource"]},
               "addata":{"au":["Author, Test"]}}}
            ]}
            """;

    private static final String RESPONSE_EBOOK_ONLY = """
            {"info":{"totalResultsLocal":1},"docs":[
              {"context":"L","pnx":{"display":{"type":["book"],"title":["Test Book"],"format":["1 online resource"]},
               "addata":{"au":["Author, Test"]}}}
            ]}
            """;

    private static final String RESPONSE_NO_MATCH = """
            {"info":{"totalResultsLocal":0},"docs":[]}
            """;

    @Test
    void lookupAndUpdateBook_marksAllThreeFormatsAvailable() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertTrue(result.getPaperAvailable());
        assertTrue(result.getEbookAvailable());
        assertTrue(book.getWccAudioAvailable());
        assertTrue(book.getWccPaperAvailable());
        assertTrue(book.getWccEbookAvailable());
        assertNull(book.getWccLookupError());
        assertNotNull(book.getWccLastChecked());
    }

    @Test
    void lookupAndUpdateBook_ebookOnly_marksPaperAndAudioFalse() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_EBOOK_ONLY);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());
        assertFalse(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_noMatch_setsLookupError() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Nonexistent Book Title");
        // Simulate stale availability from a previous lookup (or a manual override) that a
        // fresh "not held" result must clear rather than leave untouched.
        book.setWccAudioAvailable(true);
        book.setWccPaperAvailable(true);
        book.setWccEbookAvailable(true);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Washtenaw Community College", result.getErrorMessage());
        assertEquals("Not held by Washtenaw Community College", book.getWccLookupError());

        assertFalse(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
        assertFalse(result.getEbookAvailable());
        assertFalse(book.getWccAudioAvailable());
        assertFalse(book.getWccPaperAvailable());
        assertFalse(book.getWccEbookAvailable());
    }

    @Test
    void lookupAndUpdateBook_restTemplateThrows_setsErrorMessage() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new RuntimeException("connection timed out"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(book.getWccLookupError().contains("connection timed out"));
    }

    @Test
    void lookupAndUpdateBook_bookNotFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.muczynski.library.exception.LibraryException.class,
                () -> wccLookupService.lookupAndUpdateBook(99L));
    }

    @Test
    void lookupAndUpdateBook_stripsCopyAndFormatSuffixesBeforeMatching() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book, c. 2 (DVD)");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        verify(wccRestTemplate).getForObject(urlCaptor.capture(), eq(String.class));
        String url = urlCaptor.getValue().toString();
        assertTrue(url.contains("Test+Book"));
        assertFalse(url.toLowerCase(java.util.Locale.ROOT).contains("dvd"));
    }

    @Test
    void lookupAndUpdateBook_fallsBackToColonTruncatedTitle_whenFullTitleSearchFindsNothing() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Crucial Conversations: Tools for Talking When Stakes Are High");
        Author author = new Author();
        author.setName("Kerry Patterson");
        book.setAuthor(author);

        // Truncated title is only 2 words, so an author match is required to confirm it.
        String responseShortTitle = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Crucial Conversations"],"format":["300 pages"]},
                   "addata":{"au":["Patterson, Kerry"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH, responseShortTitle);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertEquals("Crucial Conversations", result.getMatchedTitle());
    }

    @Test
    void lookupAndUpdateBook_matchesAuthorlessEntry_whenTitleIsLongEnough() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Crucial Conversations Tools For Talking");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        String responseNoAuthor = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Crucial Conversations Tools For Talking"],"format":["300 pages"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseNoAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        // Matched on the first (title + author) attempt despite the entry having no author on
        // record, since the title is long enough (>4 words) to be trusted on its own.
        verify(wccRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchAuthorlessEntry_whenTitleIsShort() {
        // A short (<=4 word) title always needs an author match, on every search attempt - an
        // entry with no author on record can never satisfy that, regardless of which query
        // variant found it.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Short Title");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        String responseNoAuthor = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Short Title"],"format":["300 pages"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseNoAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Washtenaw Community College", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_excludesCentralIndexEntries_notLocallyHeld() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        String responseCentralOnly = """
                {"info":{"totalResultsLocal":0},"docs":[
                  {"context":"PC","pnx":{"display":{"type":["article"],"title":["Test Book"],"format":["online"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseCentralOnly);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Washtenaw Community College", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchUnrelatedTitleStartingWithSameWord_noAuthor() {
        // Regression test: a book titled just "Ellen" with no author on record must not match
        // an unrelated WCC title that merely starts with the same word.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Ellen");

        String responseUnrelated = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Ellen G White Story"],"format":["300 pages"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseUnrelated);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Washtenaw Community College", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchUnrelatedLongerTitle_regressionForJoshua() {
        // Regression test from the real reported bug: "Joshua" falsely matched an unrelated
        // "Joshua in a Troubled World".
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Joshua");

        String responseUnrelated = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Joshua in a Troubled World"],"format":["300 pages"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseUnrelated);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Washtenaw Community College", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchSubstringAuthorName() {
        // Author matching is a whole-word match, not a substring match: our author last name
        // "White" must not match an entry whose author is "Whitehead, John".
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Short Title");
        Author author = new Author();
        author.setName("Ellen White");
        book.setAuthor(author);

        String responseDifferentAuthor = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Short Title"],"format":["300 pages"]},
                   "addata":{"au":["Whitehead, John"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseDifferentAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_matchesExactAuthorWordAmongAdditionalAuthors() {
        // Author matching checks the "addau" (additional authors) array too, word by word.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Short Title");
        Author author = new Author();
        author.setName("Ellen White");
        book.setAuthor(author);

        String responseMatchingAuthor = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Short Title"],"format":["300 pages"]},
                   "addata":{"au":["Main, Author"],"addau":["White, Ellen G."]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseMatchingAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_noAlternateTitle_usesOnlyPrimaryUnchanged() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getAudioAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getPaperAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getEbookAvailable()));
    }

    @Test
    void lookupAndUpdateBook_withAlternateTitle_searchesBothAndMergesFlags() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAlternateTitles(List.of("Test Book Alternate"));
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        // primary no match, alternate succeeds
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH)
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getAudioAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getPaperAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getEbookAvailable()));
    }

    @Test
    void lookupAndUpdateBook_matchesAlternateAuthorLastName() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Confessions");
        Author author = new Author();
        author.setName("Aurelius Augustinus");
        author.setAlternateNames(List.of("Saint Augustine"));
        book.setAuthor(author);

        String heldAsAugustine = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Confessions"],"format":["300 pages"]},
                   "addata":{"au":["Augustine, Saint"]}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(wccRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(heldAsAugustine);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertFalse(result.getAudioAvailable());
        assertFalse(result.getEbookAvailable());
    }
}
