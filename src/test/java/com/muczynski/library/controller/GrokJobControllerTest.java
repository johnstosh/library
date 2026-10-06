/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.AuthorEnrichmentResultDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.exception.BookHasNoPhotosException;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.service.AuthorService;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.BooksFromFeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Start-then-poll flow for long Grok actions: start returns 202 + jobId at once, the job
 * status endpoint returns the same result / error the synchronous endpoint would.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GrokJobControllerTest {

    private static final String OUT_OF_CREDITS =
            "Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private BooksFromFeedService booksFromFeedService;

    private String startJob(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("jobId").asText();
    }

    private JsonNode pollUntilDone(String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/grok-jobs/" + jobId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            if (!"RUNNING".equals(body.get("status").asText())) {
                return body;
            }
            Thread.sleep(20);
        }
        fail("Grok job did not finish");
        return null;
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void bookFromTitleAuthor_start_returns202ThenPollReturnsBook() throws Exception {
        BookDto updated = new BookDto();
        updated.setId(2120L);
        updated.setTitle("Pride and Prejudice");
        updated.setAuthorId(5L);
        when(bookService.getBookFromTitleAuthor(2120L, "Pride and Prejudice", "Jane Austen")).thenReturn(updated);

        MvcResult started = mockMvc.perform(post("/api/books/2120/book-from-title-author/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Pride and Prejudice", "authorName", "Jane Austen"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.kind").value("book-from-title-author"))
                .andExpect(header().exists("Location"))
                .andReturn();

        JsonNode done = pollUntilDone(startJob(started));
        assertEquals("SUCCEEDED", done.get("status").asText());
        assertEquals("Pride and Prejudice", done.get("result").get("title").asText());
        assertEquals(5L, done.get("result").get("authorId").asLong());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void bookByPhoto_start_outOfCredits_jobFailsWith402Message() throws Exception {
        when(bookService.generateTempBook(1L)).thenThrow(new GrokCreditsExhaustedException());

        MvcResult started = mockMvc.perform(post("/api/books/1/book-by-photo/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode done = pollUntilDone(startJob(started));
        assertEquals("FAILED", done.get("status").asText());
        assertEquals(402, done.get("httpStatus").asInt());
        assertEquals(OUT_OF_CREDITS, done.get("error").asText());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void bookFromFirstPhoto_start_noPhotos_jobFailsWith400Message() throws Exception {
        when(bookService.generateBookFromFirstPhoto(1L)).thenThrow(
                new BookHasNoPhotosException(BookHasNoPhotosException.BOOK_FROM_FIRST_PHOTO_MESSAGE));

        MvcResult started = mockMvc.perform(post("/api/books/1/book-from-first-photo/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode done = pollUntilDone(startJob(started));
        assertEquals("FAILED", done.get("status").asText());
        assertEquals(400, done.get("httpStatus").asInt());
        assertEquals("This book has no photos, so Book from First Photo has nothing to read.",
                done.get("error").asText());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void titleAuthorFromPhoto_start_returnsPreview() throws Exception {
        BookDto preview = new BookDto();
        preview.setId(1L);
        preview.setTitle("Extracted Title");
        when(bookService.getTitleAuthorFromPhoto(1L)).thenReturn(preview);

        MvcResult started = mockMvc.perform(post("/api/books/1/title-author-from-photo/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode done = pollUntilDone(startJob(started));
        assertEquals("SUCCEEDED", done.get("status").asText());
        assertEquals("Extracted Title", done.get("result").get("title").asText());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void authorGenerateMissing_start_returnsEnrichmentResult() throws Exception {
        when(authorService.generateMissingData(3L)).thenReturn(AuthorEnrichmentResultDto.builder()
                .authorId(3L).name("Jane Austen").success(true).skipped(false)
                .filledFields(List.of("nationality")).build());

        MvcResult started = mockMvc.perform(post("/api/authors/3/generate-missing/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode done = pollUntilDone(startJob(started));
        assertEquals("SUCCEEDED", done.get("status").asText());
        assertEquals("nationality", done.get("result").get("filledFields").get(0).asText());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void booksFromFeedProcessSingle_start_returnsProcessResult() throws Exception {
        when(booksFromFeedService.processSingleBook(9L)).thenReturn(Map.of("success", true, "bookId", 9L));

        MvcResult started = mockMvc.perform(post("/api/books-from-feed/process-single/9/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode done = pollUntilDone(startJob(started));
        assertEquals("SUCCEEDED", done.get("status").asText());
        assertEquals(true, done.get("result").get("success").asBoolean());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void start_requiresLibrarian() throws Exception {
        mockMvc.perform(post("/api/books/1/book-by-photo/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void unknownJob_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/api/grok-jobs/no-such-job"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(GrokJobController.NOT_FOUND_MESSAGE));
    }

    @Test
    @WithMockUser(username = "someone-else", authorities = "LIBRARIAN")
    void otherUsersJob_returns404() throws Exception {
        // Job started by "someone-else" is visible to them...
        when(bookService.getTitleAuthorFromPhoto(1L)).thenReturn(new BookDto());
        MvcResult started = mockMvc.perform(post("/api/books/1/title-author-from-photo/start"))
                .andExpect(status().isAccepted())
                .andReturn();
        String jobId = startJob(started);
        pollUntilDone(jobId);

        // ...but not to a different user.
        mockMvc.perform(get("/api/grok-jobs/" + jobId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("intruder").authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isNotFound());
    }
}
