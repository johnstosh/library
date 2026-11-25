/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A related feast day is a month and day. The stored year is only so February 29 fits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = "LIBRARIAN")
class RelatedFeastDayTest {

    private static final String BRANCH = "Feast Day Branch";
    private static final String AUTHOR = "Feast Day Author";
    private static final String CLARE = "Feast Day Clare of Assisi";
    private static final String JOSEPH = "Feast Day Joseph";
    private static final String LEAP = "Feast Day February 29";
    private static final String CLARE_EXPLANATION =
            "August 11 in the West and August 12 in the Maronite Catholic Church.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BranchRepository branchRepository;

    @BeforeEach
    void cleanLeftovers() {
        deleteFeastRows();
    }

    @AfterEach
    void cleanup() {
        deleteFeastRows();
    }

    @Test
    void savedMonthDayReloadsAndSurvivesCloneAndImportExport() throws Exception {
        Library library = new Library();
        library.setBranchName(BRANCH);
        library.setLibrarySystemName("Feast Day System");
        library = branchRepository.save(library);

        Author author = new Author();
        author.setName(AUTHOR);
        author = authorRepository.save(author);

        long clareId = createBook(CLARE, library.getId(), author.getId(), "--08-11", CLARE_EXPLANATION);
        long josephId = createBook(JOSEPH, library.getId(), author.getId(), "--03-19", "");
        long leapId = createBook(LEAP, library.getId(), author.getId(), "--02-29", null);

        assertEquals("date", jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_name = 'book' and column_name = 'related_feast_day'
                """, String.class));
        assertEquals(LocalDate.of(2000, 8, 11), storedDate(clareId));
        assertEquals(LocalDate.of(2000, 3, 19), storedDate(josephId));
        assertEquals(LocalDate.of(2000, 2, 29), storedDate(leapId));

        mockMvc.perform(get("/api/books/" + clareId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatedFeastDay").value("--08-11"))
                .andExpect(jsonPath("$.relatedFeastDayExplanation").value(CLARE_EXPLANATION));
        MvcResult joseph = mockMvc.perform(get("/api/books/" + josephId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatedFeastDay").value("--03-19"))
                .andReturn();
        JsonNode josephJson = objectMapper.readTree(joseph.getResponse().getContentAsString());
        assertTrue(josephJson.get("relatedFeastDayExplanation") == null
                || josephJson.get("relatedFeastDayExplanation").isNull()
                || josephJson.get("relatedFeastDayExplanation").asText().isBlank());
        assertFalse(joseph.getResponse().getContentAsString().contains("2000-03-19"));
        assertFalse(mockMvc.perform(get("/api/books/" + clareId)).andReturn()
                .getResponse().getContentAsString().contains("2000-08-11"));
        mockMvc.perform(get("/api/books/" + leapId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatedFeastDay").value("--02-29"));

        MvcResult cloned = mockMvc.perform(post("/api/books/" + clareId + "/clone"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relatedFeastDay").value("--08-11"))
                .andExpect(jsonPath("$.relatedFeastDayExplanation").value(CLARE_EXPLANATION))
                .andReturn();
        assertFalse(cloned.getResponse().getContentAsString().contains("2000-08-11"));
        long cloneId = objectMapper.readTree(cloned.getResponse().getContentAsString()).get("id").asLong();
        assertEquals(MonthDay.of(8, 11), bookRepository.findById(cloneId).orElseThrow().getRelatedFeastDay());
        assertEquals(CLARE_EXPLANATION, bookRepository.findById(cloneId).orElseThrow().getRelatedFeastDayExplanation());

        MvcResult exported = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();
        String exportJson = exported.getResponse().getContentAsString();
        JsonNode books = objectMapper.readTree(exportJson).get("books");
        JsonNode exportedClare = findBook(books, CLARE);
        JsonNode exportedJoseph = findBook(books, JOSEPH);
        JsonNode exportedLeap = findBook(books, LEAP);
        assertEquals("--08-11", exportedClare.get("relatedFeastDay").asText());
        assertFalse(exportedClare.get("relatedFeastDay").asText().contains("2000"));
        assertEquals(CLARE_EXPLANATION, exportedClare.get("relatedFeastDayExplanation").asText());
        assertEquals("--03-19", exportedJoseph.get("relatedFeastDay").asText());
        assertTrue(exportedJoseph.get("relatedFeastDayExplanation") == null
                || exportedJoseph.get("relatedFeastDayExplanation").isNull()
                || exportedJoseph.get("relatedFeastDayExplanation").asText().isBlank());
        assertEquals("--02-29", exportedLeap.get("relatedFeastDay").asText());

        jdbc.update("""
                update book set related_feast_day = null, related_feast_day_explanation = null
                where title like 'Feast Day %'
                """);
        MvcResult imported = mockMvc.perform(post("/api/import/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportJson))
                .andReturn();
        assertEquals(200, imported.getResponse().getStatus(), imported.getResponse().getContentAsString());

        Book reloadedClare = bookRepository.findAllByTitleOrderByIdAsc(CLARE).get(0);
        Book reloadedJoseph = bookRepository.findAllByTitleOrderByIdAsc(JOSEPH).get(0);
        Book reloadedLeap = bookRepository.findAllByTitleOrderByIdAsc(LEAP).get(0);
        assertEquals(MonthDay.of(8, 11), reloadedClare.getRelatedFeastDay());
        assertEquals(CLARE_EXPLANATION, reloadedClare.getRelatedFeastDayExplanation());
        assertEquals(MonthDay.of(3, 19), reloadedJoseph.getRelatedFeastDay());
        assertNull(reloadedJoseph.getRelatedFeastDayExplanation());
        assertEquals(MonthDay.of(2, 29), reloadedLeap.getRelatedFeastDay());
        assertEquals(LocalDate.of(2000, 2, 29), storedDate(reloadedLeap.getId()));
    }

    private long createBook(String title, long libraryId, long authorId, String feastDay, String explanation)
            throws Exception {
        String explanationJson = explanation == null
                ? ""
                : ",\"relatedFeastDayExplanation\":" + objectMapper.writeValueAsString(explanation);
        String body = """
                {"title":"%s","authorId":%d,"libraryId":%d,"status":"ACTIVE","relatedFeastDay":"%s"%s}
                """.formatted(title, authorId, libraryId, feastDay, explanationJson);
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("2000-" + feastDay.substring(2)), json);
        return objectMapper.readTree(json).get("id").asLong();
    }

    private LocalDate storedDate(long bookId) {
        return jdbc.queryForObject("select related_feast_day from book where id = ?", LocalDate.class, bookId);
    }

    private static JsonNode findBook(JsonNode books, String title) {
        for (JsonNode book : books) {
            if (title.equals(book.path("title").asText())) {
                return book;
            }
        }
        throw new AssertionError("Missing exported book " + title);
    }

    private void deleteFeastRows() {
        List<Long> ids = jdbc.queryForList("select id from book where title like 'Feast Day %'", Long.class);
        if (!ids.isEmpty()) {
            String in = ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
            jdbc.update("delete from book_tags where book_id in (" + in + ")");
            jdbc.update("delete from book where id in (" + in + ")");
        }
        jdbc.update("delete from author where name = ?", AUTHOR);
        jdbc.update("delete from library where name = ?", BRANCH);
    }
}
