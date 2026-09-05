/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.service.AuthorService;
import com.muczynski.library.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unauthenticated visitors can load book/author detail pages and their GET APIs.
 * List and edit URLs stay behind login at the React layer; Spring still serves the SPA.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCatalogAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private AuthorService authorService;

    @Test
    void centurySchoolbookFontIsPublic() throws Exception {
        var result = mockMvc.perform(get("/fonts/CenturySchL-Roma.ttf"))
                .andExpect(status().isOk())
                .andReturn();

        // Long-lived cache for fonts (issue #291): 1 year, public, immutable.
        // This must NOT be applied to HTML or API responses.
        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertNotNull(cacheControl, "Cache-Control header should be present for fonts");
        assertTrue(cacheControl.contains("max-age=31536000") || cacheControl.contains("max-age=365d"),
                "Font should have ~1 year max-age (31536000s)");
        assertTrue(cacheControl.contains("public"), "Font cache should be public");
        assertTrue(cacheControl.contains("immutable"), "Font cache should be immutable");
    }

    @Test
    void spaBookViewIsPublic() throws Exception {
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk());
    }

    @Test
    void spaAuthorViewIsPublic() throws Exception {
        mockMvc.perform(get("/authors/1"))
                .andExpect(status().isOk());
    }

    @Test
    void apiBookByIdIsPublic() throws Exception {
        BookDto book = new BookDto();
        book.setId(1L);
        book.setTitle("Initial Book");
        when(bookService.getBookById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk());
    }

    @Test
    void apiAuthorByIdIsPublic() throws Exception {
        AuthorDto author = new AuthorDto();
        author.setId(1L);
        author.setName("Initial Author");
        when(authorService.getAuthorById(1L)).thenReturn(author);

        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk());
    }
}
