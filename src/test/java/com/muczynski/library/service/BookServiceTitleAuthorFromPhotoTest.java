/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTitleAuthorFromPhotoTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private AskGrok askGrok;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BookService bookService;

    @Test
    void getTitleAuthorFromPhoto_returnsPreviewWithoutPersistingBook() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Original Title");

        BookDto dto = new BookDto();
        dto.setId(1L);
        dto.setTitle("Original Title");
        dto.setAuthorId(10L);

        Photo photo = new Photo();
        photo.setImage("img".getBytes());
        photo.setContentType("image/jpeg");

        Author existingAuthor = new Author();
        existingAuthor.setId(20L);
        existingAuthor.setName("Jane Austen");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);
        when(photoRepository.findByBookIdOrderByPhotoOrder(1L)).thenReturn(List.of(photo));
        when(askGrok.analyzePhoto(any(), any(), anyString(), anyString()))
                .thenReturn("{\"title\": \"Pride and Prejudice\", \"authorName\": \"Jane Austen\"}");
        when(authorRepository.findAllByNameOrderByIdAsc("Jane Austen"))
                .thenReturn(List.of(existingAuthor));

        BookDto result = bookService.getTitleAuthorFromPhoto(1L);

        assertEquals("Pride and Prejudice", result.getTitle());
        assertEquals(20L, result.getAuthorId());
        assertEquals("Jane Austen", result.getAuthor());
        verify(bookRepository, never()).save(any());
        verify(authorRepository, never()).save(any());
    }

    @Test
    void getTitleAuthorFromPhoto_createsAuthorButDoesNotPersistBook() {
        Book book = new Book();
        book.setId(1L);

        BookDto dto = new BookDto();
        dto.setId(1L);
        dto.setTitle("Original Title");

        Photo photo = new Photo();
        photo.setImage("img".getBytes());
        photo.setContentType("image/jpeg");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);
        when(photoRepository.findByBookIdOrderByPhotoOrder(1L)).thenReturn(List.of(photo));
        when(askGrok.analyzePhoto(any(), any(), anyString(), anyString()))
                .thenReturn("{\"title\": \"New Book\", \"authorName\": \"New Author\"}");
        when(authorRepository.findAllByNameOrderByIdAsc("New Author"))
                .thenReturn(Collections.emptyList());
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> {
            Author author = invocation.getArgument(0);
            author.setId(99L);
            return author;
        });

        BookDto result = bookService.getTitleAuthorFromPhoto(1L);

        assertEquals("New Book", result.getTitle());
        assertEquals(99L, result.getAuthorId());
        assertEquals("New Author", result.getAuthor());
        verify(authorRepository).save(any(Author.class));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void getTitleAuthorFromPhoto_throwsWhenBookHasNoPhotos() {
        Book book = new Book();
        book.setId(1L);
        BookDto dto = new BookDto();
        dto.setId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);
        when(photoRepository.findByBookIdOrderByPhotoOrder(1L)).thenReturn(Collections.emptyList());

        assertThrows(LibraryException.class, () -> bookService.getTitleAuthorFromPhoto(1L));
        verify(bookRepository, never()).save(any());
    }
}
