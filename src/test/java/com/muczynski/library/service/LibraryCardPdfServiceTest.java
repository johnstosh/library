/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.LibraryCardDesign;
import com.muczynski.library.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LibraryCardPdfService
 * Tests PDF generation logic for library cards
 */
@SpringBootTest
@ActiveProfiles("test")
class LibraryCardPdfServiceTest {

    @Autowired
    private LibraryCardPdfService libraryCardPdfService;

    @Test
    void testGenerateLibraryCardPdf_WithDefaultDesign() throws IOException {
        // Arrange
        User user = createTestUser("Test User", LibraryCardDesign.CLASSICAL_DEVOTION);

        // Act
        byte[] pdfBytes = libraryCardPdfService.generateLibraryCardPdf(user);

        // Assert
        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF should have content");

        // Verify PDF signature
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader, "PDF should start with %PDF header");
    }

    @Test
    void testGenerateLibraryCardPdf_WithAllDesigns() throws IOException {
        // Test all designs can generate PDFs
        for (LibraryCardDesign design : LibraryCardDesign.values()) {
            // Arrange
            User user = createTestUser("User " + design.name(), design);

            // Act
            byte[] pdfBytes = libraryCardPdfService.generateLibraryCardPdf(user);

            // Assert
            assertNotNull(pdfBytes, "PDF for design " + design + " should not be null");
            assertTrue(pdfBytes.length > 0, "PDF for design " + design + " should have content");

            // Verify it's a valid PDF
            String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
            assertEquals("%PDF", pdfHeader, "PDF for design " + design + " should be valid");
        }
    }

    @Test
    void testGenerateLibraryCardPdf_WithNullDesign_UsesDefault() throws IOException {
        // Arrange - null design should fall back to default
        User user = createTestUser("Test User", null);

        // Act
        byte[] pdfBytes = libraryCardPdfService.generateLibraryCardPdf(user);

        // Assert
        assertNotNull(pdfBytes, "PDF should be generated even with null design");
        assertTrue(pdfBytes.length > 0, "PDF should have content");

        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader, "PDF should be valid");
    }

    @Test
    void testGenerateLibraryCardPdf_DifferentDesignsProduceDifferentPDFs() throws IOException {
        // Arrange
        User user1 = createTestUser("User1", LibraryCardDesign.COUNTRYSIDE_YOUTH);
        User user2 = createTestUser("User2", LibraryCardDesign.SACRED_HEART_PORTRAIT);

        // Act
        byte[] pdf1 = libraryCardPdfService.generateLibraryCardPdf(user1);
        byte[] pdf2 = libraryCardPdfService.generateLibraryCardPdf(user2);

        // Assert - Different designs should produce different PDFs
        assertNotNull(pdf1);
        assertNotNull(pdf2);
        assertFalse(java.util.Arrays.equals(pdf1, pdf2),
                "Different designs should produce different PDFs");
    }

    @Test
    void testGenerateLibraryCardPdf_PDFSizeIsReasonable() throws IOException {
        // Arrange
        User user = createTestUser("Size Test User", LibraryCardDesign.CLASSICAL_DEVOTION);

        // Act
        byte[] pdfBytes = libraryCardPdfService.generateLibraryCardPdf(user);

        // Assert - PDF should be reasonably sized
        assertTrue(pdfBytes.length > 10000, "PDF should be larger than 10KB (has image content)");
        assertTrue(pdfBytes.length < 5000000, "PDF should be smaller than 5MB (wallet-sized card)");
    }

    @Test
    void testGenerateLibraryCardPdf_ConsistentSize() throws IOException {
        // Test that generating the same card multiple times produces similar size PDFs
        // Note: PDFs may not be byte-identical due to timestamps, but size should be similar
        // Arrange
        User user = createTestUser("Consistency Test", LibraryCardDesign.RADIANT_BLESSING);

        // Act - Generate twice
        byte[] pdf1 = libraryCardPdfService.generateLibraryCardPdf(user);
        byte[] pdf2 = libraryCardPdfService.generateLibraryCardPdf(user);

        // Assert - Same user and design should produce similar-sized PDFs (within 10%)
        int sizeDifference = Math.abs(pdf1.length - pdf2.length);
        int allowedVariance = (int) (pdf1.length * 0.1); // 10% variance allowed
        assertTrue(sizeDifference <= allowedVariance,
                "PDF sizes should be similar (difference: " + sizeDifference + " bytes)");
    }

    @Test
    void testLibraryCardDesign_GetDisplayName() {
        // Test that all designs have proper display names
        assertEquals("Countryside Youth", LibraryCardDesign.COUNTRYSIDE_YOUTH.getDisplayName());
        assertEquals("Sacred Heart Portrait", LibraryCardDesign.SACRED_HEART_PORTRAIT.getDisplayName());
        assertEquals("Radiant Blessing", LibraryCardDesign.RADIANT_BLESSING.getDisplayName());
        assertEquals("Patron of Creatures", LibraryCardDesign.PATRON_OF_CREATURES.getDisplayName());
        assertEquals("Classical Devotion", LibraryCardDesign.CLASSICAL_DEVOTION.getDisplayName());
    }

    @Test
    void testLibraryCardDesign_GetImageFilename() {
        // Test that all designs have proper filenames
        assertEquals("countryside_youth.jpg", LibraryCardDesign.COUNTRYSIDE_YOUTH.getImageFilename());
        assertEquals("sacred_heart_portrait.jpg", LibraryCardDesign.SACRED_HEART_PORTRAIT.getImageFilename());
        assertEquals("radiant_blessing.jpg", LibraryCardDesign.RADIANT_BLESSING.getImageFilename());
        assertEquals("patron_of_creatures.jpg", LibraryCardDesign.PATRON_OF_CREATURES.getImageFilename());
        assertEquals("classical_devotion.jpg", LibraryCardDesign.CLASSICAL_DEVOTION.getImageFilename());
    }

    @Test
    void testLibraryCardDesign_GetDefault() {
        // Test default design
        assertEquals(LibraryCardDesign.CLASSICAL_DEVOTION, LibraryCardDesign.getDefault(),
                "Default design should be CLASSICAL_DEVOTION");
    }

    // ==================== Helper Methods ====================

    private User createTestUser(String username, LibraryCardDesign design) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setLibraryCardDesign(design);
        return user;
    }
}
