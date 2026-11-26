/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.MainCharacterSex;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A book's main character is male, female, or blank.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(authorities = "LIBRARIAN")
class MainCharacterSexBookTest {

    private static final String MALE_TITLE = "Main Character Male Story";
    private static final String BLANK_TITLE = "Main Character Blank Story";

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
        deleteRows();
    }

    @AfterEach
    void cleanup() {
        deleteRows();
    }

    @Test
    void maleFemaleAndBlankRoundTripThroughSaveCloneAndImport() throws Exception {
        Library library = new Library();
        library.setBranchName("Main Character Branch");
        library.setLibrarySystemName("Main Character System");
        library = branchRepository.save(library);

        Author author = new Author();
        author.setName("Main Character Author");
        author = authorRepository.save(author);

        long maleId = create(MALE_TITLE, library.getId(), author.getId(), "MALE");
        long blankId = create(BLANK_TITLE, library.getId(), author.getId(), null);

        assertEquals("MALE", stored(maleId));
        assertNull(stored(blankId));
        mockMvc.perform(get("/api/books/" + maleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainCharacterSex").value("MALE"));
        MvcResult blank = mockMvc.perform(get("/api/books/" + blankId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode blankJson = objectMapper.readTree(blank.getResponse().getContentAsString());
        assertTrue(blankJson.get("mainCharacterSex") == null || blankJson.get("mainCharacterSex").isNull());

        MvcResult cleared = mockMvc.perform(put("/api/books/" + maleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(MALE_TITLE, library.getId(), author.getId(), "")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode clearedJson = objectMapper.readTree(cleared.getResponse().getContentAsString());
        assertTrue(clearedJson.get("mainCharacterSex") == null || clearedJson.get("mainCharacterSex").isNull());
        assertNull(stored(maleId));

        mockMvc.perform(put("/api/books/" + maleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(MALE_TITLE, library.getId(), author.getId(), "female")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainCharacterSex").value("FEMALE"));
        assertEquals(MainCharacterSex.FEMALE, bookRepository.findById(maleId).orElseThrow().getMainCharacterSex());

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Main Character Rejected", library.getId(), author.getId(), "both")))
                .andExpect(status().isBadRequest());

        MvcResult cloned = mockMvc.perform(post("/api/books/" + maleId + "/clone"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mainCharacterSex").value("FEMALE"))
                .andReturn();
        long cloneId = objectMapper.readTree(cloned.getResponse().getContentAsString()).get("id").asLong();
        assertEquals(MainCharacterSex.FEMALE, bookRepository.findById(cloneId).orElseThrow().getMainCharacterSex());

        MvcResult exported = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();
        String exportJson = exported.getResponse().getContentAsString();
        JsonNode books = objectMapper.readTree(exportJson).get("books");
        assertEquals("FEMALE", find(books, MALE_TITLE).get("mainCharacterSex").asText());
        JsonNode exportedBlank = find(books, BLANK_TITLE);
        assertTrue(exportedBlank.get("mainCharacterSex") == null || exportedBlank.get("mainCharacterSex").isNull());

        jdbc.update("update book set main_character_sex = null where title like 'Main Character %'");
        MvcResult imported = mockMvc.perform(post("/api/import/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportJson))
                .andReturn();
        assertEquals(200, imported.getResponse().getStatus(), imported.getResponse().getContentAsString());
        assertEquals(MainCharacterSex.FEMALE,
                bookRepository.findAllByTitleOrderByIdAsc(MALE_TITLE).get(0).getMainCharacterSex());
        assertNull(bookRepository.findAllByTitleOrderByIdAsc(BLANK_TITLE).get(0).getMainCharacterSex());
    }

    private long create(String title, long libraryId, long authorId, String sex) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(title, libraryId, authorId, sex)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String body(String title, long libraryId, long authorId, String sex) {
        String sexJson = sex == null ? "" : ",\"mainCharacterSex\":" + json(sex);
        return """
                {"title":"%s","authorId":%d,"libraryId":%d,"status":"ACTIVE"%s}
                """.formatted(title, authorId, libraryId, sexJson);
    }

    private String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String stored(long bookId) {
        return jdbc.queryForObject(
                "select main_character_sex from book where id = ?", String.class, bookId);
    }

    private static JsonNode find(JsonNode books, String title) {
        for (JsonNode book : books) {
            if (title.equals(book.path("title").asText())) {
                return book;
            }
        }
        throw new AssertionError("Missing " + title);
    }

    private void deleteRows() {
        java.util.List<Long> ids = jdbc.queryForList(
                "select id from book where title like 'Main Character %'", Long.class);
        if (!ids.isEmpty()) {
            String in = ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
            jdbc.update("delete from book_tags where book_id in (" + in + ")");
            jdbc.update("delete from book where id in (" + in + ")");
        }
        jdbc.update("delete from author where name = 'Main Character Author'");
        jdbc.update("delete from library where name = 'Main Character Branch'");
    }
}
