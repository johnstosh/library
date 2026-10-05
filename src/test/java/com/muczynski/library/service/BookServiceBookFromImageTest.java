/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.exception.BookHasNoPhotosException;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Book from Image / Book from First Photo must never overwrite an existing book's
 * title or author when there is nothing to read or the AI call fails.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceBookFromImageTest {

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

    private Book existingBook() {
        Book book = new Book();
        book.setId(2115L);
        book.setTitle("The Real Title");
        BookDto dto = new BookDto();
        dto.setId(2115L);
        dto.setTitle("The Real Title");
        dto.setAuthorId(42L);
        when(bookRepository.findById(2115L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);
        return book;
    }

    private Photo photo() {
        Photo photo = new Photo();
        photo.setImage("img".getBytes());
        photo.setContentType("image/jpeg");
        return photo;
    }

    @Test
    void generateTempBook_noPhotos_throwsAndLeavesBookUnchanged() {
        Book book = existingBook();
        when(photoRepository.findByBookIdOrderByPhotoOrder(2115L)).thenReturn(Collections.emptyList());

        BookHasNoPhotosException ex = assertThrows(BookHasNoPhotosException.class,
                () -> bookService.generateTempBook(2115L));

        assertEquals("This book has no photos, so Book from Image has nothing to read.", ex.getMessage());
        assertEquals("The Real Title", book.getTitle());
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(authorRepository);
        verifyNoInteractions(askGrok);
    }

    @Test
    void generateBookFromFirstPhoto_noPhotos_throwsAndLeavesBookUnchanged() {
        Book book = existingBook();
        when(photoRepository.findByBookIdOrderByPhotoOrder(2115L)).thenReturn(Collections.emptyList());

        BookHasNoPhotosException ex = assertThrows(BookHasNoPhotosException.class,
                () -> bookService.generateBookFromFirstPhoto(2115L));

        assertEquals("This book has no photos, so Book from First Photo has nothing to read.", ex.getMessage());
        assertEquals("The Real Title", book.getTitle());
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(authorRepository);
        verifyNoInteractions(askGrok);
    }

    @Test
    void generateTempBook_grokOutOfCredits_surfacesErrorAndLeavesTitleAndAuthorUnchanged() {
        Book book = existingBook();
        when(photoRepository.findByBookIdOrderByPhotoOrder(2115L)).thenReturn(List.of(photo()));
        when(askGrok.analyzePhotos(anyList(), anyString(), anyString(), anyString()))
                .thenThrow(new GrokCreditsExhaustedException());

        assertThrows(GrokCreditsExhaustedException.class, () -> bookService.generateTempBook(2115L));

        assertEquals("The Real Title", book.getTitle());
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(authorRepository);
    }

    @Test
    void generateBookFromFirstPhoto_grokOutOfCredits_surfacesErrorAndLeavesTitleAndAuthorUnchanged() {
        Book book = existingBook();
        when(photoRepository.findByBookIdOrderByPhotoOrder(2115L)).thenReturn(List.of(photo()));
        when(askGrok.analyzePhotos(anyList(), anyString(), anyString(), anyString()))
                .thenThrow(new GrokCreditsExhaustedException());

        assertThrows(GrokCreditsExhaustedException.class, () -> bookService.generateBookFromFirstPhoto(2115L));

        assertEquals("The Real Title", book.getTitle());
        verify(bookRepository, never()).save(any());
        verifyNoInteractions(authorRepository);
    }
}
