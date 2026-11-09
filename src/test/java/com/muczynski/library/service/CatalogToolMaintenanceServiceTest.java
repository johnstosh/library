/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.CatalogToolApplyResultDto;
import com.muczynski.library.dto.CatalogToolCandidatesDto;
import com.muczynski.library.dto.CatalogToolChangeDto;
import com.muczynski.library.dto.CatalogToolPreviewDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.util.ChicagoTitleCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogToolMaintenanceServiceTest {

    @Autowired
    private CatalogToolMaintenanceService service;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chicagoPreviewListsEveryTitleThatWillChangeAndSkipsATakenTitle() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book lower = bookRepository.save(book("the history of " + token, author));
        Book ready = bookRepository.save(book("Ready " + token, author));
        Book withdrawn = book("a withdrawn " + token, author);
        withdrawn.setStatus(BookStatus.WITHDRAWN);
        withdrawn = bookRepository.save(withdrawn);
        Book taken = bookRepository.save(book(ChicagoTitleCase.toChicago("the taken " + token), author));
        Book collision = bookRepository.save(book("the taken " + token, author));

        CatalogToolPreviewDto preview = service.previewChicago();

        assertTrue(changeIds(preview).contains(lower.getId()));
        assertTrue(changeIds(preview).contains(withdrawn.getId()));
        assertFalse(changeIds(preview).contains(ready.getId()));
        assertFalse(changeIds(preview).contains(taken.getId()));
        assertFalse(changeIds(preview).contains(collision.getId()));
        CatalogToolChangeDto lowerChange = change(preview, lower.getId());
        assertEquals("the history of " + token, lowerChange.getBefore());
        assertEquals(ChicagoTitleCase.toChicago("the history of " + token), lowerChange.getAfter());
        assertTrue(preview.getSkipped().stream().anyMatch(row -> collision.getId().equals(row.getBookId())));
        assertTrue(preview.getMessage().contains("title"));
    }

    @Test
    void chicagoApplyWritesThePreviewAndUndoRestoresIt() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book lower = bookRepository.save(book("the history of " + token, author));
        String before = lower.getTitle();
        String after = ChicagoTitleCase.toChicago(before);

        CatalogToolApplyResultDto applied = service.applyChicago(List.of(change(lower.getId(), before, before, after)));

        assertEquals(1, applied.getApplied());
        assertNotNull(applied.getActionId());
        assertEquals(after, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
        assertEquals(applied.getActionId(), service.undoableChicago().getActionId());

        CatalogToolApplyResultDto undone = service.undoChicago(applied.getActionId());
        assertEquals(1, undone.getApplied());
        assertEquals(before, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
        assertNull(service.undoableChicago().getActionId());

        CatalogToolApplyResultDto again = service.undoChicago(applied.getActionId());
        assertEquals("Already undone.", again.getError());
        assertEquals(before, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
    }

    @Test
    void chicagoApplySkipsATitleThatChangedSinceRecalc() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book lower = bookRepository.save(book("edited " + token, author));

        CatalogToolApplyResultDto applied = service.applyChicago(List.of(
                change(lower.getId(), "the history of " + token, "the history of " + token,
                        ChicagoTitleCase.toChicago("the history of " + token))));

        assertEquals(0, applied.getApplied());
        assertNull(applied.getActionId());
        assertEquals("edited " + token, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
    }

    @Test
    void chicagoUndoLeavesATitleWhoseOldWordingIsNowTaken() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        String before = "the history of " + token;
        Book lower = bookRepository.save(book(before, author));
        String after = ChicagoTitleCase.toChicago(before);
        CatalogToolApplyResultDto applied = service.applyChicago(List.of(change(lower.getId(), before, before, after)));
        bookRepository.save(book(before, author));

        CatalogToolApplyResultDto undone = service.undoChicago(applied.getActionId());

        assertEquals(0, undone.getApplied());
        assertTrue(undone.getSkippedChanges().get(0).getReason().contains("already used"));
        assertEquals(after, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
        assertEquals(applied.getActionId(), service.undoableChicago().getActionId());
    }

    @Test
    void grokipediaApplyAddsOnlyAndUndoRestoresThePreviousValue() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book empty = bookRepository.save(book("Empty " + token, author));
        Book dashed = book("Dash " + token, author);
        dashed.setGrokipediaUrl("-");
        dashed = bookRepository.save(dashed);
        Book existing = book("Kept " + token, author);
        existing.setGrokipediaUrl("https://grokipedia.com/page/Kept_" + token);
        existing = bookRepository.save(existing);
        String added = "https://grokipedia.com/page/Empty_" + token;
        String dashAdded = "https://grokipedia.com/page/Dash_" + token;

        CatalogToolApplyResultDto applied = service.applyGrokipedia(List.of(
                change(empty.getId(), "Empty " + token, "", added),
                change(dashed.getId(), dashed.getTitle(), "-", dashAdded),
                change(existing.getId(), existing.getTitle(), existing.getGrokipediaUrl(),
                        "https://grokipedia.com/page/Replaced_" + token),
                change(empty.getId(), empty.getTitle(), "", "https://example.com/not-grokipedia")));

        assertEquals(2, applied.getApplied());
        assertEquals(added, bookRepository.findById(empty.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals(dashAdded, bookRepository.findById(dashed.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals("https://grokipedia.com/page/Kept_" + token,
                bookRepository.findById(existing.getId()).orElseThrow().getGrokipediaUrl());

        service.undoGrokipedia(applied.getActionId());
        assertNull(bookRepository.findById(empty.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals("-", bookRepository.findById(dashed.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals("https://grokipedia.com/page/Kept_" + token,
                bookRepository.findById(existing.getId()).orElseThrow().getGrokipediaUrl());
    }

    @Test
    void grokipediaCandidatesIncludeBooksThatAlreadyHaveAUrl() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book empty = bookRepository.save(book("Empty " + token, author));
        Book linked = book("Linked " + token, author);
        linked.setGrokipediaUrl("https://grokipedia.com/page/Linked_" + token);
        linked = bookRepository.save(linked);

        List<Long> ids = service.grokipediaCandidateIds().getBookIds();

        assertTrue(ids.contains(empty.getId()));
        assertTrue(ids.contains(linked.getId()));
    }

    @Test
    void grokipediaApplyAddsAnAlternateUrlWithoutRemovingTheExistingOne() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book book = book("Linked " + token, author);
        String kept = "https://grokipedia.com/page/Linked_" + token;
        String added = "https://grokipedia.com/page/Alt_" + token;
        book.setGrokipediaUrl(kept);
        book = bookRepository.save(book);
        String after = kept + " " + added;

        CatalogToolApplyResultDto applied = service.applyGrokipedia(List.of(
                change(book.getId(), book.getTitle(), kept, after)));

        assertEquals(1, applied.getApplied());
        assertEquals(after, bookRepository.findById(book.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals(added, applied.getChanges().get(0).getAdded());

        service.undoGrokipedia(applied.getActionId());
        assertEquals(kept, bookRepository.findById(book.getId()).orElseThrow().getGrokipediaUrl());
    }

    @Test
    void authorGrokipediaApplyAddsOnlyAndUndoRestoresThePreviousValue() {
        String token = token();
        Author empty = authorRepository.save(author("Empty " + token));
        Author linked = author("Linked " + token);
        String kept = "https://grokipedia.com/page/Linked_" + token;
        linked.setGrokipediaUrl(kept);
        linked = authorRepository.save(linked);
        String added = "https://grokipedia.com/page/Empty_" + token;
        String alternate = "https://grokipedia.com/page/Pen_" + token;

        CatalogToolCandidatesDto candidates = service.grokipediaAuthorCandidateIds();
        assertTrue(candidates.getAuthorIds().contains(empty.getId()));
        assertTrue(candidates.getAuthorIds().contains(linked.getId()));

        CatalogToolApplyResultDto applied = service.applyGrokipediaAuthors(List.of(
                authorChange(empty.getId(), empty.getName(), "", added),
                authorChange(linked.getId(), linked.getName(), kept, kept + " " + alternate),
                authorChange(linked.getId(), linked.getName(), kept, "https://grokipedia.com/page/Replaced_" + token)));

        assertEquals(2, applied.getApplied());
        assertEquals(added, authorRepository.findById(empty.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals(kept + " " + alternate, authorRepository.findById(linked.getId()).orElseThrow().getGrokipediaUrl());

        service.undoGrokipediaAuthors(applied.getActionId());
        assertNull(authorRepository.findById(empty.getId()).orElseThrow().getGrokipediaUrl());
        assertEquals(kept, authorRepository.findById(linked.getId()).orElseThrow().getGrokipediaUrl());
    }

    @Test
    void freeTextApplyAddsLinksAndUndoRemovesOnlyThoseLinks() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book book = book("Linked " + token, author);
        book.setFreeTextUrl("https://a.example/" + token);
        book = bookRepository.save(book);
        String before = book.getFreeTextUrl();
        String after = before + " https://b.example/" + token;

        CatalogToolApplyResultDto rejected = service.applyFreeText(List.of(
                change(book.getId(), book.getTitle(), before, "https://b.example/" + token)));
        assertEquals(0, rejected.getApplied());
        assertEquals(before, bookRepository.findById(book.getId()).orElseThrow().getFreeTextUrl());

        CatalogToolApplyResultDto applied = service.applyFreeText(List.of(
                change(book.getId(), book.getTitle(), before, after)));
        assertEquals(1, applied.getApplied());
        assertEquals(after, bookRepository.findById(book.getId()).orElseThrow().getFreeTextUrl());
        assertTrue(applied.getChanges().get(0).getAdded().contains("https://b.example/" + token));

        service.undoFreeText(applied.getActionId());
        assertEquals(before, bookRepository.findById(book.getId()).orElseThrow().getFreeTextUrl());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void authorGrokipediaRequiresLibrarian() throws Exception {
        mockMvc.perform(get("/api/maintenance/grokipedia-authors/candidates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void chicagoPreviewRequiresLibrarian() throws Exception {
        mockMvc.perform(post("/api/maintenance/chicago-titles/preview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void chicagoPreviewRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/maintenance/chicago-titles/preview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private static List<Long> changeIds(CatalogToolPreviewDto preview) {
        return preview.getChanges().stream().map(CatalogToolChangeDto::getBookId).toList();
    }

    private static CatalogToolChangeDto change(CatalogToolPreviewDto preview, Long bookId) {
        return preview.getChanges().stream()
                .filter(row -> bookId.equals(row.getBookId()))
                .findFirst()
                .orElseThrow();
    }

    private static CatalogToolChangeDto change(Long bookId, String title, String before, String after) {
        return CatalogToolChangeDto.builder()
                .bookId(bookId)
                .title(title)
                .before(before)
                .after(after)
                .build();
    }

    private static CatalogToolChangeDto authorChange(Long authorId, String name, String before, String after) {
        return CatalogToolChangeDto.builder()
                .authorId(authorId)
                .title(name)
                .before(before)
                .after(after)
                .build();
    }

    private static String token() {
        return "Tool" + UUID.randomUUID().toString().replace("-", "");
    }

    private static Author author(String name) {
        Author author = new Author();
        author.setName(name);
        return author;
    }

    private static Book book(String title, Author author) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        return book;
    }
}
