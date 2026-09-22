/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AclaLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclaLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate aclaRestTemplate;

    private AclaLookupService aclaLookupService;

    @BeforeEach
    void setUp() {
        aclaLookupService = new AclaLookupService(bookRepository, aclaRestTemplate, 0, 0, 0);
    }

    private static final String RESPONSE_WITH_ALL_FORMATS = """
            {"entities":{"bibs":{
              "S1":{"briefInfo":{"title":"Test Book","authors":["Author, Test"],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}},
              "S2":{"briefInfo":{"title":"Test Book","authors":["Author, Test"],"format":"EBOOK","superFormats":["BOOKS","ELECTRONIC_FORMATS"],"consumptionFormat":"READ"}},
              "S3":{"briefInfo":{"title":"Test Book","authors":["Author, Test"],"format":"EAUDIOBOOK","superFormats":["AUDIOBOOKS_SPOKEN_WORD"],"consumptionFormat":"LISTEN"}}
            }}}
            """;

    private static final String RESPONSE_EBOOK_ONLY = """
            {"entities":{"bibs":{
              "S1":{"briefInfo":{"title":"Test Book","authors":["Author, Test"],"format":"EBOOK","superFormats":["BOOKS","ELECTRONIC_FORMATS"],"consumptionFormat":"READ"}}
            }}}
            """;

    private static final String RESPONSE_PAPER_ONLY = """
            {"entities":{"bibs":{
              "S1":{"briefInfo":{"title":"Test Book","authors":["Author, Test"],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}}
            }}}
            """;

    private static final String RESPONSE_NO_MATCH = """
            {"entities":{"bibs":{}}}
            """;

    @Test
    void lookupAndUpdateBook_marksAllThreeFormatsAvailable() {
        Book book = bookWithAuthor("Test Book", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getAudioAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getPaperAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getEbookAvailable()));
        assertTrue(book.getAclaAudioAvailable());
        assertTrue(book.getAclaPaperAvailable());
        assertTrue(book.getAclaEbookAvailable());
        assertNull(book.getAclaLookupError());
        assertNotNull(book.getAclaLastChecked());
        verify(aclaRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    void lookupAndUpdateBook_ebookOnly_marksPaperAndAudioFalse() {
        Book book = bookWithAuthor("Test Book", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_EBOOK_ONLY);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());
        assertFalse(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_followUpFormatSearchFillsMissingEbook() {
        Book book = bookWithAuthor("Test Book", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(
                        RESPONSE_PAPER_ONLY,
                        RESPONSE_NO_MATCH,
                        RESPONSE_NO_MATCH,
                        RESPONSE_NO_MATCH,
                        RESPONSE_EBOOK_ONLY);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertTrue(result.getEbookAvailable());
        assertFalse(result.getAudioAvailable());
    }

    @Test
    void lookupAndUpdateBook_noMatch_setsLookupError() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Nonexistent Book Title");
        book.setAclaAudioAvailable(true);
        book.setAclaPaperAvailable(true);
        book.setAclaEbookAvailable(true);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by ACLA", result.getErrorMessage());
        assertEquals("Not held by ACLA", book.getAclaLookupError());
        assertFalse(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
        assertFalse(result.getEbookAvailable());
        assertFalse(book.getAclaAudioAvailable());
        assertFalse(book.getAclaPaperAvailable());
        assertFalse(book.getAclaEbookAvailable());
    }

    @Test
    void lookupAndUpdateBook_restTemplateThrows_setsErrorMessage() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new RuntimeException("connection timed out"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(book.getAclaLookupError().contains("connection timed out"));
    }

    @Test
    void lookupAndUpdateBook_httpErrorWithHugeBody_storesShortStatusError() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        HttpClientErrorException httpError = forbiddenWithHugeBody();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class))).thenThrow(httpError);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Error: HTTP 403 Forbidden", result.getErrorMessage());
        assertEquals("Error: HTTP 403 Forbidden", book.getAclaLookupError());
        assertTrue(httpError.getMessage().length() > 255);
    }

    @Test
    void lookupAndUpdateBook_retriesForbiddenThenSucceeds() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        AclaLookupService retryingService = new AclaLookupService(
                bookRepository, aclaRestTemplate, 2, 0, 0);

        HttpClientErrorException httpError = forbiddenWithHugeBody();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(httpError)
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = retryingService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertNull(book.getAclaLookupError());
        verify(aclaRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    void lookupAndUpdateBook_followUpForbidden_keepsFormatsFromFirstSearch() {
        Book book = bookWithAuthor("Test Book", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_PAPER_ONLY)
                .thenThrow(forbiddenWithHugeBody());
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertFalse(result.getAudioAvailable());
        assertFalse(result.getEbookAvailable());
        assertNull(book.getAclaLookupError());
    }

    @Test
    void isRetryableStatus_coversCdnThrottleAndGatewayErrors() {
        assertTrue(AclaLookupService.isRetryableStatus(403));
        assertTrue(AclaLookupService.isRetryableStatus(429));
        assertTrue(AclaLookupService.isRetryableStatus(503));
        assertFalse(AclaLookupService.isRetryableStatus(404));
        assertFalse(AclaLookupService.isRetryableStatus(400));
    }

    private static HttpClientErrorException forbiddenWithHugeBody() {
        String restTemplateMessage = "403 Forbidden from https://gateway.bibliocommons.com/v2/libraries/acl/bibs/search: ["
                + "z".repeat(400) + "]";
        byte[] body = ("<!DOCTYPE html>" + "z".repeat(5000)).getBytes(StandardCharsets.UTF_8);
        return HttpClientErrorException.create(
                restTemplateMessage, HttpStatus.FORBIDDEN, "Forbidden",
                HttpHeaders.EMPTY, body, StandardCharsets.UTF_8);
    }

    @Test
    void lookupAndUpdateBook_longExceptionMessage_truncatesLookupErrorTo255() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new RuntimeException("x".repeat(1000)));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals(255, result.getErrorMessage().length());
        assertEquals(255, book.getAclaLookupError().length());
        assertTrue(book.getAclaLookupError().startsWith("Error: "));
        assertTrue(book.getAclaLookupError().endsWith("..."));
    }

    @Test
    void lookupAndUpdateBook_bookNotFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.muczynski.library.exception.LibraryException.class,
                () -> aclaLookupService.lookupAndUpdateBook(99L));
    }

    @Test
    void lookupAndUpdateBook_stripsCopyAndFormatSuffixesBeforeMatching() {
        Book book = bookWithAuthor("Test Book, c. 2 (DVD)", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());

        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(aclaRestTemplate).getForObject(captor.capture(), eq(String.class));
        String uri = captor.getValue().toString();
        assertTrue(uri.contains("Test+Book") || uri.contains("Test%20Book"));
        assertFalse(uri.contains("c.+2") || uri.contains("c.%202") || uri.contains("DVD"));
        assertTrue(uri.contains("searchType=keyword"));
    }

    @Test
    void lookupAndUpdateBook_fallsBackToColonTruncatedTitle_whenFullTitleSearchFindsNothing() {
        Book book = bookWithAuthor("Crucial Conversations: Tools for Talking When Stakes Are High", "Kerry Patterson");

        String responseShortTitle = """
                {"entities":{"bibs":{
                  "S1":{"briefInfo":{"title":"Crucial Conversations","authors":["Patterson, Kerry"],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}}
                }}}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH, responseShortTitle);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertEquals("Crucial Conversations", result.getMatchedTitle());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchUnrelatedTitleStartingWithSameWord_noAuthor() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Ellen");

        String responseUnrelated = """
                {"entities":{"bibs":{
                  "S1":{"briefInfo":{"title":"Ellen G White Story","authors":["White, Ellen"],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}}
                }}}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseUnrelated);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by ACLA", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchWhenTitlesDifferInLength_noFuzzyMatching() {
        Book book = bookWithAuthor("Crucial Conversations", "Kerry Patterson");

        String responseLongerTitle = """
                {"entities":{"bibs":{
                  "S1":{"briefInfo":{"title":"Crucial Conversations Tools For Talking When Stakes Are High","authors":["Patterson, Kerry"],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}}
                }}}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseLongerTitle);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchSubstringAuthorName() {
        Book book = bookWithAuthor("Short Title", "Ellen White");

        String responseDifferentAuthor = """
                {"entities":{"bibs":{
                  "S1":{"briefInfo":{"title":"Short Title","authors":["Whitehead, John"],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}}
                }}}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseDifferentAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_matchesExactAuthorWordAmongMultipleWords() {
        Book book = bookWithAuthor("Short Title", "Ellen White");

        String responseMatchingAuthor = """
                {"entities":{"bibs":{
                  "S1":{"briefInfo":{"title":"Short Title","authors":["White, Ellen G."],"format":"BK","superFormats":["BOOKS"],"consumptionFormat":"READ"}}
                }}}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(responseMatchingAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
    }

    private Book bookWithAuthor(String title, String authorName) {
        Book book = new Book();
        book.setId(1L);
        book.setTitle(title);
        Author author = new Author();
        author.setName(authorName);
        book.setAuthor(author);
        return book;
    }

    @Test
    void lookupAndUpdateBook_noAlternateTitle_usesOnlyPrimaryUnchanged() {
        Book book = bookWithAuthor("Test Book", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getAudioAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getPaperAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getEbookAvailable()));
    }

    @Test
    void lookupAndUpdateBook_withAlternateTitle_searchesBothAndMergesFlags() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        book.setAlternateTitle("Test Book Alt");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        // primary no match, alternate succeeds with all formats
        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH)
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getAudioAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getPaperAvailable()));
        assertTrue(Boolean.TRUE.equals(result.getEbookAvailable()));
        verify(aclaRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
    }
}
