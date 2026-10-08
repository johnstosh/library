/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.NameNormalizationResultDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.util.CanonicalAuthorName;
import com.muczynski.library.util.ChicagoTitleCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites selected books' titles into Chicago title case, and their authors
 * into canonical given-name-then-family-name form.
 * <p>
 * Author rows are updated in place when every book by that author is selected
 * and the canonical name is free. Otherwise the selected books are relinked to
 * the existing canonical author, or to one new author created for that name.
 * The managed author is edited directly so its photos stay put.
 */
@Service
public class CatalogNameNormalizationService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookMapper bookMapper;

    @Transactional
    public List<NameNormalizationResultDto> normalizeTitles(List<Long> bookIds) {
        List<Long> ids = distinctIds(bookIds);
        Map<Long, Book> books = loadBooks(ids);
        Set<String> claimed = new HashSet<>();
        List<NameNormalizationResultDto> results = new ArrayList<>();
        for (Long id : ids) {
            Book book = books.get(id);
            if (book == null) {
                results.add(missing(id));
                continue;
            }
            String before = book.getTitle();
            String after = ChicagoTitleCase.toChicago(before);
            if (after == null || after.isBlank() || after.equals(before)) {
                results.add(unchanged(book, before, before == null ? "" : before));
                continue;
            }
            if (bookRepository.existsByTitleAndIdNot(after, book.getId()) || !claimed.add(after)) {
                results.add(NameNormalizationResultDto.builder()
                        .bookId(book.getId())
                        .title(before)
                        .before(before)
                        .after(after)
                        .changed(false)
                        .success(false)
                        .errorMessage("Title \"" + after + "\" is already used by another book")
                        .build());
                continue;
            }
            book.setTitle(after);
            bookRepository.save(book);
            results.add(changed(book, before, after));
        }
        return results;
    }

    @Transactional
    public List<NameNormalizationResultDto> normalizeAuthors(List<Long> bookIds) {
        List<Long> ids = distinctIds(bookIds);
        Map<Long, Book> books = loadBooks(ids);

        Map<Long, List<Book>> selectedByAuthor = new LinkedHashMap<>();
        Map<Long, String> originalNames = new HashMap<>();
        Map<Long, String> desiredByAuthor = new HashMap<>();
        for (Long id : ids) {
            Book book = books.get(id);
            if (book == null || book.getAuthor() == null) {
                continue;
            }
            Author author = book.getAuthor();
            originalNames.putIfAbsent(author.getId(), author.getName());
            selectedByAuthor.computeIfAbsent(author.getId(), ignored -> new ArrayList<>()).add(book);
        }

        Map<String, Author> targets = new HashMap<>();
        for (Map.Entry<Long, List<Book>> entry : selectedByAuthor.entrySet()) {
            Author author = entry.getValue().get(0).getAuthor();
            String original = originalNames.get(author.getId());
            String desired = CanonicalAuthorName.canonical(original);
            if (desired == null || desired.isBlank()) {
                desired = original;
            }
            desiredByAuthor.put(author.getId(), desired);
            if (desired == null || desired.equals(original)) {
                continue;
            }
            List<Author> existing = authorRepository.findAllByNameOrderByIdAsc(desired);
            if (!existing.isEmpty()) {
                targets.put(desired, existing.get(0));
            }
        }

        List<Author> renameCandidates = new ArrayList<>();
        for (Map.Entry<Long, List<Book>> entry : selectedByAuthor.entrySet()) {
            Author author = entry.getValue().get(0).getAuthor();
            String desired = desiredByAuthor.get(author.getId());
            String original = originalNames.get(author.getId());
            if (desired == null || desired.equals(original) || desired.isBlank()) {
                continue;
            }
            if (targets.containsKey(desired)) {
                continue;
            }
            long total = bookRepository.countByAuthorId(author.getId());
            if (total == entry.getValue().size()) {
                renameCandidates.add(author);
            }
        }
        renameCandidates.sort(Comparator.comparing(Author::getId));
        for (Author author : renameCandidates) {
            String desired = desiredByAuthor.get(author.getId());
            if (targets.containsKey(desired)) {
                continue;
            }
            author.setName(desired);
            authorRepository.saveAndFlush(author);
            targets.put(desired, author);
        }

        List<NameNormalizationResultDto> results = new ArrayList<>();
        for (Long id : ids) {
            Book book = books.get(id);
            if (book == null) {
                results.add(missing(id));
                continue;
            }
            if (book.getAuthor() == null) {
                results.add(unchanged(book, "", ""));
                continue;
            }
            Author current = book.getAuthor();
            String before = originalNames.getOrDefault(current.getId(), current.getName());
            String after = desiredByAuthor.getOrDefault(current.getId(), before);
            if (after == null || after.isBlank() || after.equals(before)) {
                results.add(unchanged(book, before, before == null ? "" : before));
                continue;
            }
            Author target = targets.get(after);
            if (target == null) {
                Author created = new Author();
                created.setName(after);
                target = authorRepository.saveAndFlush(created);
                targets.put(after, target);
            }
            if (!target.getId().equals(current.getId())) {
                book.setAuthor(target);
                bookRepository.save(book);
            }
            results.add(changed(book, before, after));
        }
        return results;
    }

    private Map<Long, Book> loadBooks(List<Long> ids) {
        Map<Long, Book> books = new HashMap<>();
        if (ids.isEmpty()) {
            return books;
        }
        for (Book book : bookRepository.findAllById(ids)) {
            books.put(book.getId(), book);
        }
        return books;
    }

    private static List<Long> distinctIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(bookIds));
    }

    private NameNormalizationResultDto changed(Book book, String before, String after) {
        return NameNormalizationResultDto.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .before(before)
                .after(after)
                .changed(true)
                .success(true)
                .updatedBook(bookMapper.toDto(book))
                .build();
    }

    private static NameNormalizationResultDto unchanged(Book book, String before, String after) {
        return NameNormalizationResultDto.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .before(before)
                .after(after)
                .changed(false)
                .success(true)
                .build();
    }

    private static NameNormalizationResultDto missing(Long id) {
        return NameNormalizationResultDto.builder()
                .bookId(id)
                .changed(false)
                .success(false)
                .errorMessage("Book not found")
                .build();
    }
}
