/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.ReadingDifficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Active books with no call number and not electronic match no individual
 * status chip. Selecting every chip must still include them.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookStatusAllChipsIntegrationTest {

    private static final List<Long> NO_IDS = List.of(-1L);
    private static final List<ReadingDifficulty> UNUSED_DIFFICULTIES = List.of(ReadingDifficulty.UNSET);

    @Autowired
    private BookRepository bookRepository;

    @Test
    void findWithFilters_allStatusChips_includesActiveBookWithoutLoc() {
        Book book = new Book();
        String title = "All status chips " + UUID.randomUUID();
        book.setTitle(title);
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.now());
        book.setLocNumber(null);
        book.setElectronicResource(false);
        Long bookId = bookRepository.saveAndFlush(book).getId();

        Page<Book> inLibraryOnly = search(title, true, false, false, false, false, false);
        assertFalse(inLibraryOnly.getContent().stream().anyMatch(b -> b.getId().equals(bookId)));

        Page<Book> allChips = search(title, true, true, true, true, true, true);
        assertTrue(allChips.getContent().stream().anyMatch(b -> b.getId().equals(bookId)));
    }

    private Page<Book> search(String query, boolean inLibrary, boolean electronic,
                              boolean lost, boolean withdrawn, boolean onOrder, boolean requested) {
        return bookRepository.findWithFilters(
                query, inLibrary, electronic, false, false,
                false, LocalDateTime.of(1970, 1, 1, 0, 0), NO_IDS,
                false, false, false, false,
                lost, withdrawn, onOrder, requested,
                false,
                false, false, false, false, false, false,
                false, false, false,
                false,
                false, UNUSED_DIFFICULTIES, false,
                false, NO_IDS,
                PageRequest.of(0, 20));
    }
}
