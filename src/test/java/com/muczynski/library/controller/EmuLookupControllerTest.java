/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.EmuLookupResultDto;
import com.muczynski.library.service.EmuLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Integration Tests for EmuLookupController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmuLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmuLookupService emuLookupService;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void lookupSingleBook_librarian_returnsResult() throws Exception {
        EmuLookupResultDto result = EmuLookupResultDto.builder()
                .bookId(1L)
                .success(true)
                .audioAvailable(true)
                .paperAvailable(false)
                .ebookAvailable(true)
                .matchedTitle("Test Book")
                .build();

        when(emuLookupService.lookupAndUpdateBook(eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/emu-lookup/lookup/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.audioAvailable").value(true))
                .andExpect(jsonPath("$.paperAvailable").value(false))
                .andExpect(jsonPath("$.ebookAvailable").value(true));
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void lookupSingleBook_regularUser_returnsResult() throws Exception {
        EmuLookupResultDto result = EmuLookupResultDto.builder()
                .bookId(1L)
                .success(true)
                .audioAvailable(false)
                .paperAvailable(true)
                .ebookAvailable(false)
                .matchedTitle("Test Book")
                .build();

        when(emuLookupService.lookupAndUpdateBook(eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/emu-lookup/lookup/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paperAvailable").value(true));
    }

    @Test
    void lookupSingleBook_unauthenticated_returnsResult() throws Exception {
        EmuLookupResultDto result = EmuLookupResultDto.builder()
                .bookId(1L)
                .success(true)
                .audioAvailable(true)
                .paperAvailable(false)
                .ebookAvailable(false)
                .matchedTitle("Test Book")
                .build();

        when(emuLookupService.lookupAndUpdateBook(eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/emu-lookup/lookup/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.audioAvailable").value(true));
    }
}
