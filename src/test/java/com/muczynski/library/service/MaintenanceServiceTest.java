/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.GenreChangeLineDto;
import com.muczynski.library.dto.IllegalGenresMaintenanceDto;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MaintenanceServiceTest {

    @Autowired
    private MaintenanceService service;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aRemovedTagIsNotAlsoASpellingCorrection() {
        MaintenanceService.EditCounts removed = MaintenanceService.editCounts(List.of("fiction", "devotional"));
        assertEquals(0, removed.corrections);
        assertEquals(1, removed.removed);

        MaintenanceService.EditCounts renamed = MaintenanceService.editCounts(List.of("children"));
        assertEquals(1, renamed.corrections);
        assertEquals(0, renamed.removed);
    }

    @Test
    void recalcListsDropsBeforeRenamesAndNamesOnlySmallGroups() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        String illegal = "zzz-" + token;
        bookRepository.save(book("GenreDrop A " + token, author, List.of(illegal)));
        bookRepository.save(book("GenreDrop B " + token, author, List.of(illegal)));
        bookRepository.save(book("GenreDrop C " + token, author, List.of(illegal)));
        bookRepository.save(book("GenreDrop D " + token, author, List.of(illegal)));
        bookRepository.save(book("GenreAlpha " + token, author, List.of("read-alouds")));
        bookRepository.save(book("GenreBeta " + token, author, List.of("read-alouds")));

        IllegalGenresMaintenanceDto preview = service.countIllegalGenres();
        GenreChangeLineDto drop = line(preview, "drop " + illegal);
        GenreChangeLineDto rename = line(preview, "get read-alouds changed to read-aloud");

        assertTrue(preview.getChanges().indexOf(drop) < preview.getChanges().indexOf(rename));
        assertEquals("4 titles drop " + illegal, drop.getSummary());
        assertFalse(drop.getSummary().contains("GenreDrop A"));
        assertEquals("GenreAlpha " + token + " and GenreBeta " + token + " get read-alouds changed to read-aloud",
                rename.getSummary());
        assertTrue(preview.getBooksAffected() >= 6);
    }

    @Test
    void cleanupAndUndoRestoreThePreviousGenres() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book renamed = bookRepository.save(book("GenreAlpha " + token, author, List.of("children")));
        Book dropped = bookRepository.save(book("GenreDrop A " + token, author, List.of("zzz-" + token)));
        Book kept = bookRepository.save(book("GenreReady " + token, author, List.of("fiction")));
        String illegal = "zzz-" + token;
        Book mixed = bookRepository.save(book("GenreMixed " + token, author, List.of("fiction", illegal)));
        MaintenanceService.EditCounts expected = expectedEdits();

        IllegalGenresMaintenanceDto cleaned = service.cleanupIllegalGenres();
        assertNotNull(cleaned.getActionId());
        assertEquals(bookRepository.count(), cleaned.getBooksScanned());
        assertTrue(cleaned.getMessage().startsWith("Looked at " + cleaned.getBooksScanned() + " books."));
        assertEquals(expected.corrections, cleaned.getPluralCorrections());
        assertEquals(expected.removed, cleaned.getIllegalRemoved());
        assertEquals(List.of("childrens"), bookRepository.findById(renamed.getId()).orElseThrow().getTagsList());
        assertEquals(List.of(), bookRepository.findById(dropped.getId()).orElseThrow().getTagsList());
        assertEquals(List.of("fiction"), bookRepository.findById(kept.getId()).orElseThrow().getTagsList());
        assertEquals(List.of("fiction"), bookRepository.findById(mixed.getId()).orElseThrow().getTagsList());

        Book stale = bookRepository.findById(renamed.getId()).orElseThrow();
        stale.setTagsList(new ArrayList<>(List.of("history")));
        bookRepository.save(stale);

        IllegalGenresMaintenanceDto undone = service.undoGenreCleanup(cleaned.getActionId());
        assertTrue(undone.getMessage().contains("left alone"));
        assertEquals(List.of("history"), bookRepository.findById(renamed.getId()).orElseThrow().getTagsList());
        assertEquals(List.of("zzz-" + token), bookRepository.findById(dropped.getId()).orElseThrow().getTagsList());

        IllegalGenresMaintenanceDto again = service.undoGenreCleanup(cleaned.getActionId());
        assertEquals("Already undone.", again.getError());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void genreUndoRequiresLibrarian() throws Exception {
        mockMvc.perform(post("/api/maintenance/illegal-genres/undo")
                        .contentType("application/json")
                        .content("{\"actionId\":1}"))
                .andExpect(status().isForbidden());
    }

    private MaintenanceService.EditCounts expectedEdits() {
        long corrections = 0;
        long removed = 0;
        Map<Long, List<String>> tags = new LinkedHashMap<>();
        for (Object[] row : bookRepository.findGenreMaintenanceRows()) {
            Long id = ((Number) row[0]).longValue();
            tags.computeIfAbsent(id, ignored -> new ArrayList<>());
            if (row[2] != null) {
                tags.get(id).add(row[2].toString());
            }
        }
        for (List<String> bookTags : tags.values()) {
            MaintenanceService.EditCounts counts = MaintenanceService.editCounts(bookTags);
            corrections += counts.corrections;
            removed += counts.removed;
        }
        return new MaintenanceService.EditCounts(corrections, removed);
    }

    private static GenreChangeLineDto line(IllegalGenresMaintenanceDto preview, String fragment) {
        return preview.getChanges().stream()
                .filter(row -> row.getSummary() != null && row.getSummary().contains(fragment))
                .findFirst()
                .orElseThrow();
    }

    private static String token() {
        return "Genre" + UUID.randomUUID().toString().replace("-", "");
    }

    private static Author author(String name) {
        Author author = new Author();
        author.setName(name);
        return author;
    }

    private static Book book(String title, Author author, List<String> tags) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book.setTagsList(new ArrayList<>(tags));
        return book;
    }
}
