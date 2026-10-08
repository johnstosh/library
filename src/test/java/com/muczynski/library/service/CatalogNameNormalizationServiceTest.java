/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.AuthorNameNormalizationResultDto;
import com.muczynski.library.dto.NameNormalizationResultDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogNameNormalizationServiceTest {

    @Autowired
    private CatalogNameNormalizationService service;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void rewritesNonconformingTitlesAndLeavesConformingOnesAlone() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book lower = bookRepository.save(book("the history of " + token, author));
        Book ready = bookRepository.save(book("Ready " + token, author));

        List<NameNormalizationResultDto> results = service.normalizeTitles(List.of(lower.getId(), ready.getId()));

        assertTrue(results.get(0).isChanged());
        assertEquals("The History of " + token, results.get(0).getAfter());
        assertFalse(results.get(1).isChanged());
        assertEquals("The History of " + token, results.get(0).getUpdatedBook().getTitle());
        assertEquals("The History of " + token, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
        assertEquals("Ready " + token, bookRepository.findById(ready.getId()).orElseThrow().getTitle());
    }

    @Test
    void skipsATitleThatWouldCollide() {
        String token = token();
        Author author = authorRepository.save(author("Author " + token));
        Book ready = bookRepository.save(book("The History of " + token, author));
        Book lower = bookRepository.save(book("the history of " + token, author));

        List<NameNormalizationResultDto> results = service.normalizeTitles(List.of(lower.getId()));

        assertFalse(results.get(0).isSuccess());
        assertEquals("the history of " + token, bookRepository.findById(lower.getId()).orElseThrow().getTitle());
        assertEquals(ready.getId(), bookRepository.findById(ready.getId()).orElseThrow().getId());
    }

    @Test
    void renamesAuthorInPlaceWhenEveryBookIsSelectedAndKeepsPhotos() {
        String token = token();
        Author author = authorRepository.saveAndFlush(author("Simpson, " + token));
        Photo photo = new Photo();
        photo.setAuthor(author);
        photo.setImage(new byte[] {1, 2, 3});
        photo.setContentType("image/jpeg");
        author.getPhotos().add(photo);
        authorRepository.saveAndFlush(author);
        Book book = bookRepository.save(book("Life of " + token, author));

        List<NameNormalizationResultDto> results = service.normalizeAuthors(List.of(book.getId()));

        assertTrue(results.get(0).isChanged());
        assertEquals("Simpson, " + token, results.get(0).getBefore());
        assertEquals(token + " Simpson", results.get(0).getAfter());
        Author reloaded = authorRepository.findById(author.getId()).orElseThrow();
        assertEquals(token + " Simpson", reloaded.getName());
        assertEquals(1, reloaded.getPhotos().size());
        assertEquals(author.getId(), bookRepository.findById(book.getId()).orElseThrow().getAuthor().getId());
    }

    @Test
    void relinksOnlySelectedBooksWhenTheAuthorHasOthers() {
        String token = token();
        Author author = authorRepository.save(author("Simpson, " + token));
        Book selected = bookRepository.save(book("Selected " + token, author));
        Book other = bookRepository.save(book("Other " + token, author));

        List<NameNormalizationResultDto> results = service.normalizeAuthors(List.of(selected.getId()));

        assertTrue(results.get(0).isChanged());
        assertEquals(token + " Simpson", results.get(0).getAfter());
        Book selectedReloaded = bookRepository.findById(selected.getId()).orElseThrow();
        Book otherReloaded = bookRepository.findById(other.getId()).orElseThrow();
        assertEquals(token + " Simpson", selectedReloaded.getAuthor().getName());
        assertEquals("Simpson, " + token, otherReloaded.getAuthor().getName());
        assertFalse(selectedReloaded.getAuthor().getId().equals(author.getId()));
    }

    @Test
    void expandsInitialsAndReusesAnExistingCanonicalAuthor() {
        String token = token();
        Author canonical = authorRepository.save(author("Barney John Paul " + token));
        Author inverted = authorRepository.save(author(token + ", B. J.-P. (Barney John-Paul)"));
        Book book = bookRepository.save(book("Letters of " + token, inverted));

        List<NameNormalizationResultDto> results = service.normalizeAuthors(List.of(book.getId()));

        assertTrue(results.get(0).isChanged());
        assertEquals("Barney John Paul " + token, results.get(0).getAfter());
        assertEquals(canonical.getId(), bookRepository.findById(book.getId()).orElseThrow().getAuthor().getId());
        assertEquals(token + ", B. J.-P. (Barney John-Paul)",
                authorRepository.findById(inverted.getId()).orElseThrow().getName());
    }

    @Test
    void mergesTwoInvertedAuthorsInOneBatchOntoOneRow() {
        String token = token();
        Author first = authorRepository.save(author("Simpson, " + token));
        Author second = authorRepository.save(author("Simpson, S. (" + token + ")"));
        Book firstBook = bookRepository.save(book("First " + token, first));
        Book secondBook = bookRepository.save(book("Second " + token, second));

        List<NameNormalizationResultDto> results = service.normalizeAuthors(List.of(firstBook.getId(), secondBook.getId()));

        assertEquals(token + " Simpson", results.get(0).getAfter());
        assertEquals(token + " Simpson", results.get(1).getAfter());
        Long targetId = bookRepository.findById(firstBook.getId()).orElseThrow().getAuthor().getId();
        assertEquals(targetId, bookRepository.findById(secondBook.getId()).orElseThrow().getAuthor().getId());
    }

    @Test
    void renamesSelectedAuthorInPlaceAndKeepsPhotosAndAlternateNames() {
        String token = token();
        Author author = authorRepository.saveAndFlush(author("Simpson, " + token));
        author.setAlternateNames(new ArrayList<>(List.of("Pen " + token)));
        Photo photo = new Photo();
        photo.setAuthor(author);
        photo.setImage(new byte[] {1, 2, 3});
        photo.setContentType("image/jpeg");
        author.getPhotos().add(photo);
        authorRepository.saveAndFlush(author);

        List<AuthorNameNormalizationResultDto> results = service.normalizeAuthorNames(List.of(author.getId()));

        assertTrue(results.get(0).isChanged());
        assertEquals("Simpson, " + token, results.get(0).getBefore());
        assertEquals(token + " Simpson", results.get(0).getAfter());
        Author reloaded = authorRepository.findById(author.getId()).orElseThrow();
        assertEquals(token + " Simpson", reloaded.getName());
        assertEquals(List.of("Pen " + token), reloaded.getAlternateNames());
        assertEquals(1, reloaded.getPhotos().size());
        assertEquals(token + " Simpson", results.get(0).getUpdatedAuthor().getName());
    }

    @Test
    void skipsAnAuthorWhoseCanonicalNameIsAlreadyUsed() {
        String token = token();
        Author canonical = authorRepository.save(author(token + " Simpson"));
        Author inverted = authorRepository.save(author("Simpson, " + token));

        List<AuthorNameNormalizationResultDto> results = service.normalizeAuthorNames(List.of(inverted.getId()));

        assertFalse(results.get(0).isSuccess());
        assertEquals("Simpson, " + token, authorRepository.findById(inverted.getId()).orElseThrow().getName());
        assertEquals(token + " Simpson", authorRepository.findById(canonical.getId()).orElseThrow().getName());
    }

    @Test
    void renamesOnlyTheLowestIdWhenTwoAuthorsCanonicalizeToTheSameName() {
        String token = token();
        Author first = authorRepository.save(author("Simpson, " + token));
        Author second = authorRepository.save(author("Simpson, S. (" + token + ")"));

        List<AuthorNameNormalizationResultDto> results = service.normalizeAuthorNames(List.of(second.getId(), first.getId()));

        assertEquals(second.getId(), results.get(0).getAuthorId());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(1).isChanged());
        assertEquals(token + " Simpson", authorRepository.findById(first.getId()).orElseThrow().getName());
        assertEquals("Simpson, S. (" + token + ")", authorRepository.findById(second.getId()).orElseThrow().getName());
    }

    @Test
    void leavesACanonicalAuthorAndReportsAMissingAuthor() {
        String token = token();
        Author ready = authorRepository.save(author(token + " Ready"));

        List<AuthorNameNormalizationResultDto> results = service.normalizeAuthorNames(List.of(ready.getId(), 9_000_000_000L));

        assertFalse(results.get(0).isChanged());
        assertTrue(results.get(0).isSuccess());
        assertEquals(token + " Ready", authorRepository.findById(ready.getId()).orElseThrow().getName());
        assertFalse(results.get(1).isSuccess());
        assertEquals("Author not found", results.get(1).getErrorMessage());
    }

    private static String token() {
        return "Norm" + UUID.randomUUID().toString().replace("-", "");
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
