/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.YdlLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YdlLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate ydlRestTemplate;

    @InjectMocks
    private YdlLookupService ydlLookupService;

    private static final String RESPONSE_WITH_ALL_FORMATS = """
            {"totalPages":1,"page":0,"totalResults":1,"data":[
              {"title":"Test Book","primaryAgent":{"label":"Author, Test"},
               "materialTabs":[
                 {"name":"Book","type":"physical"},
                 {"name":"Audio Books","type":"physical"},
                 {"name":"Ebook","type":"electronic"}
               ]}
            ]}
            """;

    private static final String RESPONSE_EBOOK_ONLY = """
            {"totalPages":1,"page":0,"totalResults":1,"data":[
              {"title":"Test Book","primaryAgent":{"label":"Author, Test"},
               "materialTabs":[
                 {"name":"Ebook","type":"electronic"}
               ]}
            ]}
            """;

    private static final String RESPONSE_NO_MATCH = """
            {"totalPages":0,"page":0,"totalResults":0,"data":[]}
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
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertTrue(result.getPaperAvailable());
        assertTrue(result.getEbookAvailable());
        assertTrue(book.getYdlAudioAvailable());
        assertTrue(book.getYdlPaperAvailable());
        assertTrue(book.getYdlEbookAvailable());
        assertNull(book.getYdlLookupError());
        assertNotNull(book.getYdlLastChecked());
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
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(RESPONSE_EBOOK_ONLY);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

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
        book.setYdlAudioAvailable(true);
        book.setYdlPaperAvailable(true);
        book.setYdlEbookAvailable(true);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(RESPONSE_NO_MATCH);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by YDL", result.getErrorMessage());
        assertEquals("Not held by YDL", book.getYdlLookupError());

        assertFalse(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
        assertFalse(result.getEbookAvailable());
        assertFalse(book.getYdlAudioAvailable());
        assertFalse(book.getYdlPaperAvailable());
        assertFalse(book.getYdlEbookAvailable());
    }

    @Test
    void lookupAndUpdateBook_restTemplateThrows_setsErrorMessage() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenThrow(new RuntimeException("connection timed out"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(book.getYdlLookupError().contains("connection timed out"));
    }

    @Test
    void lookupAndUpdateBook_bookNotFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.muczynski.library.exception.LibraryException.class,
                () -> ydlLookupService.lookupAndUpdateBook(99L));
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
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(ydlRestTemplate).postForObject(anyString(), captor.capture(), any());
        String searchText = (String) captor.getValue().getBody().get("searchText");
        assertEquals("\"Test Book\" Author", searchText);
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
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Crucial Conversations","primaryAgent":{"label":"Patterson, Kerry"},
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(RESPONSE_NO_MATCH, responseShortTitle);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

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
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Crucial Conversations Tools For Talking",
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseNoAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        // Matched on the first (title + author) attempt despite the entry having no author on
        // record, since the title is long enough (>4 words) to be trusted on its own.
        verify(ydlRestTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), any());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchAuthorlessEntry_whenTitleIsShort() {
        // A short (<=4 word) title always needs an author match, on every search attempt -
        // an entry with no author on record can never satisfy that, regardless of which
        // query variant found it.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Short Title");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        String responseNoAuthor = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Short Title",
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseNoAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by YDL", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchUnrelatedTitleStartingWithSameWord_noAuthor() {
        // Regression test: a book titled just "Ellen" with no author on record must not match
        // an unrelated YDL title that merely starts with the same word, e.g. "Ellen G White Story".
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Ellen");

        String responseUnrelated = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Ellen G White Story",
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseUnrelated);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by YDL", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchUnrelatedLongerTitle_regressionForJoshua() {
        // Regression test from the real reported bug: "Joshua" falsely matched YDL's
        // "Joshua in a Troubled World".
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Joshua");

        String responseUnrelated = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Joshua in a Troubled World",
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseUnrelated);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by YDL", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchMidWordPrefix() {
        // "Ellen" must not match "Ellenwood" - not a word-boundary prefix, just shared letters.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Ellen");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        String responseMidWord = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Ellenwood","primaryAgent":{"label":"Author, Test"},
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseMidWord);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchWhenTitlesDifferInLength_noFuzzyMatching() {
        // Fuzzy/prefix title matching has been removed entirely: a short title that is only
        // the "core" of a longer YDL title (with a subtitle we don't have) no longer matches
        // by itself, even with a corroborating author. Our own colon-truncation cascade is the
        // supported way to bridge subtitle differences (see the fallback test above) - it
        // still requires an exact match against the truncated candidate title.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Crucial Conversations");
        Author author = new Author();
        author.setName("Kerry Patterson");
        book.setAuthor(author);

        String responseLongerTitle = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Crucial Conversations Tools For Talking When Stakes Are High",
                   "primaryAgent":{"label":"Patterson, Kerry"},
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseLongerTitle);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchSubstringAuthorName() {
        // Author matching is a whole-word match, not a substring match: our author last name
        // "White" must not match an entry whose author label is "Whitehead, John".
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Short Title");
        Author author = new Author();
        author.setName("Ellen White");
        book.setAuthor(author);

        String responseDifferentAuthor = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Short Title","primaryAgent":{"label":"Whitehead, John"},
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseDifferentAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_matchesExactAuthorWordAmongMultipleWords() {
        // Author matching checks each word in the label individually, so a "Last, First"
        // formatted label still matches on the last-name word.
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Short Title");
        Author author = new Author();
        author.setName("Ellen White");
        book.setAuthor(author);

        String responseMatchingAuthor = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Short Title","primaryAgent":{"label":"White, Ellen G."},
                   "materialTabs":[{"name":"Book","type":"physical"}]}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(responseMatchingAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
    }
}
