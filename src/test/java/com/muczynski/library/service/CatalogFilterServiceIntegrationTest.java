/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.AuthorSummaryDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the catalog list queries against Postgres so a JPQL mistake fails here
 * instead of on the Books, Prices, or Authors page.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogFilterServiceIntegrationTest {

    @Autowired
    private CatalogFilterService catalogFilterService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookPriceRepository bookPriceRepository;

    @Test
    void bookSummariesApplyQueryPlotBindingAndPrices() {
        String token = "Filter" + UUID.randomUUID().toString().replace("-", "");
        Author author = authorRepository.save(authorNamed("Author " + token));

        Book shortPlot = bookRepository.save(book(token + " short", author, "short", proper()));
        Book bothProper = bookRepository.save(book(token + " proper", author, proper(), proper()));
        Book priced = bookRepository.save(book(token + " priced", author, null, null));
        Book unmatched = bookRepository.save(book(token + " missing", author, null, null));
        Book errored = bookRepository.save(book(token + " errored", author, null, null));
        savePrice(priced, new BigDecimal("4.86"), null);
        savePrice(unmatched, null, "No matching listing");
        savePrice(errored, null, "AbeBooks rate limited");

        List<Long> byTitle = ids(catalogFilterService.bookSummaries(query(token), null, true));
        assertTrue(byTitle.contains(shortPlot.getId()));
        assertTrue(byTitle.contains(priced.getId()));

        List<Long> byAuthor = ids(catalogFilterService.bookSummaries(query("Author " + token), null, true));
        assertTrue(byAuthor.contains(shortPlot.getId()));
        assertTrue(byAuthor.contains(bothProper.getId()));

        CatalogFilterService.BookCatalogFilter plot = query(token);
        plot.withoutProperPlotOrDescription = true;
        List<Long> plotIds = ids(catalogFilterService.bookSummaries(plot, null, true));
        assertTrue(plotIds.contains(shortPlot.getId()));
        assertFalse(plotIds.contains(bothProper.getId()));

        CatalogFilterService.BookCatalogFilter traits = query(token + " short");
        traits.bindings = List.of(BookCoverType.UNKNOWN);
        traits.readingDifficulties = List.of(ReadingDifficulty.UNSET);
        traits.desireUnset = true;
        assertEquals(List.of(shortPlot.getId()), ids(catalogFilterService.bookSummaries(traits, null, true)));

        CatalogFilterService.BookCatalogFilter withPrices = query(token);
        withPrices.withPrices = true;
        assertEquals(List.of(priced.getId()), ids(catalogFilterService.bookSummaries(withPrices, null, true)));

        CatalogFilterService.BookCatalogFilter noPrices = query(token);
        noPrices.noPrices = true;
        List<Long> noPriceIds = ids(catalogFilterService.bookSummaries(noPrices, null, true));
        assertFalse(noPriceIds.contains(priced.getId()));
        assertTrue(noPriceIds.contains(unmatched.getId()));

        CatalogFilterService.BookCatalogFilter older = query(token);
        older.noPrices = true;
        older.priceOlder = true;
        List<Long> olderIds = ids(catalogFilterService.bookSummaries(older, null, true));
        assertTrue(olderIds.contains(unmatched.getId()));
        assertFalse(olderIds.contains(priced.getId()));

        CatalogFilterService.BookCatalogFilter errors = query(token);
        errors.lookupErrors = true;
        assertEquals(List.of(errored.getId()), ids(catalogFilterService.bookSummaries(errors, null, true)));
    }

    @Test
    void authorSummariesAndMostRecentWithOtherChips() {
        String token = "AuthorFilter" + UUID.randomUUID().toString().replace("-", "");
        Author recent = authorRepository.save(authorNamed(token + " recent"));
        Author older = authorRepository.save(authorNamed(token + " older"));
        Author noBooks = authorRepository.save(authorNamed(token + " none"));
        recent.setGrokipediaUrl("-");
        older.setGrokipediaUrl("https://grokipedia.com/page/Example");
        authorRepository.save(recent);
        authorRepository.save(older);

        Book recentBook = book(token + " recent book", recent, null, null);
        recentBook.setDateAddedToLibrary(LocalDateTime.of(2026, 9, 30, 15, 0));
        Book olderBook = book(token + " older book", older, null, null);
        olderBook.setDateAddedToLibrary(LocalDateTime.of(2020, 1, 1, 0, 0));
        bookRepository.save(recentBook);
        bookRepository.save(olderBook);

        CatalogFilterService.AuthorCatalogFilter mostRecent = new CatalogFilterService.AuthorCatalogFilter();
        mostRecent.mostRecent = true;
        mostRecent.withoutGrokipedia = true;
        List<Long> ids = authorIds(catalogFilterService.authorSummaries(mostRecent, null));
        assertTrue(ids.contains(recent.getId()));
        assertFalse(ids.contains(older.getId()));
        assertFalse(ids.contains(noBooks.getId()));

        CatalogFilterService.AuthorCatalogFilter described = new CatalogFilterService.AuthorCatalogFilter();
        described.withoutDescription = true;
        described.zeroBooks = true;
        List<Long> none = authorIds(catalogFilterService.authorSummaries(described, null));
        assertTrue(none.contains(noBooks.getId()));
        assertFalse(none.contains(recent.getId()));
    }

    private static CatalogFilterService.BookCatalogFilter query(String text) {
        CatalogFilterService.BookCatalogFilter filter = new CatalogFilterService.BookCatalogFilter();
        filter.query = text;
        return filter;
    }

    private static Author authorNamed(String name) {
        Author author = new Author();
        author.setName(name);
        return author;
    }

    private static Book book(String title, Author author, String plot, String description) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.of(2026, 9, 30, 12, 0));
        book.setPlotEssay(plot);
        book.setDetailedDescription(description);
        return book;
    }

    private void savePrice(Book book, BigDecimal dollars, String lookupError) {
        BookPrice price = new BookPrice();
        price.setBook(book);
        price.setCover(BookCoverType.HARDCOVER);
        price.setPriceDollars(dollars);
        price.setLookupError(lookupError);
        price.setLookedUpAt(LocalDateTime.now(ZoneOffset.UTC));
        bookPriceRepository.save(price);
    }

    private static String proper() {
        return "x".repeat(CatalogFilterService.PROPER_TEXT_MIN_CHARS);
    }

    private static List<Long> ids(List<BookSummaryDto> summaries) {
        return summaries.stream().map(BookSummaryDto::getId).sorted().toList();
    }

    private static List<Long> authorIds(List<AuthorSummaryDto> summaries) {
        return summaries.stream().map(AuthorSummaryDto::getId).toList();
    }
}
