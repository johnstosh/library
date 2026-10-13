/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.service.CatalogFilterService.BookCatalogFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ACTIVE books with no call number and not electronic match the without-loc
 * status chip (non-ACTIVE without LOC are excluded per #337), and still appear
 * when every status chip is selected.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookStatusAllChipsIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CatalogCriteria catalogCriteria;

    @Test
    void allStatusChips_includesActiveBookWithoutLoc() {
        Book book = new Book();
        String title = "All status chips " + UUID.randomUUID();
        book.setTitle(title);
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.now());
        book.setLocNumber(null);
        book.setElectronicResource(false);
        Long bookId = bookRepository.saveAndFlush(book).getId();

        Page<Book> inLibraryOnly = search(title, true, false, false, false, false, false, false);
        assertFalse(inLibraryOnly.getContent().stream().anyMatch(b -> b.getId().equals(bookId)));

        Page<Book> withoutLocOnly = search(title, false, false, true, false, false, false, false);
        assertTrue(withoutLocOnly.getContent().stream().anyMatch(b -> b.getId().equals(bookId)));

        Page<Book> allChips = search(title, true, true, true, true, true, true, true);
        assertTrue(allChips.getContent().stream().anyMatch(b -> b.getId().equals(bookId)));
    }

    private Page<Book> search(String query, boolean inLibrary, boolean electronic, boolean withoutLoc,
                              boolean lost, boolean withdrawn, boolean onOrder, boolean requested) {
        BookCatalogFilter filter = new BookCatalogFilter();
        filter.query = query;
        List<BookStatusFilter> statuses = new ArrayList<>();
        if (inLibrary) {
            statuses.add(BookStatusFilter.IN_LIBRARY);
        }
        if (electronic) {
            statuses.add(BookStatusFilter.ELECTRONIC_RESOURCE);
        }
        if (withoutLoc) {
            statuses.add(BookStatusFilter.WITHOUT_LOC);
        }
        if (lost) {
            statuses.add(BookStatusFilter.LOST);
        }
        if (withdrawn) {
            statuses.add(BookStatusFilter.WITHDRAWN);
        }
        if (onOrder) {
            statuses.add(BookStatusFilter.ON_ORDER);
        }
        if (requested) {
            statuses.add(BookStatusFilter.REQUESTED);
        }
        filter.statuses = statuses;
        return catalogCriteria.searchBooks(filter, PageRequest.of(0, 20));
    }
}
