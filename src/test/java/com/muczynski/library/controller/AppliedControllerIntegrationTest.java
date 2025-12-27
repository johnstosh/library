/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.controller.payload.RegistrationRequest;
import com.muczynski.library.domain.Applied;
import com.muczynski.library.repository.AppliedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AppliedController that test actual database operations.
 * These tests verify the full registration flow from HTTP request to database persistence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppliedControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppliedRepository appliedRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing applications
        appliedRepository.deleteAll();
    }

    @Test
    void testPublicRegister_SavesApplicationToDatabase() throws Exception {
        // Arrange - Use valid SHA-256 hash (64 hex chars)
        String validSHA256Hash = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        String requestJson = """
            {
                "username": "John Smith",
                "password": "%s",
                "authority": "USER"
            }
            """.formatted(validSHA256Hash);

        // Act - Submit application via HTTP POST
        mockMvc.perform(post("/api/application/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNoContent());

        // Assert - Verify application was saved to database
        List<Applied> applications = appliedRepository.findAll();
        assertThat(applications).hasSize(1);

        Applied savedApplication = applications.get(0);
        assertThat(savedApplication.getName()).isEqualTo("John Smith");
        // Password is bcrypt encoded, not the original SHA-256 hash
        assertThat(savedApplication.getPassword()).startsWith("$2a$");
        assertThat(savedApplication.getStatus()).isEqualTo(Applied.ApplicationStatus.PENDING);
        assertThat(savedApplication.getId()).isNotNull();
    }

    @Test
    void testPublicRegister_TrimmedUsername() throws Exception {
        // Arrange - Username with leading/trailing whitespace
        String validSHA256Hash = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";
        String requestJson = """
            {
                "username": "  Jane Doe  ",
                "password": "%s",
                "authority": "USER"
            }
            """.formatted(validSHA256Hash);

        // Act
        mockMvc.perform(post("/api/application/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNoContent());

        // Assert - Verify username is saved as-is (trimming happens in frontend)
        List<Applied> applications = appliedRepository.findAll();
        assertThat(applications).hasSize(1);
        assertThat(applications.get(0).getName()).isEqualTo("  Jane Doe  ");
    }

    @Test
    void testPublicRegister_MultipleApplications() throws Exception {
        // Arrange - First application
        String hash1 = "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb";
        String hash2 = "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d";

        String request1 = """
            {
                "username": "User One",
                "password": "%s",
                "authority": "USER"
            }
            """.formatted(hash1);

        // Second application
        String request2 = """
            {
                "username": "User Two",
                "password": "%s",
                "authority": "USER"
            }
            """.formatted(hash2);

        // Act - Submit both applications
        mockMvc.perform(post("/api/application/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request1))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/application/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request2))
                .andExpect(status().isNoContent());

        // Assert - Both applications saved
        List<Applied> applications = appliedRepository.findAll();
        assertThat(applications).hasSize(2);
        assertThat(applications)
            .extracting(Applied::getName)
            .containsExactlyInAnyOrder("User One", "User Two");
    }

    @Test
    void testPublicRegister_ReturnsCorrectStatusCode() throws Exception {
        // Arrange
        String validSHA256Hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String requestJson = """
            {
                "username": "Test User",
                "password": "%s",
                "authority": "USER"
            }
            """.formatted(validSHA256Hash);

        // Act & Assert - Verify 204 No Content response
        mockMvc.perform(post("/api/application/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNoContent());
    }
}
