/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.service.LabelsService;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * API Integration Tests for LabelsController using RestAssured
 *
 * Tests REST endpoints for book label PDF generation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LabelsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelsService labelsService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // ==================== GET /api/labels/generate Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGenerateLabelsPdf_Success() {
        // Arrange
        byte[] mockPdfBytes = "PDF content".getBytes();
        when(labelsService.generateLabelsPdf(anyList())).thenReturn(mockPdfBytes);

        // Act & Assert
        given()
            .queryParam("bookIds", 1L, 2L, 3L)
            .auth().none() // WithMockUser handles auth
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(200)
            .contentType("application/pdf")
            .header("Content-Disposition", "form-data; name=\"attachment\"; filename=\"book-labels.pdf\"");
    }

    @Test
    void testGenerateLabelsPdf_Unauthorized() {
        // Act & Assert - No authentication
        given()
            .queryParam("bookIds", 1L, 2L)
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(401);
    }

    @Test
    @WithMockUser(authorities = "USER")
    void testGenerateLabelsPdf_Forbidden_RegularUser() {
        // Act & Assert - Regular USER should not have access
        given()
            .queryParam("bookIds", 1L, 2L)
            .auth().none()
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(403);
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGenerateLabelsPdf_EmptyBookIds() {
        // Arrange
        when(labelsService.generateLabelsPdf(anyList()))
                .thenThrow(new LibraryException("No books selected for labels"));

        // Act & Assert
        given()
            .queryParam("bookIds", List.of())
            .auth().none()
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(500);
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGenerateLabelsPdf_InvalidBookIds() {
        // Arrange
        when(labelsService.generateLabelsPdf(anyList()))
                .thenThrow(new LibraryException("No books found for the given IDs"));

        // Act & Assert
        given()
            .queryParam("bookIds", 999L, 1000L)
            .auth().none()
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(422);
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGenerateLabelsPdf_SingleBook() {
        // Arrange
        byte[] mockPdfBytes = "PDF content for single book".getBytes();
        when(labelsService.generateLabelsPdf(anyList())).thenReturn(mockPdfBytes);

        // Act & Assert
        given()
            .queryParam("bookIds", 1L)
            .auth().none()
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(200)
            .contentType("application/pdf");
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGenerateLabelsPdf_ServiceException() {
        // Arrange
        when(labelsService.generateLabelsPdf(anyList()))
                .thenThrow(new RuntimeException("PDF generation failed"));

        // Act & Assert
        given()
            .queryParam("bookIds", 1L, 2L)
            .auth().none()
        .when()
            .get("/api/labels/generate")
        .then()
            .statusCode(500);
    }
}
