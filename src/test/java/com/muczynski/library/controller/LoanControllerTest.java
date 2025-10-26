// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void checkoutBook() throws Exception {
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
    @WithMockUser(authorities = "LIBRARIAN")
    void returnBook() throws Exception {
        LoanDto returnedDto = new LoanDto();
        returnedDto.setId(1L);
        when(loanService.returnBook(1L)).thenReturn(returnedDto);

        mockMvc.perform(put("/api/loans/return/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getAllLoans() throws Exception {
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
    @WithMockUser(authorities = "LIBRARIAN")
    void getLoanById() throws Exception {
        LoanDto loanDto = new LoanDto();
        loanDto.setId(1L);
        loanDto.setBookId(1L);
        when(loanService.getLoanById(1L)).thenReturn(loanDto);

        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void updateLoan() throws Exception {
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
    @WithMockUser(authorities = "LIBRARIAN")
    void deleteLoan() throws Exception {
        doNothing().when(loanService).deleteLoan(1L);

        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isNoContent());
    }
}
