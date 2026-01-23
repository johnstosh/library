/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.FreeTextBulkLookupResultDto;
import com.muczynski.library.freetext.FreeTextLookupService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FreeTextLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FreeTextLookupService freeTextLookupService;

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void lookupBook_returnsResult() throws Exception {
        FreeTextBulkLookupResultDto result = FreeTextBulkLookupResultDto.builder()
                .bookId(1L)
                .bookTitle("Pride and Prejudice")
                .authorName("Jane Austen")
                .success(true)
                .freeTextUrl("https://www.gutenberg.org/ebooks/1342")
                .providerName("Project Gutenberg")
                .providersSearched(List.of("Project Gutenberg"))
                .build();

        when(freeTextLookupService.lookupBook(1L)).thenReturn(result);

        mockMvc.perform(post("/api/free-text/lookup/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.freeTextUrl").value("https://www.gutenberg.org/ebooks/1342"))
                .andExpect(jsonPath("$.providerName").value("Project Gutenberg"));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void lookupBook_returnsFailureResult() throws Exception {
        FreeTextBulkLookupResultDto result = FreeTextBulkLookupResultDto.builder()
                .bookId(1L)
                .bookTitle("Obscure Book")
                .success(false)
                .errorMessage("Not found in any provider")
                .providersSearched(List.of("Project Gutenberg", "Internet Archive"))
                .build();

        when(freeTextLookupService.lookupBook(1L)).thenReturn(result);

        mockMvc.perform(post("/api/free-text/lookup/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Not found in any provider"))
                .andExpect(jsonPath("$.providersSearched.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void lookupBooks_returnsBulkResults() throws Exception {
        List<FreeTextBulkLookupResultDto> results = Arrays.asList(
                FreeTextBulkLookupResultDto.builder()
                        .bookId(1L)
                        .bookTitle("Book 1")
                        .success(true)
                        .freeTextUrl("https://example.com/book1")
                        .providerName("Provider1")
                        .providersSearched(List.of("Provider1"))
                        .build(),
                FreeTextBulkLookupResultDto.builder()
                        .bookId(2L)
                        .bookTitle("Book 2")
                        .success(false)
                        .errorMessage("Not found")
                        .providersSearched(List.of("Provider1", "Provider2"))
                        .build()
        );

        when(freeTextLookupService.lookupBooks(anyList())).thenReturn(results);

        mockMvc.perform(post("/api/free-text/lookup-bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].success").value(true))
                .andExpect(jsonPath("$[1].success").value(false));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void getProviders_returnsList() throws Exception {
        List<String> providers = Arrays.asList("Project Gutenberg", "Internet Archive", "LibriVox");

        when(freeTextLookupService.getProviderNames()).thenReturn(providers);

        mockMvc.perform(get("/api/free-text/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Project Gutenberg"))
                .andExpect(jsonPath("$[1]").value("Internet Archive"))
                .andExpect(jsonPath("$[2]").value("LibriVox"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void lookupBook_forbiddenForNonLibrarian() throws Exception {
        mockMvc.perform(post("/api/free-text/lookup/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lookupBook_unauthorizedWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/free-text/lookup/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
