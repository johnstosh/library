/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.YdlLookupResultDto;
import com.muczynski.library.service.YdlLookupService;
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
 * API Integration Tests for YdlLookupController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class YdlLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YdlLookupService ydlLookupService;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void lookupSingleBook_librarian_returnsResult() throws Exception {
        YdlLookupResultDto result = YdlLookupResultDto.builder()
                .bookId(1L)
                .success(true)
                .audioAvailable(true)
                .paperAvailable(false)
                .ebookAvailable(true)
                .matchedTitle("Test Book")
                .build();

        when(ydlLookupService.lookupAndUpdateBook(eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/ydl-lookup/lookup/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.audioAvailable").value(true))
                .andExpect(jsonPath("$.paperAvailable").value(false))
                .andExpect(jsonPath("$.ebookAvailable").value(true));
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void lookupSingleBook_regularUser_forbidden() throws Exception {
        mockMvc.perform(post("/api/ydl-lookup/lookup/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lookupSingleBook_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(post("/api/ydl-lookup/lookup/1"))
                .andExpect(status().isUnauthorized());
    }
}
