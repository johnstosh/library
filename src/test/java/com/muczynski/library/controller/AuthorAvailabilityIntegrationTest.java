/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthorAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @WithMockUser
    void getAuthorAvailability_rollsUpYdlAndEmuFlags() throws Exception {
        Author ydlAuthor = new Author();
        ydlAuthor.setName("Availability YDL Author");
        ydlAuthor = authorRepository.save(ydlAuthor);

        Author emuAuthor = new Author();
        emuAuthor.setName("Availability EMU Author");
        emuAuthor = authorRepository.save(emuAuthor);

        Author noneAuthor = new Author();
        noneAuthor.setName("Availability None Author");
        noneAuthor = authorRepository.save(noneAuthor);

        Book ydlBook = new Book();
        ydlBook.setTitle("YDL Paper and Audio");
        ydlBook.setAuthor(ydlAuthor);
        ydlBook.setStatus(BookStatus.ACTIVE);
        ydlBook.setDateAddedToLibrary(LocalDateTime.now());
        ydlBook.setYdlPaperAvailable(true);
        ydlBook.setYdlEbookAvailable(false);
        ydlBook.setYdlAudioAvailable(true);
        bookRepository.save(ydlBook);

        Book emuBook = new Book();
        emuBook.setTitle("EMU Ebook");
        emuBook.setAuthor(emuAuthor);
        emuBook.setStatus(BookStatus.ACTIVE);
        emuBook.setDateAddedToLibrary(LocalDateTime.now());
        emuBook.setEmuEbookAvailable(true);
        bookRepository.save(emuBook);

        Book noneBook = new Book();
        noneBook.setTitle("No Holdings");
        noneBook.setAuthor(noneAuthor);
        noneBook.setStatus(BookStatus.ACTIVE);
        noneBook.setDateAddedToLibrary(LocalDateTime.now());
        noneBook.setYdlPaperAvailable(false);
        bookRepository.save(noneBook);

        mockMvc.perform(get("/api/authors/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.authorId == " + ydlAuthor.getId() + ")].hasYdlBook").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.authorId == " + ydlAuthor.getId() + ")].hasYdlEbook").value(hasItem(false)))
                .andExpect(jsonPath("$[?(@.authorId == " + ydlAuthor.getId() + ")].hasYdlAudio").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.authorId == " + emuAuthor.getId() + ")].hasEmuEbook").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.authorId == " + noneAuthor.getId() + ")]").isEmpty());
    }
}
