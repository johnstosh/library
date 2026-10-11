/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.ReadingDifficulty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorAlternateNamesIntegrationTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void textArrayRoundTripsAndMatchesAuthorSearchCheckoutAndCatalogQuery() {
        Author author = new Author();
        author.setName("Issue372 Aurelius Augustinus");
        author.setAlternateNames(List.of("Saint Augustine", "Augustine of Hippo"));
        author = authorRepository.saveAndFlush(author);

        Author plain = new Author();
        plain.setName("Issue372 Plain Author");
        plain = authorRepository.saveAndFlush(plain);

        Book book = new Book();
        book.setTitle("Issue372 Confessions");
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.of(2026, 1, 2, 3, 4));
        book = bookRepository.saveAndFlush(book);

        entityManager.clear();

        Author reloaded = authorRepository.findById(author.getId()).orElseThrow();
        assertEquals(List.of("Saint Augustine", "Augustine of Hippo"), reloaded.getAlternateNames());

        assertEquals(1, authorRepository.findByNameContainingIgnoreCase(
                "Augustine of Hippo", PageRequest.of(0, 20)).getTotalElements());
        assertEquals(1, authorRepository.findByNameContainingIgnoreCase(
                "Issue372 Plain", PageRequest.of(0, 20)).getTotalElements());

        List<BookRepository.CheckoutMatchProjection> checkout = bookRepository.findCheckoutMatches(
                0, "", 1, "hippo", 0, "", 10);
        Long bookId = book.getId();
        assertTrue(checkout.stream().anyMatch(match -> bookId.equals(match.getId())));

        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);
        List<BookRepository.DatedBookSummaryProjection> summaries = bookRepository.findFilteredSummaries(
                "augustine of hippo",
                false, false, false, false,
                false, epoch, List.of(-1L),
                false, false, false, false,
                false, false, false, false,
                false,
                false, false, false,
                false, false, false,
                false, false, false,
                false,
                false, List.of(ReadingDifficulty.UNSET), false,
                false, List.of(-1L),
                false, List.of(BookCoverType.UNKNOWN), false,
                false, List.of(0), false,
                false,
                false, false, false,
                epoch, epoch,
                false);
        assertTrue(summaries.stream().anyMatch(summary -> bookId.equals(summary.getId())));
    }
}
