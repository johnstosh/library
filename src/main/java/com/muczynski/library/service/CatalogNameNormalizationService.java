/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AuthorNameNormalizationResultDto;
import com.muczynski.library.dto.NameNormalizationResultDto;
import com.muczynski.library.mapper.AuthorMapper;
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
 * <p>
 * The Authors page renames selected author rows in place. When the canonical
 * name already belongs to another author, that author is kept and the selected
 * author is merged into it and deleted. The old form is not stored as an
 * alternate name.
 */
@Service
public class CatalogNameNormalizationService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorMerger authorMerger;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorMapper authorMapper;

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

    /**
     * Rewrites selected author rows into canonical form. The managed author is
     * renamed in place so its photos stay. When the canonical name already
     * belongs to another author, that author is kept and this one is merged
     * into it and deleted.
     */
    @Transactional
    public List<AuthorNameNormalizationResultDto> normalizeAuthorNames(List<Long> authorIds) {
        List<Long> ids = distinctIds(authorIds);
        Map<Long, Author> authors = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Author author : authorRepository.findAllById(ids)) {
                authors.put(author.getId(), author);
            }
        }

        Map<Long, String> originalNames = new HashMap<>();
        Map<Long, String> desiredById = new HashMap<>();
        List<Author> candidates = new ArrayList<>();
        for (Long id : ids) {
            Author author = authors.get(id);
            if (author == null) {
                continue;
            }
            String original = author.getName();
            originalNames.put(id, original);
            String desired = CanonicalAuthorName.canonical(original);
            if (desired == null || desired.isBlank()) {
                desired = original;
            }
            desiredById.put(id, desired);
            if (desired != null && !desired.equals(original)) {
                candidates.add(author);
            }
        }
        candidates.sort(Comparator.comparing(Author::getId));

        Set<Long> renamed = new HashSet<>();
        Set<Long> mergedAway = new HashSet<>();
        Map<Long, Long> mergedInto = new HashMap<>();
        Map<String, List<Author>> groups = new LinkedHashMap<>();
        for (Author author : candidates) {
            groups.computeIfAbsent(desiredById.get(author.getId()), key -> new ArrayList<>()).add(author);
        }
        for (Map.Entry<String, List<Author>> entry : groups.entrySet()) {
            String desired = entry.getKey();
            List<Author> sources = new ArrayList<>(entry.getValue());
            Author occupant = authorRepository.findAllByNameOrderByIdAsc(desired).stream().findFirst().orElse(null);
            Author keeper;
            if (occupant != null) {
                keeper = occupant;
            } else {
                keeper = sources.remove(0);
                if (!desired.equals(keeper.getName())) {
                    keeper.setName(desired);
                    renamed.add(keeper.getId());
                }
            }
            if (sources.isEmpty()) {
                if (renamed.contains(keeper.getId())) {
                    authorRepository.saveAndFlush(keeper);
                }
                continue;
            }
            for (Author source : sources) {
                authorMerger.merge(keeper, source);
                mergedAway.add(source.getId());
                mergedInto.put(source.getId(), keeper.getId());
            }
        }

        List<AuthorNameNormalizationResultDto> results = new ArrayList<>();
        for (Long id : ids) {
            Author author = authors.get(id);
            if (author == null) {
                results.add(AuthorNameNormalizationResultDto.builder()
                        .authorId(id)
                        .changed(false)
                        .success(false)
                        .errorMessage("Author not found")
                        .build());
                continue;
            }
            String before = originalNames.get(id);
            String after = desiredById.getOrDefault(id, before);
            if (mergedAway.contains(id)) {
                results.add(AuthorNameNormalizationResultDto.builder()
                        .authorId(id)
                        .name(after)
                        .before(before)
                        .after(after)
                        .changed(true)
                        .success(true)
                        .mergedIntoAuthorId(mergedInto.get(id))
                        .build());
            } else if (renamed.contains(id)) {
                results.add(AuthorNameNormalizationResultDto.builder()
                        .authorId(id)
                        .name(author.getName())
                        .before(before)
                        .after(after)
                        .changed(true)
                        .success(true)
                        .updatedAuthor(authorMapper.toDto(author))
                        .build());
            } else {
                results.add(AuthorNameNormalizationResultDto.builder()
                        .authorId(id)
                        .name(before == null ? "" : before)
                        .before(before == null ? "" : before)
                        .after(after == null ? "" : after)
                        .changed(false)
                        .success(true)
                        .build());
            }
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
