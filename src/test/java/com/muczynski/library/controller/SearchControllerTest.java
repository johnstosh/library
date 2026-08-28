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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * API Integration Tests for SearchController using RestAssured.
 *
 * Tests REST endpoints with actual HTTP requests. The search API accepts
 * boolean filter params (filterInLibrary, filterElectronic, filterFreeText,
 * filterAudio, plus row-2 chips) instead of a single searchType string.
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

    private void stubAnySearch(SearchResponseDto response) {
        when(searchService.search(anyString(), anyInt(), anyInt(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), isNull()))
                .thenReturn(response);
    }

    private void stubSearch(String query, int page, int size,
            boolean inLib, boolean elec, boolean freeText, boolean audio,
            SearchResponseDto response) {
        when(searchService.search(eq(query), eq(page), eq(size),
                eq(inLib), eq(elec), eq(freeText), eq(audio),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(response);
    }

    // ── Basic search tests ────────────────────────────────────────────────

    @Test
    void testSearch_Success() {
        stubAnySearch(emptyResponse(10));

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

        stubSearch("Test Book", 0, 10, false, false, false, false, searchResults);

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

        stubSearch("book", 2, 20, false, false, false, false, searchResults);

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
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                new PageInfoDto(1, 10, 0, 10),
                new PageInfoDto(1, 5, 0, 10));

        stubSearch("", 0, 10, false, false, false, false, searchResults);

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
        SearchResponseDto searchResults = new SearchResponseDto(
                Collections.emptyList(),
                Collections.emptyList(),
                new PageInfoDto(1, 5, 0, 10),
                new PageInfoDto(1, 3, 0, 10));

        stubSearch("", 0, 10, false, false, false, false, searchResults);

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
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(),
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
        stubSearch("test", 0, 10, true, false, false, false, emptyResponse(10));

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
        stubSearch("test", 0, 10, false, true, false, false, emptyResponse(10));

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
        stubSearch("test", 0, 10, false, false, true, false, emptyResponse(10));

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
        stubSearch("test", 0, 10, false, false, false, true, emptyResponse(10));

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
        stubSearch("test", 0, 10, false, false, false, false, emptyResponse(10));

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
        stubSearch("test", 0, 10, true, false, true, false, emptyResponse(10));

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

    @Test
    void testSearch_WithNotActiveStatusFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(true), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterNotActiveStatus", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);

        verify(searchService).search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(true), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false), isNull());
    }

    @Test
    void testSearch_WithMostRecentAndWithoutLocFilters() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false),
                eq(true), eq(true), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterMostRecent", true)
            .param("filterWithoutLoc", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_WithYdlAudioFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(true), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterYdlAudio", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }

    @Test
    void testSearch_WithGrokipediaFilter() {
        when(searchService.search(eq("test"), eq(0), eq(10),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false),
                eq(false), eq(false), eq(false), eq(true), isNull()))
                .thenReturn(emptyResponse(10));

        given()
            .param("query", "test")
            .param("page", 0)
            .param("size", 10)
            .param("filterWithGrokipedia", true)
        .when()
            .get("/api/search")
        .then()
            .statusCode(200);
    }
}
