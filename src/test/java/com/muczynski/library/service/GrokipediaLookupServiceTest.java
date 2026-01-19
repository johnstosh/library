/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.GrokipediaLookupResultDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrokipediaLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GrokipediaLookupService grokipediaLookupService;

    @Test
    void generateGrokipediaUrl_replacesSpacesWithUnderscores() {
        String url = grokipediaLookupService.generateGrokipediaUrl("Little Women");
        assertEquals("https://grokipedia.com/page/Little_Women", url);
    }

    @Test
    void generateGrokipediaUrl_handlesAuthorName() {
        String url = grokipediaLookupService.generateGrokipediaUrl("Louisa May Alcott");
        assertEquals("https://grokipedia.com/page/Louisa_May_Alcott", url);
    }

    @Test
    void generateGrokipediaUrl_trimsWhitespace() {
        String url = grokipediaLookupService.generateGrokipediaUrl("  The Hobbit  ");
        assertEquals("https://grokipedia.com/page/The_Hobbit", url);
    }

    @Test
    void checkUrlExists_returnsTrueFor200() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.HEAD), isNull(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean exists = grokipediaLookupService.checkUrlExists("https://grokipedia.com/page/Test");
        assertTrue(exists);
    }

    @Test
    void checkUrlExists_returnsFalseFor404() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.HEAD), isNull(), eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        boolean exists = grokipediaLookupService.checkUrlExists("https://grokipedia.com/page/NonExistent");
        assertFalse(exists);
    }

    @Test
    void lookupBook_successfulLookup() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Little Women");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(restTemplate.exchange(
                eq("https://grokipedia.com/page/Little_Women"),
                eq(HttpMethod.HEAD),
                isNull(),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        GrokipediaLookupResultDto result = grokipediaLookupService.lookupBook(1L);

        assertTrue(result.isSuccess());
        assertEquals("https://grokipedia.com/page/Little_Women", result.getGrokipediaUrl());
        assertEquals("Little Women", result.getName());
        verify(bookRepository).save(argThat(b ->
            "https://grokipedia.com/page/Little_Women".equals(b.getGrokipediaUrl())));
    }

    @Test
    void lookupBook_urlNotFound() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Some Obscure Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.HEAD), isNull(), eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        GrokipediaLookupResultDto result = grokipediaLookupService.lookupBook(1L);

        assertFalse(result.isSuccess());
        assertNull(result.getGrokipediaUrl());
        assertTrue(result.getErrorMessage().contains("No Grokipedia page found"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void lookupBook_emptyTitle() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        GrokipediaLookupResultDto result = grokipediaLookupService.lookupBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Book has no title", result.getErrorMessage());
    }

    @Test
    void lookupAuthor_successfulLookup() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Louisa May Alcott");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(restTemplate.exchange(
                eq("https://grokipedia.com/page/Louisa_May_Alcott"),
                eq(HttpMethod.HEAD),
                isNull(),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        GrokipediaLookupResultDto result = grokipediaLookupService.lookupAuthor(1L);

        assertTrue(result.isSuccess());
        assertEquals("https://grokipedia.com/page/Louisa_May_Alcott", result.getGrokipediaUrl());
        assertEquals("Louisa May Alcott", result.getName());
        verify(authorRepository).save(argThat(a ->
            "https://grokipedia.com/page/Louisa_May_Alcott".equals(a.getGrokipediaUrl())));
    }

    @Test
    void lookupAuthor_urlNotFound() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Unknown Author");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.HEAD), isNull(), eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        GrokipediaLookupResultDto result = grokipediaLookupService.lookupAuthor(1L);

        assertFalse(result.isSuccess());
        assertNull(result.getGrokipediaUrl());
        assertTrue(result.getErrorMessage().contains("No Grokipedia page found"));
        verify(authorRepository, never()).save(any());
    }

    @Test
    void lookupBooks_multipleBooks() {
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Little Women");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Unknown Book");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));

        // First book succeeds
        when(restTemplate.exchange(
                eq("https://grokipedia.com/page/Little_Women"),
                eq(HttpMethod.HEAD),
                isNull(),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Second book fails
        when(restTemplate.exchange(
                eq("https://grokipedia.com/page/Unknown_Book"),
                eq(HttpMethod.HEAD),
                isNull(),
                eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        when(bookRepository.save(any(Book.class))).thenReturn(book1);

        List<GrokipediaLookupResultDto> results = grokipediaLookupService.lookupBooks(Arrays.asList(1L, 2L));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertFalse(results.get(1).isSuccess());
    }

    @Test
    void lookupAuthors_multipleAuthors() {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setName("Mark Twain");

        Author author2 = new Author();
        author2.setId(2L);
        author2.setName("Unknown Author");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author1));
        when(authorRepository.findById(2L)).thenReturn(Optional.of(author2));

        // First author succeeds
        when(restTemplate.exchange(
                eq("https://grokipedia.com/page/Mark_Twain"),
                eq(HttpMethod.HEAD),
                isNull(),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Second author fails
        when(restTemplate.exchange(
                eq("https://grokipedia.com/page/Unknown_Author"),
                eq(HttpMethod.HEAD),
                isNull(),
                eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        when(authorRepository.save(any(Author.class))).thenReturn(author1);

        List<GrokipediaLookupResultDto> results = grokipediaLookupService.lookupAuthors(Arrays.asList(1L, 2L));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertFalse(results.get(1).isSuccess());
    }
}
