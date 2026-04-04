/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.integration;

import com.muczynski.library.dto.BookDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @Sql(scripts = "/data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup-integration.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getBooksWith3LetterLocStart_returnsFilteredBooks() throws Exception {
        // Filter endpoints now return BookSummaryDto (id + lastModified) for cache validation
        mockMvc.perform(get("/api/books/by-3letter-loc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(999L))
                .andExpect(jsonPath("$[0].lastModified").exists());
    }

    @Test
    @WithMockUser
    @Sql(scripts = "/data-integration-nonmatching.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup-integration.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getBooksWith3LetterLocStart_returnsEmpty_whenNoMatchingBooks() throws Exception {
        mockMvc.perform(get("/api/books/by-3letter-loc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    @Sql(scripts = "/data-integration-without-grokipedia.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup-integration.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getBooksWithoutGrokipediaUrl_returnsFilteredBooks() throws Exception {
        // Filter endpoints now return BookSummaryDto (id + lastModified) for cache validation
        mockMvc.perform(get("/api/books/without-grokipedia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].lastModified").exists())
                .andExpect(jsonPath("$[1].id").exists())
                .andExpect(jsonPath("$[1].lastModified").exists());
    }

    @Test
    @WithMockUser
    @Sql(scripts = "/data-integration-with-grokipedia.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup-integration.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getBooksWithoutGrokipediaUrl_returnsEmpty_whenAllBooksHaveGrokipedia() throws Exception {
        mockMvc.perform(get("/api/books/without-grokipedia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Verifies that cloning a book produces UTC-based timestamps (no local-time offset).
     * The cloned book's dateAddedToLibrary and lastModified must be within 30 seconds
     * of UTC now, confirming the fix for the 4-5 hour timezone drift bug.
     */
    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    @Sql(scripts = "/data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup-integration-clone.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void cloneBook_timestampsAreUtc() throws Exception {
        LocalDateTime beforeClone = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(5);

        MvcResult result = mockMvc.perform(post("/api/books/999/clone"))
                .andExpect(status().isCreated())
                .andReturn();

        LocalDateTime afterClone = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(5);

        BookDto clonedBook = objectMapper.readValue(
                result.getResponse().getContentAsString(), BookDto.class);

        assertThat(clonedBook.getDateAddedToLibrary())
                .as("dateAddedToLibrary should be UTC-based (within 30s of now)")
                .isNotNull()
                .isAfterOrEqualTo(beforeClone)
                .isBeforeOrEqualTo(afterClone);

        assertThat(clonedBook.getLastModified())
                .as("lastModified should be UTC-based (within 30s of now)")
                .isNotNull()
                .isAfterOrEqualTo(beforeClone)
                .isBeforeOrEqualTo(afterClone);
    }
}
