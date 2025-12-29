/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.TestDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Integration Tests for TestDataController
 *
 * Tests REST endpoints with actual HTTP requests.
 * Note: All endpoints use @PreAuthorize("permitAll()") so no auth tests needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestDataService testDataService;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private AuthorRepository authorRepository;

    @MockitoBean
    private LoanRepository loanRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== POST /api/test-data/generate Tests ====================

    @Test
    void testGenerateTestData_Success() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numBooks", 10);

        doNothing().when(testDataService).generateTestData(10);

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test data generated successfully for 10 books"));

        verify(testDataService, times(1)).generateTestData(10);
    }

    @Test
    void testGenerateTestData_WithZeroCount() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numBooks", 0);

        doNothing().when(testDataService).generateTestData(0);

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test data generated successfully for 0 books"));

        verify(testDataService, times(1)).generateTestData(0);
    }

    @Test
    void testGenerateTestData_DefaultsToZeroWhenNotProvided() throws Exception {
        // Arrange - Empty payload should default to 0
        Map<String, Integer> payload = new HashMap<>();

        doNothing().when(testDataService).generateTestData(0);

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(testDataService, times(1)).generateTestData(0);
    }

    @Test
    void testGenerateTestData_ServiceThrowsException() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numBooks", 10);

        doThrow(new RuntimeException("Database error")).when(testDataService).generateTestData(anyInt());

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Database error"));
    }

    // ==================== POST /api/test-data/generate-loans Tests ====================

    @Test
    void testGenerateLoanData_Success() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numLoans", 5);

        doNothing().when(testDataService).generateLoanData(5);

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate-loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test data generated successfully for 5 loans"));

        verify(testDataService, times(1)).generateLoanData(5);
    }

    @Test
    void testGenerateLoanData_ServiceThrowsException() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numLoans", 5);

        doThrow(new RuntimeException("No books available")).when(testDataService).generateLoanData(anyInt());

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate-loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No books available"));
    }

    // ==================== POST /api/test-data/generate-users Tests ====================

    @Test
    void testGenerateUserData_Success() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numUsers", 5);

        doNothing().when(testDataService).generateUserData(5);

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test data generated successfully for 5 users"));

        verify(testDataService, times(1)).generateUserData(5);
    }

    @Test
    void testGenerateUserData_ServiceThrowsException() throws Exception {
        // Arrange
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numUsers", 5);

        doThrow(new RuntimeException("Authority not found")).when(testDataService).generateUserData(anyInt());

        // Act & Assert
        mockMvc.perform(post("/api/test-data/generate-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authority not found"));
    }

    // ==================== DELETE /api/test-data/delete-all Tests ====================

    @Test
    void testDeleteAll_Success() throws Exception {
        // Arrange
        doNothing().when(testDataService).deleteTestData();

        // Act & Assert
        mockMvc.perform(delete("/api/test-data/delete-all"))
                .andExpect(status().isOk());

        verify(testDataService, times(1)).deleteTestData();
    }

    @Test
    void testDeleteAll_ServiceThrowsException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Delete failed")).when(testDataService).deleteTestData();

        // Act & Assert
        mockMvc.perform(delete("/api/test-data/delete-all"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== DELETE /api/test-data/total-purge Tests ====================

    @Test
    void testTotalPurge_Success() throws Exception {
        // Arrange
        doNothing().when(testDataService).totalPurge();

        // Act & Assert
        mockMvc.perform(delete("/api/test-data/total-purge"))
                .andExpect(status().isOk());

        verify(testDataService, times(1)).totalPurge();
    }

    @Test
    void testTotalPurge_ServiceThrowsException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Purge failed")).when(testDataService).totalPurge();

        // Act & Assert
        mockMvc.perform(delete("/api/test-data/total-purge"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET /api/test-data/stats Tests ====================

    @Test
    void testGetStats_Success() throws Exception {
        // Arrange
        when(bookRepository.count()).thenReturn(42L);
        when(authorRepository.count()).thenReturn(15L);
        when(loanRepository.count()).thenReturn(7L);
        when(userRepository.count()).thenReturn(10L);

        // Act & Assert
        mockMvc.perform(get("/api/test-data/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(42))
                .andExpect(jsonPath("$.authors").value(15))
                .andExpect(jsonPath("$.loans").value(7))
                .andExpect(jsonPath("$.users").value(10));
    }

    @Test
    void testGetStats_WithZeroCounts() throws Exception {
        // Arrange
        when(bookRepository.count()).thenReturn(0L);
        when(authorRepository.count()).thenReturn(0L);
        when(loanRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/test-data/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(0))
                .andExpect(jsonPath("$.authors").value(0))
                .andExpect(jsonPath("$.loans").value(0))
                .andExpect(jsonPath("$.users").value(0));
    }

    @Test
    void testGetStats_RepositoryThrowsException() throws Exception {
        // Arrange
        when(bookRepository.count()).thenThrow(new RuntimeException("Database connection lost"));

        // Act & Assert
        mockMvc.perform(get("/api/test-data/stats"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== Integration Workflow Test ====================

    @Test
    void testWorkflow_GenerateStatsDeleteVerify() throws Exception {
        // This test simulates the UI workflow:
        // 1. Check initial stats
        // 2. Generate test data
        // 3. Check stats again (should be higher)
        // 4. Delete all test data
        // 5. Check stats again (should be back to initial)

        // Step 1: Initial stats
        when(bookRepository.count()).thenReturn(0L);
        when(authorRepository.count()).thenReturn(0L);
        when(loanRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/api/test-data/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(0));

        // Step 2: Generate 10 books
        Map<String, Integer> payload = new HashMap<>();
        payload.put("numBooks", 10);

        doNothing().when(testDataService).generateTestData(10);

        mockMvc.perform(post("/api/test-data/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 3: Stats should reflect new data
        when(bookRepository.count()).thenReturn(10L);
        when(authorRepository.count()).thenReturn(10L);

        mockMvc.perform(get("/api/test-data/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(10))
                .andExpect(jsonPath("$.authors").value(10));

        // Step 4: Delete all test data
        doNothing().when(testDataService).deleteTestData();

        mockMvc.perform(delete("/api/test-data/delete-all"))
                .andExpect(status().isOk());

        // Step 5: Verify stats back to zero
        when(bookRepository.count()).thenReturn(0L);
        when(authorRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/api/test-data/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(0))
                .andExpect(jsonPath("$.authors").value(0));
    }
}
