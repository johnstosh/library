/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.EmuLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmuLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate emuRestTemplate;

    @InjectMocks
    private EmuLookupService emuLookupService;

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
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertTrue(result.getPaperAvailable());
        assertTrue(result.getEbookAvailable());
        assertTrue(book.getEmuAudioAvailable());
        assertTrue(book.getEmuPaperAvailable());
        assertTrue(book.getEmuEbookAvailable());
        assertNull(book.getEmuLookupError());
        assertNotNull(book.getEmuLastChecked());
    }

    @Test
    void lookupAndUpdateBook_ebookOnly_marksPaperAndAudioFalse() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(RESPONSE_EBOOK_ONLY);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

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
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by EMU Halle Library", result.getErrorMessage());
        assertEquals("Not held by EMU Halle Library", book.getEmuLookupError());
    }

    @Test
    void lookupAndUpdateBook_restTemplateThrows_setsErrorMessage() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("connection timed out"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(book.getEmuLookupError().contains("connection timed out"));
    }

    @Test
    void lookupAndUpdateBook_bookNotFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.muczynski.library.exception.LibraryException.class,
                () -> emuLookupService.lookupAndUpdateBook(99L));
    }

    @Test
    void lookupAndUpdateBook_stripsCopyAndFormatSuffixesBeforeMatching() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book, c. 2 (DVD)");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(RESPONSE_WITH_ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emuRestTemplate).getForObject(urlCaptor.capture(), eq(String.class));
        String url = urlCaptor.getValue();
        assertTrue(url.contains("Test+Book"));
        assertFalse(url.toLowerCase(java.util.Locale.ROOT).contains("dvd"));
    }

    @Test
    void lookupAndUpdateBook_fallsBackToColonTruncatedTitle_whenFullTitleSearchFindsNothing() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Crucial Conversations: Tools for Talking When Stakes Are High");

        String responseShortTitle = """
                {"info":{"totalResultsLocal":1},"docs":[
                  {"context":"L","pnx":{"display":{"type":["book"],"title":["Crucial Conversations"],"format":["300 pages"]}}}
                ]}
                """;

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(RESPONSE_NO_MATCH, responseShortTitle);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

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
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(responseNoAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        // Matched on the first (title + author) attempt despite the entry having no author on
        // record, since the title is long enough (>4 words) to be trusted on its own.
        verify(emuRestTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchAuthorlessEntry_whenTitleIsShort() {
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
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(responseNoAuthor);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        // Still succeeds overall via the title-only fallback attempt, but only after the
        // first (title + author) attempt correctly rejected the authorless short-title entry.
        assertTrue(result.isSuccess());
        verify(emuRestTemplate, times(2)).getForObject(anyString(), eq(String.class));
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
        when(emuRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(responseCentralOnly);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by EMU Halle Library", result.getErrorMessage());
    }
}
