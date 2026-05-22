/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * API Integration Tests for SearchController using RestAssured.
 *
 * Tests REST endpoints with actual HTTP requests. The search API accepts
 * four boolean filter params (filterInLibrary, filterElectronic, filterFreeText,
 * filterAudio) instead of a single searchType string.
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

    // ── Helper matchers ───────────────────────────────────────────────────

    private static SearchResponseDto emptyResponse(int bookPageSize) {
        return new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                new PageInfoDto(0, 0, 0, bookPageSize),
                new PageInfoDto(0, 0, 0, bookPageSize));
    }

    // ── Basic search tests ────────────────────────────────────────────────

    @Test
    void testSearch_Success() {
        when(searchService.search(anyString(), anyInt(), anyInt(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), isNull()))
                .thenReturn(emptyResponse(10));

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
        BookDto book = new BookDto();
        book.setId(1L);
        book.setTitle("Test Book");

        SearchResponseDto searchResults = new SearchResponseDto(
                List.of(book),
                Collections.emptyList(),
                new PageInfoDto(1, 1, 0, 10),
                new PageInfoDto(0, 0, 0, 10));

        when(searchService.search(eq("Test Book"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(searchResults);

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
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                new PageInfoDto(5, 100, 2, 20),
                new PageInfoDto(0, 0, 2, 20));

        when(searchService.search(eq("book"), eq(2), eq(20),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(searchResults);

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
    void testSearch_MissingQueryDefaultsToEmpty() {
        // Missing query parameter defaults to "" and returns paginated results
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                new PageInfoDto(1, 10, 0, 10),
                new PageInfoDto(1, 5, 0, 10));

        when(searchService.search(eq(""), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(searchResults);

        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200)
            .body("bookPage.totalElements", equalTo(10));
    }

    @Test
    void testSearch_MissingPageParameterReturnsBadRequest() {
        given()
            .param("query", "test")
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(400);
    }

    @Test
    void testSearch_EmptyQueryIsValid() {
        // Empty query string returns all results — blank search is permitted
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                new PageInfoDto(1, 5, 0, 10),
                new PageInfoDto(1, 3, 0, 10));

        when(searchService.search(eq(""), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(searchResults);

        given()
            .param("query", "")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200)
            .body("bookPage.totalElements", equalTo(5))
            .body("authorPage.totalElements", equalTo(3));
    }

    @Test
    void testSearch_ServiceThrowsExceptionReturns500() {
        when(searchService.search(anyString(), anyInt(), anyInt(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), isNull()))
                .thenThrow(new RuntimeException("Database error"));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(500);
    }

    // ── Filter parameter tests ────────────────────────────────────────────

    @Test
    void testSearch_WithInLibraryFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(true), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterInLibrary", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_WithElectronicFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(true), eq(false), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterElectronic", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_WithFreeTextFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(true), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterFreeText", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_WithAudioFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(true), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterAudio", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_DefaultFiltersAreFalse() {
        // With no filter params, all booleans default to false
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_MultipleFiltersCanBeActive() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(true), eq(false), eq(true), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterInLibrary", true)
            .param("filterFreeText", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }
}
