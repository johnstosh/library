/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The duplicate-title query and scan omit WITHDRAWN books.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DuplicateTitleScanIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private DuplicateTitleService duplicateTitleService;

    @Test
    void scanOmitsWithdrawnBooks() {
        Author augustine = author("Augustine of Hippo");
        Author melville = author("Herman Melville");

        Book active = book("The Confessions of Augustine", BookStatus.ACTIVE, augustine);
        Book withdrawnNear = book("Confession of Augustine", BookStatus.WITHDRAWN, augustine);
        Book lostNear = book("Confessions of Augustine", BookStatus.LOST, augustine);
        Book withdrawnOnly = book("City of God", BookStatus.WITHDRAWN, augustine);
        Book nullStatus = book("Moby Dick", null, melville);
        bookRepository.flush();

        DuplicateTitlesResultDto result = duplicateTitleService.findDuplicates();

        assertEquals(3, result.getBooksScanned());
        assertEquals(3, result.getRepresentativesCompared());
        assertEquals(1, result.getPairs().size());

        DuplicateTitlePairDto pair = result.getPairs().get(0);
        Set<Long> pairIds = Set.of(pair.getBookAId(), pair.getBookBId());
        assertEquals(Set.of(active.getId(), lostNear.getId()), pairIds);

        Set<Long> scannedIds = bookRepository.findAllForDuplicateTitleScan().stream()
                .map(BookRepository.DuplicateTitleProjection::getId)
                .collect(Collectors.toSet());
        assertTrue(scannedIds.contains(active.getId()));
        assertTrue(scannedIds.contains(lostNear.getId()));
        assertTrue(scannedIds.contains(nullStatus.getId()));
        assertFalse(scannedIds.contains(withdrawnNear.getId()));
        assertFalse(scannedIds.contains(withdrawnOnly.getId()));
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
