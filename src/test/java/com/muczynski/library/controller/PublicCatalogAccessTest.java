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
        mockMvc.perform(get("/fonts/CenturySchL-Roma.ttf"))
                .andExpect(status().isOk());
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
