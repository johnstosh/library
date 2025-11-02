/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.SearchService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * API Integration Tests for SearchController using RestAssured
 *
 * Tests REST endpoints with actual HTTP requests according to backend-development-requirements.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // ==================== GET /api/search Tests ====================

    @Test
    void testSearch_Success() {
        // Arrange
        Map<String, Object> searchResults = new HashMap<>();
        searchResults.put("totalResults", 10);
        searchResults.put("page", 0);
        searchResults.put("size", 10);
        searchResults.put("results", java.util.List.of());

        when(searchService.search(anyString(), anyInt(), anyInt())).thenReturn(searchResults);

        // Act & Assert
        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200)
            .body("totalResults", equalTo(10))
            .body("page", equalTo(0))
            .body("size", equalTo(10));
    }

    @Test
    void testSearch_WithResults() {
        // Arrange
        Map<String, Object> book = new HashMap<>();
        book.put("id", 1);
        book.put("title", "Test Book");

        Map<String, Object> searchResults = new HashMap<>();
        searchResults.put("totalResults", 1);
        searchResults.put("page", 0);
        searchResults.put("size", 10);
        searchResults.put("results", java.util.List.of(book));

        when(searchService.search("Test Book", 0, 10)).thenReturn(searchResults);

        // Act & Assert
        given()
            .param("query", "Test Book")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200)
            .body("totalResults", equalTo(1))
            .body("results", hasSize(1))
            .body("results[0].title", equalTo("Test Book"));
    }

    @Test
    void testSearch_Pagination() {
        // Test pagination works
        Map<String, Object> searchResults = new HashMap<>();
        searchResults.put("totalResults", 100);
        searchResults.put("page", 2);
        searchResults.put("size", 20);
        searchResults.put("results", java.util.List.of());

        when(searchService.search("book", 2, 20)).thenReturn(searchResults);

        // Act & Assert
        given()
            .param("query", "book")
            .param("page", 2)
            .param("size", 20)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200)
            .body("page", equalTo(2))
            .body("size", equalTo(20))
            .body("totalResults", equalTo(100));
    }

    @Test
    void testSearch_MissingQueryParameter() {
        // Act & Assert - Missing required query parameter
        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(400); // Bad Request for missing required parameter
    }

    @Test
    void testSearch_MissingPageParameter() {
        // Act & Assert - Missing required page parameter
        given()
            .param("query", "test")
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(400); // Bad Request for missing required parameter
    }

    @Test
    void testSearch_EmptyQuery() {
        // Test empty query string
        Map<String, Object> searchResults = new HashMap<>();
        searchResults.put("totalResults", 0);
        searchResults.put("page", 0);
        searchResults.put("size", 10);
        searchResults.put("results", java.util.List.of());

        when(searchService.search("", 0, 10)).thenReturn(searchResults);

        // Act & Assert
        given()
            .param("query", "")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200)
            .body("totalResults", equalTo(0));
    }

    @Test
    void testSearch_ServiceThrowsException() {
        // Arrange - Service throws exception
        when(searchService.search(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(500); // Internal Server Error
    }
}
