/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.service.LoanService;
import com.muczynski.library.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * API Integration Tests for LoanController
 *
 * Tests REST endpoints with actual HTTP requests according to backend-development-requirements.md
 * Each endpoint should have:
 * - One test for successful request (2xx status)
 * - One test for unauthorized access (401/403 status)
 * - One test for invalid input (400 status) where applicable
 *
 * Tests cover both librarian and regular user scenarios for new authorization model
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== POST /api/loans/checkout Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testCheckoutBook_Success_AsLibrarian() throws Exception {
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(1L);
        LoanDto returnedDto = new LoanDto();
        returnedDto.setId(1L);
        returnedDto.setBookId(1L);
        returnedDto.setUserId(1L);
        when(loanService.checkoutBook(any(LoanDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testCheckoutBook_Success_AsRegularUser() throws Exception {
        // Regular users can checkout books (to themselves)
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(1L);  // Their own user ID
        LoanDto returnedDto = new LoanDto();
        returnedDto.setId(1L);
        returnedDto.setBookId(1L);
        returnedDto.setUserId(1L);
        when(loanService.checkoutBook(any(LoanDto.class))).thenReturn(returnedDto);

        mockMvc.perform(post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated());
    }

    @Test
    void testCheckoutBook_Unauthorized() throws Exception {
        // No authentication
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(1L);

        mockMvc.perform(post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/loans Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGetAllLoans_Success_AsLibrarian() throws Exception {
        // Librarians see all loans
        LoanDto dto = new LoanDto();
        dto.setId(1L);
        dto.setBookId(1L);
        when(loanService.getAllLoans(false)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk());

        when(loanService.getAllLoans(true)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/loans?showAll=true"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testGetAllLoans_Success_AsRegularUser() throws Exception {
        // Regular users see only their own loans
        LoanDto dto = new LoanDto();
        dto.setId(1L);
        dto.setBookId(1L);
        dto.setUserId(2L);
        when(loanService.getLoansByUserId(eq(1L), eq(false)))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testGetAllLoans_RegularUserSeesOnlyOwnLoans() throws Exception {
        // Verify regular user gets filtered results
        LoanDto ownLoan = new LoanDto();
        ownLoan.setId(1L);
        ownLoan.setBookId(1L);
        ownLoan.setUserId(2L);

        when(loanService.getLoansByUserId(eq(1L), eq(false)))
                .thenReturn(Collections.singletonList(ownLoan));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2L));
    }

    @Test
    void testGetAllLoans_Unauthorized() throws Exception {
        // No authentication
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/loans/{id} Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGetLoanById_Success_AsLibrarian() throws Exception {
        LoanDto loanDto = new LoanDto();
        loanDto.setId(1L);
        loanDto.setBookId(1L);
        when(loanService.getLoanById(1L)).thenReturn(loanDto);

        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testGetLoanById_Forbidden_AsRegularUser() throws Exception {
        // Regular users cannot get loans by ID (librarian-only)
        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetLoanById_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/loans/{id} Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testUpdateLoan_Success_AsLibrarian() throws Exception {
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(1L);
        LoanDto returnedDto = new LoanDto();
        returnedDto.setId(1L);
        returnedDto.setBookId(1L);
        returnedDto.setUserId(1L);
        when(loanService.updateLoan(eq(1L), any(LoanDto.class))).thenReturn(returnedDto);

        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testUpdateLoan_Forbidden_AsRegularUser() throws Exception {
        // Regular users cannot update loans
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(1L);

        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateLoan_Unauthorized() throws Exception {
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(1L);

        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /api/loans/{id} Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testDeleteLoan_Success_AsLibrarian() throws Exception {
        doNothing().when(loanService).deleteLoan(1L);

        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testDeleteLoan_Forbidden_AsRegularUser() throws Exception {
        // Regular users cannot delete loans
        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteLoan_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/loans/return/{id} Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testReturnBook_Success_AsLibrarian() throws Exception {
        LoanDto returnedDto = new LoanDto();
        returnedDto.setId(1L);
        when(loanService.returnBook(1L)).thenReturn(returnedDto);

        mockMvc.perform(put("/api/loans/return/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testReturnBook_Forbidden_AsRegularUser() throws Exception {
        // Regular users cannot return books (librarian-only)
        mockMvc.perform(put("/api/loans/return/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReturnBook_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/loans/return/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Additional Tests for Coverage ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGetLoanById_NotFound() throws Exception {
        when(loanService.getLoanById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/loans/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testReturnBook_NotFound() throws Exception {
        when(loanService.returnBook(999L)).thenReturn(null);

        mockMvc.perform(put("/api/loans/return/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testCheckoutBook_InvalidInput_MissingBookId() throws Exception {
        LoanDto inputDto = new LoanDto();
        inputDto.setUserId(1L);
        // bookId is null - should fail validation

        mockMvc.perform(post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testCheckoutBook_InvalidInput_MissingUserId() throws Exception {
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        // userId is null - should fail validation

        mockMvc.perform(post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testCheckoutBook_Forbidden_CheckoutToDifferentUser() throws Exception {
        // Regular user attempting to checkout a book to a different user
        LoanDto inputDto = new LoanDto();
        inputDto.setBookId(1L);
        inputDto.setUserId(999L);  // Different user ID

        mockMvc.perform(post("/api/loans/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }
}
