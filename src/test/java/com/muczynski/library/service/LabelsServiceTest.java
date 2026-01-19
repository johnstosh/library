/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookLocStatusDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelsServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private LabelsPdfService labelsPdfService;

    @InjectMocks
    private LabelsService labelsService;

    @Test
    void testGetBooksForLabels_WithBooks() {
        // Arrange
        LocalDateTime mostRecentDateTime = LocalDateTime.of(2025, 12, 28, 10, 0);
        LocalDateTime startOfDay = LocalDateTime.of(2025, 12, 28, 0, 0);
        LocalDateTime endOfDay = LocalDateTime.of(2025, 12, 29, 0, 0);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setLocNumber("PS3545.H16");
        book.setDateAddedToLibrary(mostRecentDateTime);

        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findMaxDateAddedToLibrary()).thenReturn(mostRecentDateTime);
        when(bookRepository.findByDateAddedToLibraryBetweenOrderByDateAddedDesc(startOfDay, endOfDay))
                .thenReturn(List.of(book));
        when(photoRepository.findFirstPhotoIdByBookId(1L)).thenReturn(null);
        when(photoRepository.findFirstPhotoChecksumByBookId(1L)).thenReturn(null);

        // Act
        List<BookLocStatusDto> result = labelsService.getBooksForLabels();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
        assertEquals("Test Author", result.get(0).getAuthorName());
        assertEquals("PS3545.H16", result.get(0).getCurrentLocNumber());
        assertTrue(result.get(0).isHasLocNumber());
    }

    @Test
    void testGetBooksForLabels_NoBooks() {
        // Arrange
        when(bookRepository.findMaxDateAddedToLibrary()).thenReturn(null);

        // Act
        List<BookLocStatusDto> result = labelsService.getBooksForLabels();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllBooksForLabels_WithBooks() {
        // Arrange
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");
        book1.setLocNumber("PS3545.H16");
        book1.setDateAddedToLibrary(LocalDateTime.of(2025, 12, 28, 10, 0));

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");
        book2.setLocNumber("PS3545.A1");
        book2.setDateAddedToLibrary(LocalDateTime.of(2025, 12, 27, 10, 0));

        when(bookRepository.findAll()).thenReturn(List.of(book1, book2));
        when(photoRepository.findFirstPhotoIdByBookId(anyLong())).thenReturn(null);
        when(photoRepository.findFirstPhotoChecksumByBookId(anyLong())).thenReturn(null);

        // Act
        List<BookLocStatusDto> result = labelsService.getAllBooksForLabels();

        // Assert
        assertEquals(2, result.size());
        // Verify books are sorted by date added (newest first)
        assertEquals("Book 1", result.get(0).getTitle());
        assertEquals("Book 2", result.get(1).getTitle());
    }

    @Test
    void testGetAllBooksForLabels_EmptyList() {
        // Arrange
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<BookLocStatusDto> result = labelsService.getAllBooksForLabels();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGenerateLabelsPdf_Success() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setLocNumber("PS3545.H16");

        byte[] expectedPdf = "PDF content".getBytes();

        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(book));
        when(labelsPdfService.generateLabelsPdf(any())).thenReturn(expectedPdf);

        // Act
        byte[] result = labelsService.generateLabelsPdf(List.of(1L));

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);
        verify(labelsPdfService).generateLabelsPdf(any());
    }

    @Test
    void testGenerateLabelsPdf_NullBookIds() {
        // Act & Assert
        LibraryException exception = assertThrows(LibraryException.class, () -> {
            labelsService.generateLabelsPdf(null);
        });

        assertEquals("No books selected for labels", exception.getMessage());
    }

    @Test
    void testGenerateLabelsPdf_EmptyBookIds() {
        // Act & Assert
        LibraryException exception = assertThrows(LibraryException.class, () -> {
            labelsService.generateLabelsPdf(Collections.emptyList());
        });

        assertEquals("No books selected for labels", exception.getMessage());
    }

    @Test
    void testGenerateLabelsPdf_NoBooksFound() {
        // Arrange
        when(bookRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

        // Act & Assert
        LibraryException exception = assertThrows(LibraryException.class, () -> {
            labelsService.generateLabelsPdf(List.of(999L));
        });

        assertEquals("No books found for the given IDs", exception.getMessage());
    }

    @Test
    void testGenerateLabelsPdf_MultipleBooks() {
        // Arrange
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");
        book1.setLocNumber("PS3545.H16");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");
        book2.setLocNumber("PS3545.A1");

        byte[] expectedPdf = "PDF with multiple books".getBytes();

        when(bookRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(book1, book2));
        when(labelsPdfService.generateLabelsPdf(any())).thenReturn(expectedPdf);

        // Act
        byte[] result = labelsService.generateLabelsPdf(List.of(1L, 2L));

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);
        verify(labelsPdfService).generateLabelsPdf(any());
    }
}
