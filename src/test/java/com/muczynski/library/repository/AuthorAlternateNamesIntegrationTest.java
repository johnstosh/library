/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.AuthorNameMatchDto;
import com.muczynski.library.service.AuthorNameMatchService;
import com.muczynski.library.service.CatalogFilterService.BookCatalogFilter;
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

    @Autowired
    private CatalogCriteria catalogCriteria;

    @Autowired
    private AuthorNameMatchService authorNameMatchService;

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

        assertEquals(1, catalogCriteria.searchAuthorsByName(
                "Augustine of Hippo", PageRequest.of(0, 20)).getTotalElements());
        assertEquals(1, catalogCriteria.searchAuthorsByName(
                "Issue372 Plain", PageRequest.of(0, 20)).getTotalElements());

        List<BookRepository.CheckoutMatchProjection> checkout = bookRepository.findCheckoutMatches(
                0, "", 1, "hippo", 0, "", 10);
        Long bookId = book.getId();
        assertTrue(checkout.stream().anyMatch(match -> bookId.equals(match.getId())));

        BookCatalogFilter filter = new BookCatalogFilter();
        filter.query = "augustine of hippo";
        List<BookRepository.DatedBookSummaryProjection> summaries = catalogCriteria.bookSummaries(filter);
        assertTrue(summaries.stream().anyMatch(summary -> bookId.equals(summary.getId())));
    }

    @Test
    void nameMatchesUseTheCheckoutGateAndRankExactNamesFirst() {
        assertTrue(authorNameMatchService.matches("au").isEmpty());

        Author exact = author("AuthorMatch Augustine", List.of());
        Author alternate = author(
                "AuthorMatch Augustinus Hippo",
                List.of("AuthorMatch Augustine of Hippo", "AuthorMatch Unique Hippo"));
        author("AuthorMatch Other", List.of());
        for (int i = 1; i <= 11; i++) {
            author(String.format("AuthorMatch Limit %02d", i), List.of());
        }
        authorRepository.flush();

        List<AuthorNameMatchDto> augustine = authorNameMatchService.matches("AuthorMatch Augustine");
        assertEquals(exact.getId(), augustine.get(0).getId());
        assertTrue(augustine.stream().anyMatch(match -> alternate.getId().equals(match.getId())));
        assertTrue(augustine.stream().noneMatch(match -> "AuthorMatch Other".equals(match.getName())));

        List<AuthorNameMatchDto> byAlternate = authorNameMatchService.matches("unique hippo");
        assertEquals(1, byAlternate.size());
        assertEquals(alternate.getId(), byAlternate.get(0).getId());

        List<AuthorNameMatchDto> limited = authorNameMatchService.matches("AuthorMatch Limit");
        assertEquals(AuthorNameMatchService.MATCH_LIMIT, limited.size());
        assertEquals("AuthorMatch Limit 01", limited.get(0).getName());
        assertEquals("AuthorMatch Limit 10", limited.get(limited.size() - 1).getName());
    }

    private Author author(String name, List<String> alternateNames) {
        Author author = new Author();
        author.setName(name);
        if (!alternateNames.isEmpty()) {
            author.setAlternateNames(alternateNames);
        }
        return authorRepository.save(author);
    }
}
