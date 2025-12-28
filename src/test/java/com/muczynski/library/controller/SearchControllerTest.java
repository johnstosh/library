/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PageInfoDto;
import com.muczynski.library.dto.SearchResponseDto;
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

import java.util.Collections;
import java.util.List;

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
        PageInfoDto bookPage = new PageInfoDto(0, 0, 0, 10);
        PageInfoDto authorPage = new PageInfoDto(0, 0, 0, 10);
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                bookPage,
                authorPage
        );

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
            .body("books", hasSize(0))
            .body("authors", hasSize(0))
            .body("bookPage.currentPage", equalTo(0))
            .body("bookPage.pageSize", equalTo(10));
    }

    @Test
    void testSearch_WithResults() {
        // Arrange
        BookDto book = new BookDto();
        book.setId(1L);
        book.setTitle("Test Book");

        PageInfoDto bookPage = new PageInfoDto(1, 1, 0, 10);
        PageInfoDto authorPage = new PageInfoDto(0, 0, 0, 10);
        SearchResponseDto searchResults = new SearchResponseDto(
                List.of(book),
                Collections.emptyList(),
                bookPage,
                authorPage
        );

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
            .body("books", hasSize(1))
            .body("books[0].title", equalTo("Test Book"))
            .body("bookPage.totalElements", equalTo(1));
    }

    @Test
    void testSearch_Pagination() {
        // Test pagination works
        PageInfoDto bookPage = new PageInfoDto(5, 100, 2, 20);
        PageInfoDto authorPage = new PageInfoDto(0, 0, 2, 20);
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                bookPage,
                authorPage
        );

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
            .body("bookPage.currentPage", equalTo(2))
            .body("bookPage.pageSize", equalTo(20))
            .body("bookPage.totalElements", equalTo(100));
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
        // Test empty query string - should throw IllegalArgumentException
        when(searchService.search("", 0, 10))
                .thenThrow(new IllegalArgumentException("Query cannot be empty"));

        // Act & Assert
        given()
            .param("query", "")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(500); // Internal Server Error due to IllegalArgumentException
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
