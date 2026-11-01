/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.service.CatalogFilterService.AuthorCatalogFilter;
import com.muczynski.library.service.CatalogFilterService.BookCatalogFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Public-library format chips OR within one library and AND across libraries.
 * A Without chip matches false and unset. On authors, Without means no title
 * has that holding.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogAvailabilityFilterIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CatalogCriteria catalogCriteria;

    @Test
    void bookFormatsOrWithinOneLibraryAndTreatUnsetAsNotHeld() {
        String marker = "Avail " + UUID.randomUUID();
        Book audio = saveBook(marker + " audio", null);
        audio.setYdlAudioAvailable(true);
        audio.setYdlPaperAvailable(false);
        bookRepository.save(audio);

        Book paper = saveBook(marker + " paper", null);
        paper.setYdlPaperAvailable(true);
        bookRepository.save(paper);

        Book both = saveBook(marker + " both", null);
        both.setYdlAudioAvailable(true);
        both.setEmuPaperAvailable(true);
        bookRepository.save(both);

        Book unset = saveBook(marker + " unset", null);
        bookRepository.saveAndFlush(unset);

        BookCatalogFilter ydlEither = new BookCatalogFilter();
        ydlEither.query = marker;
        ydlEither.ydlAudio = true;
        ydlEither.ydlBook = true;
        assertTrue(containsTitle(ydlEither, audio.getTitle()));
        assertTrue(containsTitle(ydlEither, paper.getTitle()));
        assertTrue(containsTitle(ydlEither, both.getTitle()));
        assertFalse(containsTitle(ydlEither, unset.getTitle()));

        BookCatalogFilter across = new BookCatalogFilter();
        across.query = marker;
        across.ydlAudio = true;
        across.emuBook = true;
        assertTrue(containsTitle(across, both.getTitle()));
        assertFalse(containsTitle(across, audio.getTitle()));

        BookCatalogFilter withoutAudio = new BookCatalogFilter();
        withoutAudio.query = marker;
        withoutAudio.withoutYdlAudio = true;
        assertFalse(containsTitle(withoutAudio, audio.getTitle()));
        assertTrue(containsTitle(withoutAudio, paper.getTitle()));
        assertTrue(containsTitle(withoutAudio, unset.getTitle()));

        BookCatalogFilter audioAndWithoutBook = new BookCatalogFilter();
        audioAndWithoutBook.query = marker;
        audioAndWithoutBook.ydlAudio = true;
        audioAndWithoutBook.withoutYdlBook = true;
        assertTrue(containsTitle(audioAndWithoutBook, audio.getTitle()));
        assertFalse(containsTitle(audioAndWithoutBook, paper.getTitle()));
        assertFalse(containsTitle(audioAndWithoutBook, unset.getTitle()));
    }

    @Test
    void authorWithoutMeansNoTitleHasThatHolding() {
        String marker = "Avail author " + UUID.randomUUID();
        Author audioAuthor = saveAuthor(marker + " audio");
        Book audio = saveBook(marker + " audio book", audioAuthor);
        audio.setYdlAudioAvailable(true);
        bookRepository.save(audio);

        Author paperAuthor = saveAuthor(marker + " paper");
        Book paper = saveBook(marker + " paper book", paperAuthor);
        paper.setYdlPaperAvailable(true);
        bookRepository.save(paper);

        Author unsetAuthor = saveAuthor(marker + " unset");
        saveBook(marker + " unset book", unsetAuthor);

        Author emptyAuthor = saveAuthor(marker + " empty");
        authorRepository.saveAndFlush(emptyAuthor);
        bookRepository.flush();

        AuthorCatalogFilter either = new AuthorCatalogFilter();
        either.query = marker;
        either.ydlAudio = true;
        either.ydlBook = true;
        assertTrue(containsAuthor(either, audioAuthor.getId()));
        assertTrue(containsAuthor(either, paperAuthor.getId()));
        assertFalse(containsAuthor(either, unsetAuthor.getId()));
        assertFalse(containsAuthor(either, emptyAuthor.getId()));

        AuthorCatalogFilter withoutAudio = new AuthorCatalogFilter();
        withoutAudio.query = marker;
        withoutAudio.withoutYdlAudio = true;
        assertFalse(containsAuthor(withoutAudio, audioAuthor.getId()));
        assertTrue(containsAuthor(withoutAudio, paperAuthor.getId()));
        assertTrue(containsAuthor(withoutAudio, unsetAuthor.getId()));
        assertTrue(containsAuthor(withoutAudio, emptyAuthor.getId()));

        AuthorCatalogFilter audioWithoutEbook = new AuthorCatalogFilter();
        audioWithoutEbook.query = marker;
        audioWithoutEbook.ydlAudio = true;
        audioWithoutEbook.withoutYdlEbook = true;
        assertTrue(containsAuthor(audioWithoutEbook, audioAuthor.getId()));
        assertFalse(containsAuthor(audioWithoutEbook, paperAuthor.getId()));
    }

    private Book saveBook(String title, Author author) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.now());
        return bookRepository.save(book);
    }

    private Author saveAuthor(String name) {
        Author author = new Author();
        author.setName(name);
        return authorRepository.save(author);
    }

    private boolean containsTitle(BookCatalogFilter filter, String title) {
        return catalogCriteria.searchBooks(filter, PageRequest.of(0, 50)).getContent().stream()
                .anyMatch(book -> title.equals(book.getTitle()));
    }

    private boolean containsAuthor(AuthorCatalogFilter filter, Long authorId) {
        return catalogCriteria.authorSummaries(filter).stream()
                .anyMatch(author -> authorId.equals(author.getId()));
    }
}
