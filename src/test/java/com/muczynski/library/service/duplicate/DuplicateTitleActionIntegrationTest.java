/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.DuplicateTitleActionResultDto;
import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.DuplicateTitleActionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Duplicate Titles merges and fixes record an append-only action row and can be undone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DuplicateTitleActionIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private DuplicateTitleActionRepository actionRepository;

    @Autowired
    private DuplicateTitleService duplicateTitleService;

    @Autowired
    private DuplicateTitleActionService actionService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void findLabelsACaseInsensitiveTitle() {
        Author augustine = author("Augustine");
        Author augustin = author("Augustin");
        Book left = book("Confessions", BookStatus.ACTIVE, augustine);
        Book right = book("confessions", BookStatus.LOST, augustin);
        bookRepository.flush();

        DuplicateTitlePairDto pair = pairOf(left.getId(), right.getId());
        assertEquals("Titles match case-insensitively.", pair.getTitleDifference());
    }

    @Test
    void mergeAuthorsKeepsTheFullerNameAndUndoRestoresTheDeletedAuthor() {
        Author shorter = author("John Smith");
        Author fuller = author("John Paul Smith");
        Book onShorter = book("City of God", BookStatus.ACTIVE, shorter);
        Book onFuller = book("The City of God", BookStatus.ACTIVE, fuller);
        bookRepository.flush();
        long before = actionRepository.count();

        DuplicateTitleActionResultDto merged = actionService.mergeAuthors(onShorter.getId(), onFuller.getId());
        refresh();

        assertFalse(authorRepository.existsById(shorter.getId()));
        Author kept = authorRepository.findById(fuller.getId()).orElseThrow();
        assertEquals("John Paul Smith", kept.getName());
        assertTrue(kept.getAlternateNames().contains("John Smith"));
        assertEquals(fuller.getId(), bookRepository.findById(onShorter.getId()).orElseThrow().getAuthor().getId());
        assertEquals(before + 1, actionRepository.count());

        actionService.undoAuthorMerge(merged.getActionId());
        refresh();

        assertEquals(before + 2, actionRepository.count());
        assertTrue(authorRepository.existsById(shorter.getId()));
        assertEquals("John Smith", authorRepository.findById(shorter.getId()).orElseThrow().getName());
        assertEquals(shorter.getId(), bookRepository.findById(onShorter.getId()).orElseThrow().getAuthor().getId());
        assertEquals(fuller.getId(), bookRepository.findById(onFuller.getId()).orElseThrow().getAuthor().getId());
        assertThrows(LibraryException.class, () -> actionService.undoAuthorMerge(merged.getActionId()));
        assertEquals(before + 2, actionRepository.count());
    }

    @Test
    void mergeTitlesKeepsTheCallNumberAndUndoRestoresTheDeletedBook() {
        Author augustine = author("Augustine");
        Book shelf = book("Confessions", BookStatus.ACTIVE, augustine);
        shelf.setLocNumber("BR65 .A9");
        shelf.setFreeTextUrl("https://shelf.example/a");
        shelf.setElectronicResource(false);
        Book electronic = book("Confession", BookStatus.ACTIVE, augustine);
        electronic.setElectronicResource(true);
        electronic.setFreeTextUrl("https://electronic.example/b");
        bookRepository.saveAll(java.util.List.of(shelf, electronic));
        bookRepository.flush();

        DuplicateTitleActionResultDto merged = actionService.mergeTitles(shelf.getId(), electronic.getId());
        refresh();

        assertFalse(bookRepository.existsById(electronic.getId()));
        Book kept = bookRepository.findById(shelf.getId()).orElseThrow();
        assertEquals("BR65 .A9", kept.getLocNumber());
        assertEquals("https://shelf.example/a https://electronic.example/b", kept.getFreeTextUrl());
        assertTrue(Boolean.TRUE.equals(kept.getElectronicResource()));
        assertTrue(merged.getSummary().contains("Call number"));

        actionService.undoTitleMerge(merged.getActionId());
        refresh();

        Book restored = bookRepository.findById(electronic.getId()).orElseThrow();
        assertEquals("Confession", restored.getTitle());
        assertEquals("https://electronic.example/b", restored.getFreeTextUrl());
        Book shelfAgain = bookRepository.findById(shelf.getId()).orElseThrow();
        assertEquals("https://shelf.example/a", shelfAgain.getFreeTextUrl());
        assertEquals("BR65 .A9", shelfAgain.getLocNumber());
        assertFalse(Boolean.TRUE.equals(shelfAgain.getElectronicResource()));
    }

    @Test
    void bothCallNumbersAreFixedInsteadOfMergedAndUndoRestoresTitles() {
        Author augustine = author("Augustine Hippo");
        Book first = book("Confessions", BookStatus.ACTIVE, augustine);
        first.setLocNumber("BR65");
        Book second = book("Confession", BookStatus.ACTIVE, augustine);
        second.setLocNumber("BR65.A");
        bookRepository.saveAll(java.util.List.of(first, second));
        bookRepository.flush();

        LibraryException rejected = assertThrows(LibraryException.class,
                () -> actionService.mergeTitles(first.getId(), second.getId()));
        assertTrue(rejected.getMessage().contains("Fix titles"));

        DuplicateTitleActionResultDto fixed = actionService.fixTitles(first.getId(), second.getId());
        refresh();

        Book lower = bookRepository.findById(first.getId()).orElseThrow();
        Book higher = bookRepository.findById(second.getId()).orElseThrow();
        assertEquals("Confessions", lower.getTitle());
        assertEquals("Confession, c. 2", higher.getTitle());
        assertEquals("Confession", higher.getAlternateTitle());
        assertEquals("BR65", lower.getLocNumber());
        assertEquals("BR65.A", higher.getLocNumber());
        assertTrue(fixed.getSummary().contains("Call number"));

        actionService.undoFixTitles(fixed.getActionId());
        refresh();

        assertEquals("Confessions", bookRepository.findById(first.getId()).orElseThrow().getTitle());
        assertEquals("Confession", bookRepository.findById(second.getId()).orElseThrow().getTitle());
        assertEquals(null, bookRepository.findById(second.getId()).orElseThrow().getAlternateTitle());
    }

    @Test
    void fixTitlesSkipsCopyNumbersTheCatalogAlreadyUses() {
        Author augustine = author("Augustine");
        Author augustin = author("Augustin");
        Book first = book("Confessions", BookStatus.ACTIVE, augustine);
        first.setLocNumber("BR65");
        Book copy2 = book("Confessions, c. 2", BookStatus.ACTIVE, augustine);
        Book copy3 = book("Confessions, c. 3", BookStatus.ACTIVE, augustine);
        Book copy4 = book("Confessions, c. 4", BookStatus.ACTIVE, augustine);
        Book near = book("confessions", BookStatus.ACTIVE, augustin);
        near.setLocNumber("BR65.A");
        bookRepository.flush();

        DuplicateTitlePairDto pair = pairOf(first.getId(), near.getId());
        assertTrue(pair.getTitlePlan().contains("Confessions, c. 5"), pair.getTitlePlan());
        assertFalse(pair.getTitlePlan().contains(", c. 2"), pair.getTitlePlan());

        actionService.fixTitles(first.getId(), near.getId());
        refresh();

        assertEquals("Confessions", bookRepository.findById(first.getId()).orElseThrow().getTitle());
        assertEquals("Confessions, c. 5", bookRepository.findById(near.getId()).orElseThrow().getTitle());
        assertEquals("confessions", bookRepository.findById(near.getId()).orElseThrow().getAlternateTitle());
        assertEquals("Confessions, c. 2", bookRepository.findById(copy2.getId()).orElseThrow().getTitle());
        assertEquals("Confessions, c. 3", bookRepository.findById(copy3.getId()).orElseThrow().getTitle());
        assertEquals("Confessions, c. 4", bookRepository.findById(copy4.getId()).orElseThrow().getTitle());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void userCannotMergeAuthors() throws Exception {
        mockMvc.perform(post("/api/maintenance/duplicate-titles/author-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookAId\":1,\"bookBId\":2}"))
                .andExpect(status().isForbidden());
    }

    private DuplicateTitlePairDto pairOf(Long leftId, Long rightId) {
        Set<Long> expected = Set.of(leftId, rightId);
        return duplicateTitleService.findDuplicates().getPairs().stream()
                .filter(pair -> expected.equals(Set.of(pair.getBookAId(), pair.getBookBId())))
                .findFirst()
                .orElseThrow();
    }

    private void refresh() {
        entityManager.flush();
        entityManager.clear();
    }

    private Author author(String name) {
        Author author = new Author();
        author.setName(name);
        return authorRepository.save(author);
    }

    private Book book(String title, BookStatus status, Author author) {
        Book book = new Book();
        book.setTitle(title);
        book.setStatus(status);
        book.setAuthor(author);
        return bookRepository.save(book);
    }
}
