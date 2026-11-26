/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loads catalog rows for one issue and copies the fields the letter needs.
 * The copy is detached so the mail send does not keep a persistence session open.
 */
@Service
@Transactional(readOnly = true)
public class MailingListCatalog {

    private final BookRepository bookRepository;

    public MailingListCatalog(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> feastBooks() {
        return snapshots(bookRepository.findBooksWithFeastDay(), false);
    }

    public List<Book> addedBetween(LocalDateTime startUtcInclusive, LocalDateTime endUtcExclusive) {
        return snapshots(bookRepository.findAddedToLibraryBetween(startUtcInclusive, endUtcExclusive), false);
    }

    public List<Book> tagged(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<Long> ids = bookRepository.findIdsByAnyTag(tags);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return snapshots(bookRepository.findAllById(ids), true);
    }

    private static List<Book> snapshots(List<Book> books, boolean withTags) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        List<Book> copies = new ArrayList<>(books.size());
        for (Book book : books) {
            if (book == null) {
                continue;
            }
            copies.add(snapshot(book, withTags));
        }
        return copies;
    }

    private static Book snapshot(Book book, boolean withTags) {
        Book copy = new Book();
        copy.setId(book.getId());
        copy.setTitle(book.getTitle());
        copy.setRelatedFeastDay(book.getRelatedFeastDay());
        copy.setRelatedFeastDayExplanation(book.getRelatedFeastDayExplanation());
        copy.setMainCharacterSex(book.getMainCharacterSex());
        copy.setDateAddedToLibrary(book.getDateAddedToLibrary());
        if (withTags && book.getTagsList() != null) {
            copy.setTagsList(new ArrayList<>(book.getTagsList()));
        }
        return copy;
    }
}
