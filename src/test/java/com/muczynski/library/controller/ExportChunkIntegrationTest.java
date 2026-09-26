/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.*;
import com.muczynski.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for chunked JSON export paging (Data Management ~33 GETs).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExportChunkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    private Library library;
    private Author author;

    @BeforeEach
    void setUp() {
        library = new Library();
        library.setBranchName("Chunk Library");
        library.setLibrarySystemName("Chunk System");
        library = branchRepository.save(library);

        author = new Author();
        author.setName("Chunk Author");
        author.setDateOfBirth(LocalDate.of(1900, 1, 1));
        author = authorRepository.save(author);

        for (int i = 1; i <= 5; i++) {
            Book book = new Book();
            book.setTitle("Chunk Book " + i);
            book.setPublicationYear(2000 + i);
            book.setPlotEssay("LOB summary " + i);
            book.setDetailedDescription("LOB detail " + i);
            book.setDateAddedToLibrary(LocalDateTime.now());
            book.setStatus(BookStatus.ACTIVE);
            book.setAuthor(author);
            book.setLibrary(library);
            bookRepository.save(book);
        }
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void chunkPlanIncludesAllSectionsAndPageSize() throws Exception {
        mockMvc.perform(get("/api/import/json/chunk-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetChunks", equalTo(33)))
                .andExpect(jsonPath("$.pageSize", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.sections[?(@.section=='books')].total", hasItem(5)))
                .andExpect(jsonPath("$.sections[?(@.section=='libraries')].total", hasItem(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.sections[?(@.section=='authors')].total", hasItem(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void booksChunkPagesWithKeysetAndDoesNotExceedLimit() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/import/json/chunk")
                        .param("section", "books")
                        .param("afterId", "0")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section", equalTo("books")))
                .andExpect(jsonPath("$.count", equalTo(2)))
                .andExpect(jsonPath("$.hasMore", equalTo(true)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title", startsWith("Chunk Book")))
                .andReturn();

        JsonNode page1 = objectMapper.readTree(first.getResponse().getContentAsString());
        long nextAfterId = page1.get("nextAfterId").asLong();
        assertTrue(nextAfterId > 0);

        MvcResult second = mockMvc.perform(get("/api/import/json/chunk")
                        .param("section", "books")
                        .param("afterId", String.valueOf(nextAfterId))
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", equalTo(2)))
                .andExpect(jsonPath("$.hasMore", equalTo(true)))
                .andReturn();

        JsonNode page2 = objectMapper.readTree(second.getResponse().getContentAsString());
        long next2 = page2.get("nextAfterId").asLong();

        mockMvc.perform(get("/api/import/json/chunk")
                        .param("section", "books")
                        .param("afterId", String.valueOf(next2))
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", equalTo(1)))
                .andExpect(jsonPath("$.hasMore", equalTo(false)));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void chunkedBooksAssembleToFullSection() throws Exception {
        List<String> titles = new ArrayList<>();
        long afterId = 0;
        boolean hasMore = true;
        int guard = 0;
        while (hasMore && guard++ < 20) {
            MvcResult result = mockMvc.perform(get("/api/import/json/chunk")
                            .param("section", "books")
                            .param("afterId", String.valueOf(afterId))
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode chunk = objectMapper.readTree(result.getResponse().getContentAsString());
            for (JsonNode item : chunk.get("items")) {
                titles.add(item.get("title").asText());
            }
            hasMore = chunk.get("hasMore").asBoolean();
            if (hasMore) {
                afterId = chunk.get("nextAfterId").asLong();
            }
        }
        assertEquals(5, titles.size());
        assertTrue(titles.contains("Chunk Book 1"));
        assertTrue(titles.contains("Chunk Book 5"));
    }

    @Test
    void chunkEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/import/json/chunk-plan"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/import/json/chunk").param("section", "books"))
                .andExpect(status().isUnauthorized());
    }
}
