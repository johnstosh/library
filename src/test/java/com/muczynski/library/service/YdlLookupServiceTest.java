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

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(ydlRestTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(RESPONSE_NO_MATCH);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by YDL", result.getErrorMessage());
        assertEquals("Not held by YDL", book.getYdlLookupError());
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
        assertEquals("\"Test Book\"", searchText);
    }

    @Test
    void lookupAndUpdateBook_fallsBackToColonTruncatedTitle_whenFullTitleSearchFindsNothing() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Crucial Conversations: Tools for Talking When Stakes Are High");

        String responseShortTitle = """
                {"totalPages":1,"page":0,"totalResults":1,"data":[
                  {"title":"Crucial Conversations",
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
}
