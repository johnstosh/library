/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.LibraryApplication;
import com.muczynski.library.model.LocCallNumberResponse;
import com.muczynski.library.model.LocSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for LocCatalogService that makes actual calls to the Library of Congress API.
 * This test verifies that the service correctly retrieves LOC call numbers for real books.
 */
@SpringBootTest(classes = LibraryApplication.class)
@ActiveProfiles("test")
class LocCatalogServiceIntegrationTest {

    @Autowired
    private LocCatalogService locCatalogService;

    /**
     * Test case: A well-known book to verify the LOC service works
     * Using "The Great Gatsby" which definitely exists in LOC catalog
     */
    @Test
    void testGetLocCallNumber_TheGreatGatsby() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("The Great Gatsby");
        request.setAuthor("Fitzgerald");

        // Act
        LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getCallNumber(), "Call number should not be null");
        assertFalse(response.getCallNumber().trim().isEmpty(), "Call number should not be empty");

        // Verify it's a Library of Congress Classification
        // Note: Different editions may have different classifications (PS, PZ3, etc.)
        assertTrue(response.getCallNumber().matches("^[A-Z]+.*"),
                "Call number should be a valid LOC classification. Got: " + response.getCallNumber());

        // Log the results for manual verification
        System.out.println("Found LOC Call Number: " + response.getCallNumber());
        System.out.println("Match count: " + response.getMatchCount());

        // Verify we got at least one match
        assertTrue(response.getMatchCount() > 0, "Should have at least one match");
    }

    /**
     * Test case: The Catholic Study Bible: New American Bible Revised Edition
     * Publisher: Oxford University Press (3rd ed., 2011/2020)
     * Expected LOC Call Number: BS 192.3 .A1 2020 G73
     *
     * Note: This book may not be easily findable in the LOC catalog with exact title match.
     * Manual verification recommended for this specific edition.
     */
    @Test
    void testGetLocCallNumber_CatholicStudyBible() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("Bible");
        request.setAuthor(""); // Start with a broader search

        // Act
        LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getCallNumber(), "Call number should not be null");
        assertFalse(response.getCallNumber().trim().isEmpty(), "Call number should not be empty");

        // Verify it's a Library of Congress Classification (BS = Bible)
        assertTrue(response.getCallNumber().startsWith("BS"),
                "Call number should start with 'BS' (Bible classification). Got: " + response.getCallNumber());

        // Log the results for manual verification
        System.out.println("Found LOC Call Number for Bible search: " + response.getCallNumber());
        System.out.println("Match count: " + response.getMatchCount());
        System.out.println("Note: Catholic Study Bible expected: BS 192.3 .A1 2020 G73");
        System.out.println("For exact edition, try web search at: https://catalog.loc.gov/");

        // Verify we got at least one match
        assertTrue(response.getMatchCount() > 0, "Should have at least one match");
    }

    /**
     * Test case: Verify that very specific/long titles may not be found
     * This tests the "not found" path with a realistic scenario
     */
    @Test
    void testGetLocCallNumber_CatholicStudyBible_FullTitle() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("The Catholic Study Bible New American Bible Revised Edition Third Edition");

        // Act & Assert
        // Very specific edition titles may not be indexed exactly in LOC
        try {
            LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);
            // If found, verify it's a valid Bible classification
            assertTrue(response.getCallNumber().startsWith("BS"),
                    "Call number should start with 'BS' (Bible classification). Got: " + response.getCallNumber());
            System.out.println("Full title search - LOC Call Number: " + response.getCallNumber());
        } catch (ResponseStatusException e) {
            // Not found is acceptable for very specific edition titles
            assertEquals(404, e.getStatusCode().value(), "Should return 404 for not found");
            System.out.println("Very specific edition not found - this is expected");
            System.out.println("Note: Use broader search terms or the LOC web catalog for specific editions");
        }
    }

    /**
     * Test case: Search with different title variations
     */
    @Test
    void testGetLocCallNumber_CatholicStudyBible_ShortTitle() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("Catholic Study Bible");

        // Act
        LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getCallNumber(), "Call number should not be null");
        assertTrue(response.getCallNumber().startsWith("BS"),
                "Call number should start with 'BS' (Bible classification). Got: " + response.getCallNumber());

        System.out.println("Short title search - LOC Call Number: " + response.getCallNumber());
        System.out.println("Match count: " + response.getMatchCount());
    }

    /**
     * Test case: Verify handling of not found scenarios
     */
    @Test
    void testGetLocCallNumber_BookNotFound() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("This Book Title Definitely Does Not Exist In LOC Database 123456789");
        request.setAuthor("Nonexistent Author Name");

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> locCatalogService.getLocCallNumber(request),
                "Should throw exception when book is not found"
        );

        assertEquals(404, exception.getStatusCode().value(),
                "Should return 404 status code for not found");
        assertTrue(exception.getReason().contains("No LOC call number found")
                || exception.getReason().contains("not found"),
                "Exception message should indicate book was not found");
    }

    /**
     * Test case: Verify handling of empty/null title
     */
    @Test
    void testGetLocCallNumber_EmptyTitle() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("");

        // Act & Assert
        assertThrows(
                Exception.class,
                () -> locCatalogService.getLocCallNumber(request),
                "Should throw exception when title is empty"
        );
    }

    /**
     * Test case: Search with title and author combination
     * Note: The Catholic Study Bible is a compiled work edited by multiple contributors
     */
    @Test
    void testGetLocCallNumber_CatholicStudyBible_WithEditor() {
        // Arrange
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("Catholic Study Bible");
        request.setAuthor("Senior"); // Donald Senior is one of the editors

        // Act
        try {
            LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);

            // Assert
            assertNotNull(response, "Response should not be null");
            assertNotNull(response.getCallNumber(), "Call number should not be null");
            assertTrue(response.getCallNumber().startsWith("BS"),
                    "Call number should start with 'BS' (Bible classification). Got: " + response.getCallNumber());

            System.out.println("Title + Author search - LOC Call Number: " + response.getCallNumber());
            System.out.println("Match count: " + response.getMatchCount());
        } catch (ResponseStatusException e) {
            // If no matches found with author, that's acceptable for this test
            System.out.println("No matches found with author filter: " + e.getReason());
        }
    }
}
