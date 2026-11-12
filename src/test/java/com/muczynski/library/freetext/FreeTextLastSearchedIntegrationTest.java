/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.service.CatalogToolMaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The maintenance search commits each finished book. This class does not use a
 * test transaction, because {@code REQUIRES_NEW} cannot see uncommitted rows
 * and a committed row would survive a rollback. Rows created here are deleted
 * before the method returns.
 */
@SpringBootTest
@ActiveProfiles("test")
class FreeTextLastSearchedIntegrationTest {

    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private CatalogToolMaintenanceService maintenanceService;
    @Autowired
    private FreeTextLookupService freeTextLookupService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void oldestNullsSortFirstAndAFinishedSearchCommits() {
        String token = UUID.randomUUID().toString().replace("-", "");
        List<Long> bookIds = new ArrayList<>();
        Long authorId = null;
        try {
            Author author = new Author();
            author.setName("Author " + token);
            author.setAlternateNames(List.of("Pen " + token));
            author = authorRepository.save(author);
            authorId = author.getId();

            Book never = book("Never " + token, author, null);
            Book old = book("Old " + token, author, LocalDateTime.now(ZoneOffset.UTC).minusDays(400));
            Book recent = book("Recent " + token, author, LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
            Book temporary = book("2024-01-02 " + token, author, null);
            Book searchable = book("Primary " + token, author, null);
            searchable.setAlternateTitles(List.of("Other " + token));
            searchable.setFreeTextUrl("https://old.example/" + token);
            searchable = bookRepository.save(searchable);
            for (Book saved : List.of(never, old, recent, temporary, searchable)) {
                bookIds.add(saved.getId());
            }

            List<BookRepository.FreeTextCandidateRow> ordered = bookRepository.findFreeTextCandidates();
            int neverAt = indexOf(ordered, never.getId());
            int oldAt = indexOf(ordered, old.getId());
            int recentAt = indexOf(ordered, recent.getId());
            assertTrue(neverAt >= 0 && neverAt < oldAt && oldAt < recentAt);

            List<Long> limited = maintenanceService.freeTextCandidateIds(800).getBookIds();
            assertFalse(limited.contains(temporary.getId()));

            List<BookRepository.FreeTextSearchRow> rows = bookRepository.findFreeTextSearchRows(List.of(searchable.getId()));
            assertEquals(1, rows.size());
            assertEquals("Primary " + token, rows.get(0).getTitle());
            assertEquals(List.of("Other " + token), rows.get(0).getAlternateTitles());
            assertEquals("https://old.example/" + token, rows.get(0).getFreeTextUrl());
            assertEquals("Author " + token, rows.get(0).getAuthorName());
            assertEquals(List.of("Pen " + token), rows.get(0).getAuthorAlternateNames());

            freeTextLookupService.recordFreeTextSearch(never.getId(), null, false);
            Book stampedMiss = bookRepository.findById(never.getId()).orElseThrow();
            assertNotNull(stampedMiss.getFreeTextLastSearched());
            assertNull(stampedMiss.getFreeTextUrl());

            freeTextLookupService.recordFreeTextSearch(old.getId(), "https://found.example/" + token, true);
            Book stampedHit = bookRepository.findById(old.getId()).orElseThrow();
            assertEquals("https://found.example/" + token, stampedHit.getFreeTextUrl());
            assertNotNull(stampedHit.getFreeTextLastSearched());
            assertNotNull(stampedHit.getLastModified());

            ResponseStatusException rejected = assertThrows(ResponseStatusException.class,
                    () -> maintenanceService.freeTextCandidateIds(50));
            assertEquals(HttpStatus.BAD_REQUEST, rejected.getStatusCode());
        } finally {
            if (!bookIds.isEmpty()) {
                bookRepository.deleteAllById(bookIds);
            }
            if (authorId != null) {
                authorRepository.deleteById(authorId);
            }
        }
    }

    private Book book(String title, Author author, LocalDateTime searched) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book.setFreeTextLastSearched(searched);
        return bookRepository.save(book);
    }

    private static int indexOf(List<BookRepository.FreeTextCandidateRow> rows, Long id) {
        for (int i = 0; i < rows.size(); i++) {
            if (id.equals(rows.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
