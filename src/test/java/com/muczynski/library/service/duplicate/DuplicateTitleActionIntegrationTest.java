/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.PublicLibrary;
import com.muczynski.library.domain.RejectedHolding;
import com.muczynski.library.domain.RejectedUrl;
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

import com.muczynski.library.domain.DuplicateTitleAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        shelf.setAlternateTitles(List.of("The Confessions"));
        Book electronic = book("Confession", BookStatus.ACTIVE, augustine);
        electronic.setElectronicResource(true);
        electronic.setFreeTextUrl("https://electronic.example/b");
        LocalDateTime searched = LocalDateTime.of(2024, 3, 4, 5, 6);
        electronic.setFreeTextLastSearched(searched);
        electronic.setAlternateTitles(List.of("Confessiones"));
        bookRepository.saveAll(java.util.List.of(shelf, electronic));
        bookRepository.flush();

        DuplicateTitleActionResultDto merged = actionService.mergeTitles(shelf.getId(), electronic.getId());
        refresh();

        assertFalse(bookRepository.existsById(electronic.getId()));
        Book kept = bookRepository.findById(shelf.getId()).orElseThrow();
        assertEquals("BR65 .A9", kept.getLocNumber());
        assertEquals("https://shelf.example/a https://electronic.example/b", kept.getFreeTextUrl());
        assertEquals(List.of("The Confessions", "Confessiones", "Confession"), kept.getAlternateTitles());
        assertTrue(Boolean.TRUE.equals(kept.getElectronicResource()));
        assertTrue(merged.getSummary().contains("Call number"));

        actionService.undoTitleMerge(merged.getActionId());
        refresh();

        Book restored = bookRepository.findById(electronic.getId()).orElseThrow();
        assertEquals("Confession", restored.getTitle());
        assertEquals("https://electronic.example/b", restored.getFreeTextUrl());
        assertEquals(searched, restored.getFreeTextLastSearched());
        Book shelfAgain = bookRepository.findById(shelf.getId()).orElseThrow();
        assertEquals("https://shelf.example/a", shelfAgain.getFreeTextUrl());
        assertEquals("BR65 .A9", shelfAgain.getLocNumber());
        assertEquals(List.of("The Confessions"), shelfAgain.getAlternateTitles());
        assertEquals(List.of("Confessiones"), restored.getAlternateTitles());
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
        assertEquals(null, higher.getAlternateTitles());
        assertEquals("BR65", lower.getLocNumber());
        assertEquals("BR65.A", higher.getLocNumber());
        assertTrue(fixed.getSummary().contains("Call number"));

        actionService.undoFixTitles(fixed.getActionId());
        refresh();

        assertEquals("Confessions", bookRepository.findById(first.getId()).orElseThrow().getTitle());
        assertEquals("Confession", bookRepository.findById(second.getId()).orElseThrow().getTitle());
        assertEquals(null, bookRepository.findById(second.getId()).orElseThrow().getAlternateTitles());
    }

    @Test
    void undoFixTitlesReadsAnOldStringAlternate() {
        Author augustine = author("Francis de Sales");
        Book first = book("AltArray Snapshot Work", BookStatus.ACTIVE, augustine);
        first.setLocNumber("BV4817");
        Book second = book("AltArray Snapshot Work, c. 2", BookStatus.ACTIVE, augustine);
        second.setLocNumber("BV4817.A");
        second.setAlternateTitles(List.of("Confession"));
        bookRepository.saveAll(List.of(first, second));
        bookRepository.flush();

        DuplicateTitleAction row = new DuplicateTitleAction();
        row.setActionKind(DuplicateTitleAction.FIX_TITLES);
        row.setBookAId(first.getId());
        row.setBookBId(second.getId());
        row.setSummary("Old snapshot stored one alternate title as a string.");
        row.setSnapshotJson("""
                {"kind":"FIX_TITLES","fixBookAId":%d,"fixBookATitle":"AltArray Snapshot Work","fixBookAAlternate":null,"fixBookBId":%d,"fixBookBTitle":"AltArray Snapshot Other","fixBookBAlternate":"On Loving God"}
                """.formatted(first.getId(), second.getId()));
        actionRepository.saveAndFlush(row);

        actionService.undoFixTitles(row.getId());
        refresh();

        assertEquals("AltArray Snapshot Work", bookRepository.findById(first.getId()).orElseThrow().getTitle());
        Book restored = bookRepository.findById(second.getId()).orElseThrow();
        assertEquals("AltArray Snapshot Other", restored.getTitle());
        assertEquals(List.of("On Loving God"), restored.getAlternateTitles());
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
        assertEquals(null, bookRepository.findById(near.getId()).orElseThrow().getAlternateTitles());
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

    @Test
    void mergeTitlesUnionsRejectedMatchesAndUndoRestoresThem() {
        Author augustine = author("Augustine of Hippo Rejected");
        Book shelf = book("Confessions Rejected Shelf", BookStatus.ACTIVE, augustine);
        shelf.setFreeTextUrl("https://gutenberg.org/1 https://archive.org/ok");
        shelf.setInvalidFreeTextUrls(List.of(new RejectedUrl(
                "https://gutenberg.org/1", "wrong edition", null, "ada")));
        shelf.setInvalidHoldings(List.of(new RejectedHolding(
                PublicLibrary.YDL, "shelf-1", null, "wrong book", null, "ada")));
        Book electronic = book("Confession Rejected Copy", BookStatus.ACTIVE, augustine);
        electronic.setFreeTextUrl("https://librivox.org/city");
        electronic.setInvalidFreeAudioUrls(List.of(new RejectedUrl(
                "https://librivox.org/city", "wrong reading", null, "bea")));
        electronic.setInvalidGrokipediaUrls(List.of(new RejectedUrl(
                "https://grokipedia.com/page/Other", "different book", null, "bea")));
        electronic.setInvalidHoldings(List.of(new RejectedHolding(
                PublicLibrary.EMU, "emu-1", null, "not this copy", null, "bea")));
        bookRepository.saveAll(List.of(shelf, electronic));
        bookRepository.flush();

        DuplicateTitleActionResultDto merged = actionService.mergeTitles(shelf.getId(), electronic.getId());
        refresh();

        Book kept = bookRepository.findById(shelf.getId()).orElseThrow();
        assertEquals("https://archive.org/ok", kept.getFreeTextUrl());
        assertEquals(1, kept.getInvalidFreeTextUrls().size());
        assertEquals(1, kept.getInvalidFreeAudioUrls().size());
        assertEquals(1, kept.getInvalidGrokipediaUrls().size());
        assertEquals(2, kept.getInvalidHoldings().size());

        actionService.undoTitleMerge(merged.getActionId());
        refresh();

        Book restored = bookRepository.findById(electronic.getId()).orElseThrow();
        assertEquals("https://librivox.org/city", restored.getFreeTextUrl());
        assertEquals("emu-1", restored.getInvalidHoldings().get(0).getRecordKey());
        assertEquals(1, restored.getInvalidFreeAudioUrls().size());
        Book shelfAgain = bookRepository.findById(shelf.getId()).orElseThrow();
        assertEquals("https://gutenberg.org/1 https://archive.org/ok", shelfAgain.getFreeTextUrl());
        assertEquals(1, shelfAgain.getInvalidFreeTextUrls().size());
        assertNull(shelfAgain.getInvalidFreeAudioUrls());
    }

    @Test
    void mergeAuthorsUnionsRejectedPagesAndUndoRestoresThem() {
        Author keeperAuthor = author("Ada Lovelace Smith");
        keeperAuthor.setInvalidGrokipediaUrls(List.of(new RejectedUrl(
                "https://grokipedia.com/page/Ada", "keeper", null, "ada")));
        authorRepository.save(keeperAuthor);
        Author sourceAuthor = author("Beatrice Other");
        sourceAuthor.setInvalidGrokipediaUrls(List.of(new RejectedUrl(
                "https://grokipedia.com/page/Other", "source", null, "bea")));
        authorRepository.save(sourceAuthor);
        Book onKeeper = book("Ada Rejected One", BookStatus.ACTIVE, keeperAuthor);
        Book onSource = book("Ada Rejected Two", BookStatus.ACTIVE, sourceAuthor);
        bookRepository.flush();

        DuplicateTitleActionResultDto merged = actionService.mergeAuthors(onKeeper.getId(), onSource.getId());
        refresh();

        Author kept = authorRepository.findById(keeperAuthor.getId()).orElseThrow();
        assertEquals(2, kept.getInvalidGrokipediaUrls().size());
        assertEquals("keeper", kept.getInvalidGrokipediaUrls().get(0).getReason());

        actionService.undoAuthorMerge(merged.getActionId());
        refresh();

        assertEquals("keeper", authorRepository.findById(keeperAuthor.getId()).orElseThrow()
                .getInvalidGrokipediaUrls().get(0).getReason());
        assertEquals(1, authorRepository.findById(keeperAuthor.getId()).orElseThrow()
                .getInvalidGrokipediaUrls().size());
        assertEquals("source", authorRepository.findById(sourceAuthor.getId()).orElseThrow()
                .getInvalidGrokipediaUrls().get(0).getReason());
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
