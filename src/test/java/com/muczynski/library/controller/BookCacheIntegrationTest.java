/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for book caching endpoints.
 * Tests the /api/books/summaries and /api/books/by-ids endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Library testLibrary;
    private Author testAuthor;
    private Book testBook1;
    private Book testBook2;
    private Book testBook3;

    @BeforeEach
    void setUp() {
        // Create test library
        testLibrary = new Library();
        testLibrary.setBranchName("Test Library");
        testLibrary.setLibrarySystemName("Test Library System");
        testLibrary = branchRepository.save(testLibrary);

        // Create test author
        testAuthor = new Author();
        testAuthor.setName("Test Author");
        testAuthor.setGrokipediaUrl("https://grokipedia.com/page/Test_Author");
        testAuthor = authorRepository.save(testAuthor);

        // Create test books with different lastModified timestamps
        testBook1 = new Book();
        testBook1.setTitle("Test Book 1");
        testBook1.setAuthor(testAuthor);
        testBook1.setLibrary(testLibrary);
        testBook1.setStatus(BookStatus.ACTIVE);
        testBook1.setDateAddedToLibrary(LocalDateTime.of(2025, 1, 1, 10, 0));
        testBook1.setLastModified(LocalDateTime.of(2025, 1, 1, 10, 0));
        testBook1 = bookRepository.save(testBook1);

        testBook2 = new Book();
        testBook2.setTitle("Test Book 2");
        testBook2.setAuthor(testAuthor);
        testBook2.setLibrary(testLibrary);
        testBook2.setStatus(BookStatus.ACTIVE);
        testBook2.setDateAddedToLibrary(LocalDateTime.of(2025, 1, 2, 10, 0));
        testBook2.setLastModified(LocalDateTime.of(2025, 1, 2, 10, 0));
        testBook2 = bookRepository.save(testBook2);

        testBook3 = new Book();
        testBook3.setTitle("Test Book 3");
        testBook3.setAuthor(testAuthor);
        testBook3.setLibrary(testLibrary);
        testBook3.setStatus(BookStatus.ACTIVE);
        testBook3.setDateAddedToLibrary(LocalDateTime.of(2025, 1, 3, 10, 0));
        testBook3.setLastModified(LocalDateTime.of(2025, 1, 3, 10, 0));
        testBook3 = bookRepository.save(testBook3);
    }

    @Test
    @WithMockUser
    void testGetAllBookSummaries() throws Exception {
        mockMvc.perform(get("/api/books/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].id", hasItem(testBook1.getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(testBook2.getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(testBook3.getId().intValue())))
                .andExpect(jsonPath("$[0].lastModified", notNullValue()))
                .andExpect(jsonPath("$[1].lastModified", notNullValue()))
                .andExpect(jsonPath("$[2].lastModified", notNullValue()));
    }

    @Test
    @WithMockUser
    void testGetBooksByIds() throws Exception {
        List<Long> ids = Arrays.asList(testBook1.getId(), testBook2.getId());

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        testBook1.getId().intValue(),
                        testBook2.getId().intValue()
                )))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Test Book 1", "Test Book 2")))
                .andExpect(jsonPath("$[0].author", is("Test Author")))
                .andExpect(jsonPath("$[0].authorGrokipediaUrl", is("https://grokipedia.com/page/Test_Author")))
                .andExpect(jsonPath("$[0].library", is("Test Library")))
                .andExpect(jsonPath("$[0].lastModified", notNullValue()));
    }

    @Test
    @WithMockUser
    void testGetBooksByIdsEmptyList() throws Exception {
        List<Long> ids = Arrays.asList();

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void testGetBooksByIdsSingleId() throws Exception {
        List<Long> ids = Arrays.asList(testBook1.getId());

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testBook1.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is("Test Book 1")));
    }

    @Test
    @WithMockUser
    void testGetBooksByIdsNonExistentIds() throws Exception {
        List<Long> ids = Arrays.asList(99999L, 99998L);

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void testGetBooksByIdsMixedExistentAndNonExistent() throws Exception {
        List<Long> ids = Arrays.asList(testBook1.getId(), 99999L, testBook2.getId());

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        testBook1.getId().intValue(),
                        testBook2.getId().intValue()
                )));
    }

    @Test
    void testGetAllBookSummariesUnauthenticated() throws Exception {
        // The endpoint has permitAll(), so it should work without authentication
        mockMvc.perform(get("/api/books/summaries"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void checkoutMatchesRanksActiveBooksAndStopsAtTen() throws Exception {
        Author melville = new Author();
        melville.setName("Herman Melville");
        melville = authorRepository.save(melville);

        Book exact = saveBook("Moby", melville, BookStatus.ACTIVE, "PS 2384 .M6");
        Book contains = saveBook("Moby Dick", melville, BookStatus.ACTIVE, "");
        saveBook("Withdrawn Moby", melville, BookStatus.WITHDRAWN, "PS 1");
        Book other = saveBook("Oliver Twist", testAuthor, BookStatus.ACTIVE, "PR 4568");

        mockMvc.perform(get("/api/books/checkout-matches").param("title", "moby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(exact.getId().intValue())))
                .andExpect(jsonPath("$[0].author", is("Herman Melville")))
                .andExpect(jsonPath("$[0].locNumber", is("PS 2384 .M6")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[1].id", is(contains.getId().intValue())));

        mockMvc.perform(get("/api/books/checkout-matches").param("locNumber", "PS2384"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(exact.getId().intValue())));

        mockMvc.perform(get("/api/books/checkout-matches")
                        .param("title", "moby")
                        .param("author", "nomatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(exact.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(other.getId().intValue()))));

        mockMvc.perform(get("/api/books/checkout-matches").param("title", "mo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        for (int i = 1; i <= 11; i++) {
            saveBook(String.format("Limit Title %02d", i), testAuthor, BookStatus.ACTIVE, "");
        }
        mockMvc.perform(get("/api/books/checkout-matches").param("title", "limit title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].title", is("Limit Title 01")))
                .andExpect(jsonPath("$[9].title", is("Limit Title 10")));
    }

    @Test
    @WithMockUser
    void byIdsRejectsMoreThan100ForBooksAndAuthors() throws Exception {
        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++) {
            ids.add((long) i + 1);
        }
        String body = objectMapper.writeValueAsString(ids);

        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/authors/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private Book saveBook(String title, Author author, BookStatus status, String locNumber) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setLibrary(testLibrary);
        book.setStatus(status);
        book.setLocNumber(locNumber);
        book.setDateAddedToLibrary(LocalDateTime.of(2025, 1, 4, 10, 0));
        book.setLastModified(LocalDateTime.of(2025, 1, 4, 10, 0));
        return bookRepository.save(book);
    }

    @Test
    void testGetBooksByIdsUnauthenticated() throws Exception {
        List<Long> ids = Arrays.asList(testBook1.getId());

        // The endpoint has permitAll(), so it should work without authentication
        mockMvc.perform(post("/api/books/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk());
    }
}
