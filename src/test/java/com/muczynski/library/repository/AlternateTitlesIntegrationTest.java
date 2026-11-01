/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.service.CatalogFilterService.BookCatalogFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlternateTitlesIntegrationTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CatalogCriteria catalogCriteria;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void catalogAndCheckoutMatchALaterAlternateAndDoNotScoreItTwice() {
        Author author = new Author();
        author.setName("AltTitles Bernard");
        author = authorRepository.saveAndFlush(author);

        Book exact = book(author, "Love", List.of());
        Book containsAndExactAlternate = book(author, "Zebra Love Tale", List.of("Love"));
        Book laterAlternate = book(author, "Unrelated Holding", List.of("On Loving God", "De diligendo Deo"));
        entityManager.clear();

        BookCatalogFilter filter = new BookCatalogFilter();
        filter.query = "diligendo";
        Long laterId = laterAlternate.getId();
        assertTrue(catalogCriteria.bookSummaries(filter).stream().anyMatch(row -> laterId.equals(row.getId())));

        List<BookRepository.DuplicateTitleProjection> scan = bookRepository.findAllForDuplicateTitleScan();
        BookRepository.DuplicateTitleProjection scanned = scan.stream()
                .filter(row -> laterId.equals(row.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("On Loving God", "De diligendo Deo"), scanned.getAlternateTitles());

        List<String> checkout = bookRepository.findCheckoutMatches(1, "love", 0, "", 0, "", 10).stream()
                .map(BookRepository.CheckoutMatchProjection::getTitle)
                .toList();
        assertTrue(checkout.contains(exact.getTitle()), checkout.toString());
        assertTrue(checkout.contains(containsAndExactAlternate.getTitle()), checkout.toString());
        assertTrue(checkout.indexOf("Love") < checkout.indexOf("Zebra Love Tale"), checkout.toString());
    }

    private Book book(Author author, String title, List<String> alternateTitles) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.of(2026, 3, 1, 0, 0));
        if (!alternateTitles.isEmpty()) {
            book.setAlternateTitles(alternateTitles);
        }
        return bookRepository.saveAndFlush(book);
    }
}
