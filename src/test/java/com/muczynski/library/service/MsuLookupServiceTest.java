/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.MsuLookupResultDto;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MsuLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate msuRestTemplate;

    @InjectMocks
    private MsuLookupService msuLookupService;

    private static final String ALL_FORMATS = """
            {"status":"OK","records":[
              {"title":"Test Book / Author, Test.","shortTitle":"Test Book","formats":["Book"],
               "authors":{"primary":{"Author, Test":{"role":["author"]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["Audio"],
               "authors":{"primary":{"Author, Test":{"role":[]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["eBook"],
               "authors":{"primary":{"Author, Test":{"role":[]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["Book","Electronic"],
               "authors":{"primary":{"Author, Test":{"role":[]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["CD"],
               "authors":{"primary":[]}}
            ]}
            """;

    private static final String NONE = """
            {"status":"OK","resultCount":0,"records":[]}
            """;

    @Test
    void lookupAndUpdateBook_marksPaperEbookAndAudioFromMsuFormatLabels() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertTrue(result.getPaperAvailable());
        assertTrue(result.getEbookAvailable());
        assertTrue(book.getMsuAudioAvailable());
        assertTrue(book.getMsuPaperAvailable());
        assertTrue(book.getMsuEbookAvailable());
        assertNull(book.getMsuLookupError());
        assertNotNull(book.getMsuLastChecked());

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        verify(msuRestTemplate).getForObject(urlCaptor.capture(), eq(String.class));
        String url = urlCaptor.getValue().toString();
        assertTrue(url.startsWith(MsuLookupService.SEARCH_URL));
        assertTrue(url.contains("type=Title"));
        assertTrue(url.contains("lookfor=Test%20Book") || url.contains("lookfor=Test+Book"));
        assertTrue(url.contains("shortTitle"));
        assertFalse(url.contains("primo"));
    }

    @Test
    void lookupAndUpdateBook_bookPlusElectronicIsEbookNotPaper() {
        Book book = bookWithAuthor("Online Only", "Test Author");
        String body = """
                {"status":"OK","records":[
                  {"title":"Online Only","shortTitle":"Online Only","formats":["Book","Electronic"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());
        assertFalse(result.getPaperAvailable());
        assertFalse(result.getAudioAvailable());
    }

    @Test
    void lookupAndUpdateBook_matchesShortTitleAndAuthorKey() {
        Book book = bookWithAuthor("Short Title", "Ellen White");
        String body = """
                {"status":"OK","records":[
                  {"title":"Short Title / White, Ellen.","shortTitle":"Short Title","formats":["Book"],
                   "authors":{"primary":{"White, Ellen G.":{"role":["author"]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertEquals("Short Title", result.getMatchedTitle());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchSubstringAuthor() {
        Book book = bookWithAuthor("Short Title", "Ellen White");
        String body = """
                {"status":"OK","records":[
                  {"title":"Short Title","shortTitle":"Short Title","formats":["Book"],
                   "authors":{"primary":{"Whitehead, John":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Michigan State University", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchLongerTitle() {
        Book book = bookWithAuthor("Joshua", "Test Author");
        String body = """
                {"status":"OK","records":[
                  {"title":"Joshua in a Troubled World","shortTitle":"Joshua in a Troubled World","formats":["Book"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_noMatch_clearsStaleHoldings() {
        Book book = bookWithAuthor("Nonexistent Book Title", "Test Author");
        book.setMsuAudioAvailable(true);
        book.setMsuPaperAvailable(true);
        book.setMsuEbookAvailable(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(NONE);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Michigan State University", book.getMsuLookupError());
        assertFalse(book.getMsuAudioAvailable());
        assertFalse(book.getMsuPaperAvailable());
        assertFalse(book.getMsuEbookAvailable());
    }

    @Test
    void lookupAndUpdateBook_htmlBlockPage_isAnErrorNotAMiss() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn("<html><title>Your access to the catalog has been limited</title></html>");
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertNotEquals("Not held by Michigan State University", result.getErrorMessage());
        assertTrue(book.getMsuLookupError().toLowerCase().contains("non-json")
                || book.getMsuLookupError().toLowerCase().contains("msu"));
        assertNull(book.getMsuPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_fallsBackToTitleBeforeColon() {
        Book book = bookWithAuthor("Crucial Conversations: Tools for Talking When Stakes Are High", "Kerry Patterson");
        String miss = NONE;
        String hit = """
                {"status":"OK","records":[
                  {"title":"Crucial Conversations : tools for talking when stakes are high / Kerry Patterson.",
                   "shortTitle":"Crucial Conversations","formats":["Book"],
                   "authors":{"secondary":[{"name":"Patterson, Kerry"}]}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(miss, hit);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertEquals("Crucial Conversations", result.getMatchedTitle());
    }

    @Test
    void lookupAndUpdateBook_longTitleMatchesWithoutAuthor() {
        Book book = bookWithAuthor("Crucial Conversations Tools For Talking", "Test Author");
        String body = """
                {"status":"OK","records":[
                  {"title":"Crucial Conversations Tools For Talking","formats":["Cassette"]}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_stripsCopySuffixBeforeSearching() {
        Book book = bookWithAuthor("Test Book, c. 2 (DVD)", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        assertTrue(msuLookupService.lookupAndUpdateBook(1L).isSuccess());

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        verify(msuRestTemplate).getForObject(urlCaptor.capture(), eq(String.class));
        String url = urlCaptor.getValue().toString().toLowerCase();
        assertTrue(url.contains("test%20book") || url.contains("test+book"));
        assertFalse(url.contains("dvd"));
    }

    @Test
    void lookupAndUpdateBook_withAlternateTitle_searchesBoth() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        book.setAlternateTitles(List.of("Other Title"));
        String other = """
                {"status":"OK","records":[
                  {"title":"Other Title","shortTitle":"Other Title","formats":["Vinyl Record"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(NONE, other);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
    }

    private static Book bookWithAuthor(String title, String authorName) {
        Book book = new Book();
        book.setId(1L);
        book.setTitle(title);
        Author author = new Author();
        author.setName(authorName);
        book.setAuthor(author);
        return book;
    }
}
