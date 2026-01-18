// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.dto.CheckoutCardTranscriptionDto;
import com.muczynski.library.exception.LibraryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CheckoutCardTranscriptionServiceTest {

    private AskGrok askGrok;
    private CheckoutCardTranscriptionService service;

    @BeforeEach
    void setUp() {
        askGrok = mock(AskGrok.class);
        service = new CheckoutCardTranscriptionService(askGrok);
    }

    @Test
    void testTranscribeCheckoutCard_success() throws Exception {
        // Arrange
        byte[] imageBytes = "test-image".getBytes();
        String contentType = "image/jpeg";
        String grokResponse = """
            {
              "title": "The Pushcart War",
              "author": "Jean Merrill",
              "call_number": "PZ 7 .M5453 5",
              "last_date": "1-17-26",
              "last_issued_to": "John",
              "last_due": "1-31-26"
            }
            """;

        when(askGrok.analyzePhoto(eq(imageBytes), eq(contentType), anyString()))
                .thenReturn(grokResponse);

        // Act
        CheckoutCardTranscriptionDto result = service.transcribeCheckoutCard(imageBytes, contentType);

        // Assert
        assertNotNull(result);
        assertEquals("The Pushcart War", result.getTitle());
        assertEquals("Jean Merrill", result.getAuthor());
        assertEquals("PZ 7 .M5453 5", result.getCallNumber());
        assertEquals("1-17-26", result.getLastDate());
        assertEquals("John", result.getLastIssuedTo());
        assertEquals("1-31-26", result.getLastDue());

        verify(askGrok).analyzePhoto(eq(imageBytes), eq(contentType), anyString());
    }

    @Test
    void testTranscribeCheckoutCard_withExtraText() throws Exception {
        // Arrange - Grok might include extra explanation text
        byte[] imageBytes = "test-image".getBytes();
        String contentType = "image/jpeg";
        String grokResponse = """
            Here is the extracted data:
            {
              "title": "Test Book",
              "author": "Test Author",
              "call_number": "AB 123",
              "last_date": "N/A",
              "last_issued_to": "N/A",
              "last_due": "N/A"
            }
            This is the information from the card.
            """;

        when(askGrok.analyzePhoto(eq(imageBytes), eq(contentType), anyString()))
                .thenReturn(grokResponse);

        // Act
        CheckoutCardTranscriptionDto result = service.transcribeCheckoutCard(imageBytes, contentType);

        // Assert
        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        assertEquals("Test Author", result.getAuthor());
        assertEquals("AB 123", result.getCallNumber());

        verify(askGrok).analyzePhoto(eq(imageBytes), eq(contentType), anyString());
    }

    @Test
    void testTranscribeCheckoutCard_invalidJson() {
        // Arrange
        byte[] imageBytes = "test-image".getBytes();
        String contentType = "image/jpeg";
        String grokResponse = "This is not valid JSON";

        when(askGrok.analyzePhoto(eq(imageBytes), eq(contentType), anyString()))
                .thenReturn(grokResponse);

        // Act & Assert
        assertThrows(LibraryException.class, () ->
                service.transcribeCheckoutCard(imageBytes, contentType)
        );

        verify(askGrok).analyzePhoto(eq(imageBytes), eq(contentType), anyString());
    }

    @Test
    void testTranscribeCheckoutCard_grokThrowsException() {
        // Arrange
        byte[] imageBytes = "test-image".getBytes();
        String contentType = "image/jpeg";

        when(askGrok.analyzePhoto(eq(imageBytes), eq(contentType), anyString()))
                .thenThrow(new LibraryException("xAI API key not configured"));

        // Act & Assert
        assertThrows(LibraryException.class, () ->
                service.transcribeCheckoutCard(imageBytes, contentType)
        );

        verify(askGrok).analyzePhoto(eq(imageBytes), eq(contentType), anyString());
    }

    @Test
    void testTranscribeCheckoutCard_emptyResponse() {
        // Arrange
        byte[] imageBytes = "test-image".getBytes();
        String contentType = "image/jpeg";
        String grokResponse = "";

        when(askGrok.analyzePhoto(eq(imageBytes), eq(contentType), anyString()))
                .thenReturn(grokResponse);

        // Act & Assert
        assertThrows(LibraryException.class, () ->
                service.transcribeCheckoutCard(imageBytes, contentType)
        );
    }

    @Test
    void testTranscribeCheckoutCard_withNAValues() throws Exception {
        // Arrange
        byte[] imageBytes = "test-image".getBytes();
        String contentType = "image/jpeg";
        String grokResponse = """
            {
              "title": "Book Without Checkouts",
              "author": "New Author",
              "call_number": "XY 999",
              "last_date": "N/A",
              "last_issued_to": "N/A",
              "last_due": "N/A"
            }
            """;

        when(askGrok.analyzePhoto(eq(imageBytes), eq(contentType), anyString()))
                .thenReturn(grokResponse);

        // Act
        CheckoutCardTranscriptionDto result = service.transcribeCheckoutCard(imageBytes, contentType);

        // Assert
        assertNotNull(result);
        assertEquals("Book Without Checkouts", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("XY 999", result.getCallNumber());
        assertEquals("N/A", result.getLastDate());
        assertEquals("N/A", result.getLastIssuedTo());
        assertEquals("N/A", result.getLastDue());
    }
}
