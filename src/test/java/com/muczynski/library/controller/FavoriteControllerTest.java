/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.FavoriteItemType;
import com.muczynski.library.dto.FavoriteItemDto;
import com.muczynski.library.dto.FavoriteSummaryDto;
import com.muczynski.library.dto.FavoriteUpdateDto;
import com.muczynski.library.service.FavoriteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetSummary_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/favorites/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", authorities = "USER")
    void testGetSummary_Success() throws Exception {
        when(favoriteService.getSummary(1L))
                .thenReturn(new FavoriteSummaryDto(List.of(9L), List.of(4L)));

        mockMvc.perform(get("/api/favorites/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteBookIds[0]").value(9))
                .andExpect(jsonPath("$.favoriteAuthorIds[0]").value(4));
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void testReplaceItem_Success() throws Exception {
        FavoriteItemDto item = new FavoriteItemDto(
                FavoriteItemType.BOOK, 9L, List.of("Have Read"), List.of("Have Read", "Needs Review"));
        when(favoriteService.replaceItem(eq(1L), any(FavoriteUpdateDto.class), eq(true)))
                .thenReturn(item);

        mockMvc.perform(put("/api/favorites/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FavoriteUpdateDto(FavoriteItemType.BOOK, 9L, List.of("Have Read")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedLists[0]").value("Have Read"));
    }
}
