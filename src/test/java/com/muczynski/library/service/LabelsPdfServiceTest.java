/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LabelsPdfService
 *
 * Tests basic PDF generation functionality including:
 * - PDF generation without errors
 * - PDF byte array is non-empty
 * - Handling of single and multiple books
 * - Handling of books with and without LOC numbers
 */
class LabelsPdfServiceTest {

    private LabelsPdfService labelsPdfService;

    @BeforeEach
    void setUp() {
        labelsPdfService = new LabelsPdfService();
        // Set default font sizes using reflection (since @Value doesn't work in unit tests)
        ReflectionTestUtils.setField(labelsPdfService, "titleFontSize", 11);
        ReflectionTestUtils.setField(labelsPdfService, "authorFontSize", 10);
        ReflectionTestUtils.setField(labelsPdfService, "locFontSize", 10);
    }

    @Test
    void testGenerateLabelsPdf_SingleBook() {
        // Arrange
        Book book = createBook(1L, "The Great Gatsby", "F. Scott Fitzgerald", "PS3511.I9 G7");
        List<Book> books = List.of(book);

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes, "PDF byte array should not be null");
        assertTrue(pdfBytes.length > 0, "PDF byte array should not be empty");
        // PDF files start with "%PDF-" signature
        assertTrue(pdfBytes[0] == 0x25 && pdfBytes[1] == 0x50 && pdfBytes[2] == 0x44 && pdfBytes[3] == 0x46,
                "PDF should start with %PDF signature");
    }

    @Test
    void testGenerateLabelsPdf_MultipleBooks() {
        // Arrange
        List<Book> books = new ArrayList<>();
        books.add(createBook(1L, "Book 1", "Author 1", "PS3511.I9 G7"));
        books.add(createBook(2L, "Book 2", "Author 2", "PS3545.H16"));
        books.add(createBook(3L, "Book 3", "Author 3", "BX4705.M124 A77 2005"));

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // PDF with multiple books should be larger than single book
        assertTrue(pdfBytes.length > 1000, "PDF with multiple books should be reasonably sized");
    }

    @Test
    void testGenerateLabelsPdf_BookWithoutLocNumber() {
        // Arrange
        Book book = createBook(1L, "Book Without LOC", "Test Author", null);
        List<Book> books = List.of(book);

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateLabelsPdf_BookWithoutAuthor() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Book Without Author");
        book.setLocNumber("PS3511.I9 G7");
        // No author set

        List<Book> books = List.of(book);

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateLabelsPdf_ManyBooks_MultiplePages() {
        // Arrange - Create more than 15 books to test multiple pages (15 labels per page)
        List<Book> books = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            books.add(createBook((long) i, "Book " + i, "Author " + i, "PS3511.I9 G" + i));
        }

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // PDF with 20 books should be larger than one with 3 books
        assertTrue(pdfBytes.length > 2000, "PDF with 20 books should be reasonably sized");
    }

    @Test
    void testGenerateLabelsPdf_LongTitleAndAuthor() {
        // Arrange
        Book book = createBook(1L,
                "This is a Very Long Book Title That Should Test Text Wrapping in the Label Cell",
                "Author With A Very Long Name That Should Also Test Text Wrapping",
                "BX4705.M124 A77 2005");
        List<Book> books = List.of(book);

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateLabelsPdf_SpecialCharactersInText() {
        // Arrange
        Book book = createBook(1L,
                "Book with Special Characters: \"Quotes\" & Ampersands",
                "Author's Name with Apostrophe",
                "PS3511.I9 G7");
        List<Book> books = List.of(book);

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerateLabelsPdf_ExactlyOnePageOfLabels() {
        // Arrange - Create exactly 15 books (one full page)
        List<Book> books = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            books.add(createBook((long) i, "Book " + i, "Author " + i, "PS3511.I9 G" + i));
        }

        // Act
        byte[] pdfBytes = labelsPdfService.generateLabelsPdf(books);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    /**
     * Helper method to create a test book
     */
    private Book createBook(Long id, String title, String authorName, String locNumber) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setLocNumber(locNumber);

        if (authorName != null) {
            Author author = new Author();
            author.setName(authorName);
            book.setAuthor(author);
        }

        return book;
    }
}
