/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.AuthorNames;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.CatalogMaintenanceAction;
import com.muczynski.library.domain.RejectedMatches;
import com.muczynski.library.domain.RejectedUrl;
import com.muczynski.library.dto.CatalogToolApplyResultDto;
import com.muczynski.library.dto.CatalogToolCandidatesDto;
import com.muczynski.library.dto.CatalogToolChangeDto;
import com.muczynski.library.dto.CatalogToolPreviewDto;
import com.muczynski.library.dto.CatalogToolSnapshotDto;
import com.muczynski.library.dto.CatalogToolUndoableDto;
import com.muczynski.library.freetext.FreeTextLookupCache;
import com.muczynski.library.freetext.FreeTextLookupService;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.CatalogMaintenanceActionRepository;
import com.muczynski.library.util.ChicagoTitleCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Locale;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Management copies of Quick Grokipedia lookup, Chicago title case, and
 * free-text lookup (Issue #386). Chicago and Grokipedia: Recalc lists every
 * change and saves nothing, Do It writes that list, Undo puts the previous
 * values back. Free text searches a capped oldest-first prefix and stores each
 * finished book immediately. Each tool covers every book, including withdrawn
 * copies. Grokipedia and free text only add. They do not replace or remove a
 * stored value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogToolMaintenanceService {

    static final int GROKIPEDIA_BATCH_LIMIT = 25;
    static final int FREE_TEXT_BATCH_LIMIT = 10;
    /** Session sizes for the oldest-first free-text search. */
    public static final Set<Integer> FREE_TEXT_LIMITS = Set.of(100, 200, 400, 800);
    private static final int APPLY_LIMIT = 20000;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CatalogMaintenanceActionRepository actionRepository;
    private final GrokipediaLookupService grokipediaLookupService;
    private final FreeTextLookupService freeTextLookupService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CatalogToolPreviewDto previewChicago() {
        List<BookRepository.TitleMaintenanceProjection> rows = bookRepository.findAllTitlesForMaintenance();
        Set<String> claimed = new HashSet<>();
        for (BookRepository.TitleMaintenanceProjection row : rows) {
            if (row.getTitle() != null) {
                claimed.add(row.getTitle());
            }
        }
        List<CatalogToolChangeDto> changes = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (BookRepository.TitleMaintenanceProjection row : rows) {
            String before = row.getTitle();
            if (!ChicagoTitleCase.needsWork(before)) {
                continue;
            }
            String after = ChicagoTitleCase.toChicago(before);
            if (after == null || after.isBlank() || after.equals(before)) {
                continue;
            }
            if (claimed.contains(after)) {
                skipped.add(CatalogToolChangeDto.builder()
                        .bookId(row.getId())
                        .title(before)
                        .before(before)
                        .after(after)
                        .reason("Chicago form \"" + after + "\" is already used by another book")
                        .build());
                continue;
            }
            claimed.remove(before);
            claimed.add(after);
            changes.add(CatalogToolChangeDto.builder()
                    .bookId(row.getId())
                    .title(before)
                    .before(before)
                    .after(after)
                    .build());
        }
        return preview(rows.size(), changes, skipped, chicagoPreviewMessage(changes.size(), skipped.size()));
    }

    @Transactional(readOnly = true)
    public CatalogToolCandidatesDto grokipediaCandidateIds() {
        return CatalogToolCandidatesDto.builder()
                .bookIds(bookRepository.findAllIdsOrderById())
                .build();
    }

    @Transactional(readOnly = true)
    public CatalogToolCandidatesDto grokipediaAuthorCandidateIds() {
        return CatalogToolCandidatesDto.builder()
                .authorIds(authorRepository.findAllIdsOrderById())
                .build();
    }

    /**
     * Oldest free-text searches first, with never-searched books before any stored time.
     * Blank and temporary primary titles are dropped before the limit is applied.
     * Alternate titles do not affect the order.
     */
    @Transactional(readOnly = true)
    public CatalogToolCandidatesDto freeTextCandidateIds(int limit) {
        if (!FREE_TEXT_LIMITS.contains(limit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search 100, 200, 400, or 800 books.");
        }
        List<Long> ids = new ArrayList<>();
        for (BookRepository.FreeTextCandidateRow row : bookRepository.findFreeTextCandidates()) {
            String title = row.getTitle();
            if (title == null || title.isBlank() || BooksFromFeedService.isTemporaryTitle(title)) {
                continue;
            }
            ids.add(row.getId());
            if (ids.size() >= limit) {
                break;
            }
        }
        return CatalogToolCandidatesDto.builder().bookIds(ids).build();
    }

    @Transactional(readOnly = true)
    public CatalogToolPreviewDto previewGrokipedia(List<Long> bookIds) {
        List<Long> ids = distinctIds(bookIds);
        if (ids.isEmpty()) {
            return preview(0, List.of(), List.of(), "No books in this batch.");
        }
        if (ids.size() > GROKIPEDIA_BATCH_LIMIT) {
            return errorPreview("Preview at most " + GROKIPEDIA_BATCH_LIMIT + " books at a time.");
        }
        List<CatalogToolChangeDto> changes = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (Long bookId : ids) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) {
                skipped.add(missing(bookId));
                continue;
            }
            String existing = book.getGrokipediaUrl();
            CatalogToolChangeDto change = grokipediaAddition(
                    book.getId(), null, book.getTitle(), existing, Book.titlesToSearch(book),
                    book.getInvalidGrokipediaUrls());
            if (change != null) {
                changes.add(change);
            }
        }
        return preview(ids.size(), changes, skipped, countMessage(
                changes.size(), "book would gain a Grokipedia link.", "books would gain a Grokipedia link."));
    }

    @Transactional(readOnly = true)
    public CatalogToolPreviewDto previewGrokipediaAuthors(List<Long> authorIds) {
        List<Long> ids = distinctIds(authorIds);
        if (ids.isEmpty()) {
            return preview(0, List.of(), List.of(), "No authors in this batch.");
        }
        if (ids.size() > GROKIPEDIA_BATCH_LIMIT) {
            return errorPreview("Preview at most " + GROKIPEDIA_BATCH_LIMIT + " authors at a time.");
        }
        List<CatalogToolChangeDto> changes = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (Long authorId : ids) {
            Author author = authorRepository.findById(authorId).orElse(null);
            if (author == null) {
                skipped.add(missingAuthor(authorId));
                continue;
            }
            String existing = author.getGrokipediaUrl();
            CatalogToolChangeDto change = grokipediaAddition(
                    null, author.getId(), author.getName(), existing, Author.namesToSearch(author),
                    author.getInvalidGrokipediaUrls());
            if (change != null) {
                changes.add(change);
            }
        }
        return preview(ids.size(), changes, skipped, countMessage(
                changes.size(), "author would gain a Grokipedia link.", "authors would gain a Grokipedia link."));
    }

    /**
     * Search one small batch and store each finished book before the next.
     * Runs outside a transaction so provider HTTP does not hold a connection.
     * Each write goes through {@link FreeTextLookupService#recordFreeTextSearch}
     * in its own transaction. A blank or temporary primary title is skipped
     * and not stamped.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CatalogToolPreviewDto searchFreeText(List<Long> bookIds) {
        List<Long> ids = distinctIds(bookIds);
        if (ids.isEmpty()) {
            return preview(0, List.of(), List.of(), "No books in this batch.");
        }
        if (ids.size() > FREE_TEXT_BATCH_LIMIT) {
            return errorPreview("Search at most " + FREE_TEXT_BATCH_LIMIT + " books at a time.");
        }
        Map<Long, BookRepository.FreeTextSearchRow> rows = new LinkedHashMap<>();
        for (BookRepository.FreeTextSearchRow row : bookRepository.findFreeTextSearchRows(ids)) {
            rows.putIfAbsent(row.getId(), row);
        }
        List<CatalogToolChangeDto> changes = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (Long bookId : ids) {
            BookRepository.FreeTextSearchRow row = rows.get(bookId);
            if (row == null) {
                skipped.add(missing(bookId));
                continue;
            }
            String title = row.getTitle();
            if (title == null || title.isBlank() || BooksFromFeedService.isTemporaryTitle(title)) {
                continue;
            }
            Book book = bookForFreeTextSearch(row);
            List<String> titles = Book.titlesToSearch(book);
            if (titles.isEmpty()) {
                continue;
            }
            String existing = row.getFreeTextUrl();
            try {
                FreeTextLookupService.Discovery discovery = freeTextLookupService.discoverUrls(
                        titles, AuthorNames.lookupNames(book.getAuthor()));
                String found = discovery.getUrls();
                if (found == null || found.isBlank()) {
                    freeTextLookupService.recordFreeTextSearch(bookId, null, false);
                    continue;
                }
                Book stored = bookRepository.findById(bookId).orElse(null);
                boolean audio = discovery.getProviderName() != null
                        && discovery.getProviderName().toLowerCase(Locale.ROOT).contains("librivox");
                String allowed = stored == null ? found : RejectedMatches.withoutRejectedFreeText(
                        found, stored.getInvalidFreeTextUrls(), stored.getInvalidFreeAudioUrls(), audio);
                if (allowed == null || allowed.isBlank()) {
                    freeTextLookupService.recordFreeTextSearch(bookId, null, false);
                    continue;
                }
                String merged = FreeTextLookupCache.mergeUrls(existing, allowed);
                String added = addedUrls(existing, merged);
                if (sameText(merged, existing) || added.isBlank()) {
                    freeTextLookupService.recordFreeTextSearch(bookId, null, false);
                    continue;
                }
                freeTextLookupService.recordFreeTextSearch(bookId, merged, true);
                changes.add(CatalogToolChangeDto.builder()
                        .bookId(bookId)
                        .title(title)
                        .before(existing == null ? "" : existing)
                        .after(merged)
                        .added(added)
                        .build());
            } catch (RuntimeException e) {
                log.warn("Free-text search failed for book {}: {}", bookId, e.getMessage());
                try {
                    freeTextLookupService.recordFreeTextSearch(bookId, null, false);
                } catch (RuntimeException stampError) {
                    log.warn("Could not stamp free-text search for book {}: {}", bookId, stampError.getMessage());
                }
                skipped.add(CatalogToolChangeDto.builder()
                        .bookId(bookId)
                        .title(title)
                        .reason("Lookup failed: " + e.getMessage())
                        .build());
            }
        }
        return preview(ids.size(), changes, skipped, countMessage(
                changes.size(), "book gained a free-text link.", "books gained a free-text link."));
    }

    /**
     * Transient book and author built from the search projection.
     * {@link Book#titlesToSearch} and {@link AuthorNames#lookupNames} read these
     * fields. The entities are never saved.
     */
    private static Book bookForFreeTextSearch(BookRepository.FreeTextSearchRow row) {
        Book book = new Book();
        book.setId(row.getId());
        book.setTitle(row.getTitle());
        book.setAlternateTitles(row.getAlternateTitles());
        book.setFreeTextUrl(row.getFreeTextUrl());
        if (row.getAuthorName() != null || (row.getAuthorAlternateNames() != null && !row.getAuthorAlternateNames().isEmpty())) {
            Author author = new Author();
            author.setName(row.getAuthorName());
            author.setAlternateNames(row.getAuthorAlternateNames());
            book.setAuthor(author);
        }
        return book;
    }

    @Transactional
    public CatalogToolApplyResultDto applyChicago(List<CatalogToolChangeDto> requested) {
        List<CatalogToolChangeDto> changes = limit(requested);
        if (changes == null) {
            return errorApply("Do It accepts at most " + APPLY_LIMIT + " changes at a time.");
        }
        if (changes.isEmpty()) {
            return nothingToDo();
        }
        List<CatalogToolChangeDto> applied = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        Set<String> claimed = new HashSet<>();
        for (CatalogToolChangeDto change : changes) {
            if (change == null || change.getBookId() == null) {
                continue;
            }
            Book book = bookRepository.findById(change.getBookId()).orElse(null);
            if (book == null) {
                skipped.add(missing(change.getBookId()));
                continue;
            }
            String current = book.getTitle();
            String before = change.getBefore();
            String after = change.getAfter();
            if (!sameText(current, before)) {
                skipped.add(skip(book, "Title changed since Recalc"));
                continue;
            }
            String chicago = ChicagoTitleCase.toChicago(before);
            if (chicago == null || !chicago.equals(after) || chicago.equals(current)) {
                skipped.add(skip(book, "Title would not change"));
                continue;
            }
            if (claimed.contains(after) || bookRepository.existsByTitleAndIdNot(after, book.getId())) {
                skipped.add(skip(book, "Chicago form \"" + after + "\" is already used by another book"));
                continue;
            }
            book.setTitle(after);
            bookRepository.saveAndFlush(book);
            claimed.add(after);
            applied.add(CatalogToolChangeDto.builder()
                    .bookId(book.getId())
                    .title(before)
                    .before(before)
                    .after(after)
                    .build());
        }
        return finish(CatalogMaintenanceAction.CHICAGO_TITLES, applied, skipped,
                applyMessage("Rewrote", applied.size(), "title", "titles", "into Chicago title case.", skipped.size()));
    }

    @Transactional
    public CatalogToolApplyResultDto applyGrokipedia(List<CatalogToolChangeDto> requested) {
        List<CatalogToolChangeDto> changes = limit(requested);
        if (changes == null) {
            return errorApply("Do It accepts at most " + APPLY_LIMIT + " changes at a time.");
        }
        if (changes.isEmpty()) {
            return nothingToDo();
        }
        List<CatalogToolChangeDto> applied = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (CatalogToolChangeDto change : changes) {
            if (change == null || change.getBookId() == null) {
                continue;
            }
            Book book = bookRepository.findById(change.getBookId()).orElse(null);
            if (book == null) {
                skipped.add(missing(change.getBookId()));
                continue;
            }
            String current = book.getGrokipediaUrl();
            String after = RejectedMatches.withoutRejectedGrokipedia(change.getAfter(), book.getInvalidGrokipediaUrls());
            if (after == null || addedUrls(current, after).isBlank()) {
                skipped.add(skip(book, "Rejected Grokipedia page"));
                continue;
            }
            String reason = grokipediaRejectReason(current, change.getBefore(), after);
            if (reason != null) {
                skipped.add(skip(book, reason));
                continue;
            }
            book.setGrokipediaUrl(after);
            bookRepository.save(book);
            applied.add(CatalogToolChangeDto.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .before(current)
                    .after(after)
                    .added(addedUrls(current, after))
                    .build());
        }
        return finish(CatalogMaintenanceAction.GROKIPEDIA_URLS, applied, skipped,
                applyMessage("Added Grokipedia links on", applied.size(), "book", "books", ".", skipped.size()));
    }

    @Transactional
    public CatalogToolApplyResultDto applyGrokipediaAuthors(List<CatalogToolChangeDto> requested) {
        List<CatalogToolChangeDto> changes = limit(requested);
        if (changes == null) {
            return errorApply("Do It accepts at most " + APPLY_LIMIT + " changes at a time.");
        }
        if (changes.isEmpty()) {
            return nothingToDo();
        }
        List<CatalogToolChangeDto> applied = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (CatalogToolChangeDto change : changes) {
            if (change == null || change.getAuthorId() == null) {
                continue;
            }
            Author author = authorRepository.findById(change.getAuthorId()).orElse(null);
            if (author == null) {
                skipped.add(missingAuthor(change.getAuthorId()));
                continue;
            }
            String current = author.getGrokipediaUrl();
            String after = RejectedMatches.withoutRejectedGrokipedia(
                    change.getAfter(), author.getInvalidGrokipediaUrls());
            if (after == null || addedUrls(current, after).isBlank()) {
                skipped.add(skipAuthor(author, "Rejected Grokipedia page"));
                continue;
            }
            String reason = grokipediaRejectReason(current, change.getBefore(), after);
            if (reason != null) {
                skipped.add(skipAuthor(author, reason));
                continue;
            }
            author.setGrokipediaUrl(after);
            authorRepository.save(author);
            applied.add(CatalogToolChangeDto.builder()
                    .authorId(author.getId())
                    .title(author.getName())
                    .before(current)
                    .after(after)
                    .added(addedUrls(current, after))
                    .build());
        }
        return finish(CatalogMaintenanceAction.GROKIPEDIA_AUTHOR_URLS, applied, skipped,
                applyMessage("Added Grokipedia links on", applied.size(), "author", "authors", ".", skipped.size()));
    }

    @Transactional
    public CatalogToolApplyResultDto undoChicago(Long actionId) {
        return undo(actionId, CatalogMaintenanceAction.CHICAGO_TITLES, CatalogMaintenanceAction.UNDO_CHICAGO_TITLES,
                (book, change) -> {
                    if (!sameText(book.getTitle(), change.getAfter())) {
                        return "Title is no longer \"" + change.getAfter() + "\"";
                    }
                    String previous = change.getBefore();
                    if (previous == null || previous.isBlank()) {
                        return "Previous title was blank";
                    }
                    if (bookRepository.existsByTitleAndIdNot(previous, book.getId())) {
                        return "Previous title \"" + previous + "\" is already used by another book";
                    }
                    book.setTitle(previous);
                    bookRepository.saveAndFlush(book);
                    return null;
                });
    }

    @Transactional
    public CatalogToolApplyResultDto undoGrokipedia(Long actionId) {
        return undo(actionId, CatalogMaintenanceAction.GROKIPEDIA_URLS, CatalogMaintenanceAction.UNDO_GROKIPEDIA_URLS,
                (book, change) -> {
                    if (!sameText(book.getGrokipediaUrl(), change.getAfter())) {
                        return "URL is no longer the one that was added";
                    }
                    book.setGrokipediaUrl(storedGrokipedia(change.getBefore()));
                    bookRepository.save(book);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public CatalogToolUndoableDto undoableChicago() {
        return undoable(CatalogMaintenanceAction.CHICAGO_TITLES);
    }

    @Transactional
    public CatalogToolApplyResultDto undoGrokipediaAuthors(Long actionId) {
        return undoAuthors(actionId, CatalogMaintenanceAction.GROKIPEDIA_AUTHOR_URLS,
                CatalogMaintenanceAction.UNDO_GROKIPEDIA_AUTHOR_URLS);
    }

    @Transactional(readOnly = true)
    public CatalogToolUndoableDto undoableGrokipedia() {
        return undoable(CatalogMaintenanceAction.GROKIPEDIA_URLS);
    }

    @Transactional(readOnly = true)
    public CatalogToolUndoableDto undoableGrokipediaAuthors() {
        return undoable(CatalogMaintenanceAction.GROKIPEDIA_AUTHOR_URLS);
    }

    private CatalogToolApplyResultDto undo(Long actionId, String forwardKind, String undoKind, Restorer restorer) {
        if (actionId == null) {
            return errorApply("Missing action.");
        }
        CatalogMaintenanceAction action = actionRepository.findById(actionId).orElse(null);
        if (action == null || !forwardKind.equals(action.getActionKind())) {
            return errorApply("That action cannot be undone.");
        }
        if (actionRepository.existsByReversesActionId(actionId)) {
            return errorApply("Already undone.");
        }
        CatalogToolSnapshotDto snapshot = read(action);
        List<CatalogToolChangeDto> applied = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (CatalogToolChangeDto change : snapshot.getChanges()) {
            if (change == null || change.getBookId() == null) {
                continue;
            }
            Book book = bookRepository.findById(change.getBookId()).orElse(null);
            if (book == null) {
                skipped.add(missing(change.getBookId()));
                continue;
            }
            String reason = restorer.restore(book, change);
            if (reason != null) {
                CatalogToolChangeDto skip = skip(book, reason);
                skip.setBefore(change.getBefore());
                skip.setAfter(change.getAfter());
                skipped.add(skip);
                continue;
            }
            applied.add(CatalogToolChangeDto.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .before(change.getAfter())
                    .after(change.getBefore())
                    .build());
        }
        if (applied.isEmpty()) {
            return CatalogToolApplyResultDto.builder()
                    .applied(0)
                    .skipped(skipped.size())
                    .message(skipped.isEmpty() ? "Nothing to undo." : "Nothing was restored.")
                    .changes(List.of())
                    .skippedChanges(skipped)
                    .build();
        }
        String summary = restoredSummary(applied.size(), skipped.size(), "book", "books");
        Long id = record(undoKind, summary, applied, actionId);
        return CatalogToolApplyResultDto.builder()
                .actionId(id)
                .actionKind(undoKind)
                .applied(applied.size())
                .skipped(skipped.size())
                .message(summary)
                .changes(applied)
                .skippedChanges(skipped)
                .build();
    }

    private CatalogToolApplyResultDto undoAuthors(Long actionId, String forwardKind, String undoKind) {
        if (actionId == null) {
            return errorApply("Missing action.");
        }
        CatalogMaintenanceAction action = actionRepository.findById(actionId).orElse(null);
        if (action == null || !forwardKind.equals(action.getActionKind())) {
            return errorApply("That action cannot be undone.");
        }
        if (actionRepository.existsByReversesActionId(actionId)) {
            return errorApply("Already undone.");
        }
        CatalogToolSnapshotDto snapshot = read(action);
        List<CatalogToolChangeDto> applied = new ArrayList<>();
        List<CatalogToolChangeDto> skipped = new ArrayList<>();
        for (CatalogToolChangeDto change : snapshot.getChanges()) {
            if (change == null || change.getAuthorId() == null) {
                continue;
            }
            Author author = authorRepository.findById(change.getAuthorId()).orElse(null);
            if (author == null) {
                skipped.add(missingAuthor(change.getAuthorId()));
                continue;
            }
            if (!sameText(author.getGrokipediaUrl(), change.getAfter())) {
                CatalogToolChangeDto skip = skipAuthor(author, "URL is no longer the one that was added");
                skip.setBefore(change.getBefore());
                skip.setAfter(change.getAfter());
                skipped.add(skip);
                continue;
            }
            author.setGrokipediaUrl(storedGrokipedia(change.getBefore()));
            authorRepository.save(author);
            applied.add(CatalogToolChangeDto.builder()
                    .authorId(author.getId())
                    .title(author.getName())
                    .before(change.getAfter())
                    .after(change.getBefore())
                    .build());
        }
        if (applied.isEmpty()) {
            return CatalogToolApplyResultDto.builder()
                    .applied(0)
                    .skipped(skipped.size())
                    .message(skipped.isEmpty() ? "Nothing to undo." : "Nothing was restored.")
                    .changes(List.of())
                    .skippedChanges(skipped)
                    .build();
        }
        String summary = restoredSummary(applied.size(), skipped.size(), "author", "authors");
        Long id = record(undoKind, summary, applied, actionId);
        return CatalogToolApplyResultDto.builder()
                .actionId(id)
                .actionKind(undoKind)
                .applied(applied.size())
                .skipped(skipped.size())
                .message(summary)
                .changes(applied)
                .skippedChanges(skipped)
                .build();
    }

    private static String restoredSummary(int restored, int leftAlone, String one, String many) {
        String summary = "Restored " + restored + " " + (restored == 1 ? one : many) + ".";
        if (leftAlone > 0) {
            summary += " " + leftAlone + (leftAlone == 1 ? " " + one + " was" : " " + many + " were") + " left alone.";
        }
        return summary;
    }

    /**
     * A Grokipedia page for one of {@code names} that is not already stored.
     * Reads the names before the lookup so the HTTP call does not keep a database session.
     */
    private CatalogToolChangeDto grokipediaAddition(Long bookId, Long authorId, String label,
                                                     String existing, List<String> names,
                                                     List<RejectedUrl> rejected) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        List<String> found = grokipediaLookupService.findQuickUrls(names, rejected);
        if (found.isEmpty()) {
            return null;
        }
        String merged = FreeTextLookupCache.mergeUrls(existing, String.join(" ", found));
        if (sameText(merged, existing)) {
            return null;
        }
        String added = addedUrls(existing, merged);
        if (added.isBlank()) {
            return null;
        }
        return CatalogToolChangeDto.builder()
                .bookId(bookId)
                .authorId(authorId)
                .title(label)
                .before(existing == null ? "" : existing)
                .after(merged)
                .added(added)
                .build();
    }

    /**
     * @return a skip reason, or null when {@code after} only adds Grokipedia page URLs
     */
    private static String grokipediaRejectReason(String current, String before, String after) {
        if (!sameText(current, before)) {
            return "Links changed since Recalc";
        }
        if (!addsOnly(current, after)) {
            return "Would replace or remove an existing link";
        }
        List<String> known = FreeTextLookupCache.extractUrls(current);
        for (String url : FreeTextLookupCache.extractUrls(after)) {
            boolean already = false;
            for (String existing : known) {
                if (existing.equalsIgnoreCase(url)) {
                    already = true;
                    break;
                }
            }
            if (!already && !GrokipediaLookupService.isGrokipediaPageUrl(url)) {
                return "Not a Grokipedia page URL";
            }
        }
        return null;
    }

    private CatalogToolUndoableDto undoable(String kind) {
        List<CatalogMaintenanceAction> rows = actionRepository.findUndoable(kind, PageRequest.of(0, 1));
        if (rows.isEmpty()) {
            return CatalogToolUndoableDto.builder().build();
        }
        CatalogMaintenanceAction action = rows.get(0);
        int count = 0;
        try {
            count = read(action).getChanges().size();
        } catch (RuntimeException e) {
            log.warn("Could not read undo snapshot {}: {}", action.getId(), e.getMessage());
        }
        return CatalogToolUndoableDto.builder()
                .actionId(action.getId())
                .changeCount(count)
                .summary(action.getSummary())
                .build();
    }

    private CatalogToolApplyResultDto finish(String kind, List<CatalogToolChangeDto> applied,
                                              List<CatalogToolChangeDto> skipped, String summary) {
        Long actionId = null;
        if (!applied.isEmpty()) {
            actionId = record(kind, summary, applied, null);
        }
        return CatalogToolApplyResultDto.builder()
                .actionId(actionId)
                .actionKind(actionId == null ? null : kind)
                .applied(applied.size())
                .skipped(skipped.size())
                .message(summary)
                .changes(applied)
                .skippedChanges(skipped)
                .build();
    }

    private Long record(String kind, String summary, List<CatalogToolChangeDto> applied, Long reversesActionId) {
        CatalogMaintenanceAction row = new CatalogMaintenanceAction();
        row.setActionKind(kind);
        row.setSummary(summary);
        row.setReversesActionId(reversesActionId);
        try {
            row.setSnapshotJson(objectMapper.writeValueAsString(new CatalogToolSnapshotDto(applied)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store the undo snapshot", e);
        }
        return actionRepository.save(row).getId();
    }

    private CatalogToolSnapshotDto read(CatalogMaintenanceAction action) {
        try {
            CatalogToolSnapshotDto snapshot = objectMapper.readValue(
                    action.getSnapshotJson(), CatalogToolSnapshotDto.class);
            if (snapshot == null || snapshot.getChanges() == null) {
                return new CatalogToolSnapshotDto(List.of());
            }
            return snapshot;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read the undo snapshot", e);
        }
    }

    private static List<CatalogToolChangeDto> limit(List<CatalogToolChangeDto> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        if (requested.size() > APPLY_LIMIT) {
            return null;
        }
        return requested;
    }

    private static List<Long> distinctIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Long id : bookIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }

    static boolean missingGrokipediaUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        return "-".equals(url.trim());
    }

    private static String storedGrokipedia(String before) {
        if (before == null || before.isBlank()) {
            return null;
        }
        return before;
    }

    private static boolean addsOnly(String current, String after) {
        if (after == null || after.isBlank()) {
            return false;
        }
        if (!FreeTextLookupCache.urlsLostIfReplaced(current, after).isEmpty()) {
            return false;
        }
        return sameText(FreeTextLookupCache.mergeUrls(current, after), after);
    }

    private static String addedUrls(String before, String after) {
        List<String> known = FreeTextLookupCache.extractUrls(before);
        List<String> extra = new ArrayList<>();
        for (String url : FreeTextLookupCache.extractUrls(after)) {
            boolean already = false;
            for (String existing : known) {
                if (existing.equalsIgnoreCase(url)) {
                    already = true;
                    break;
                }
            }
            if (!already) {
                extra.add(url);
            }
        }
        return String.join(" ", extra);
    }

    private static boolean sameText(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equals(b);
    }

    private static CatalogToolPreviewDto preview(int scanned, List<CatalogToolChangeDto> changes,
                                                  List<CatalogToolChangeDto> skipped, String message) {
        return CatalogToolPreviewDto.builder()
                .booksScanned(scanned)
                .changeCount(changes.size())
                .message(message)
                .changes(changes)
                .skipped(skipped)
                .build();
    }

    private static CatalogToolPreviewDto errorPreview(String error) {
        return CatalogToolPreviewDto.builder()
                .booksScanned(0)
                .changeCount(0)
                .error(error)
                .message(error)
                .changes(List.of())
                .skipped(List.of())
                .build();
    }

    private static CatalogToolApplyResultDto nothingToDo() {
        return CatalogToolApplyResultDto.builder()
                .applied(0)
                .skipped(0)
                .message("Nothing to do.")
                .changes(List.of())
                .skippedChanges(List.of())
                .build();
    }

    private static CatalogToolApplyResultDto errorApply(String error) {
        return CatalogToolApplyResultDto.builder()
                .applied(0)
                .skipped(0)
                .error(error)
                .message(error)
                .changes(List.of())
                .skippedChanges(List.of())
                .build();
    }

    private static CatalogToolChangeDto missing(Long bookId) {
        return CatalogToolChangeDto.builder()
                .bookId(bookId)
                .reason("Book not found")
                .build();
    }

    private static CatalogToolChangeDto missingAuthor(Long authorId) {
        return CatalogToolChangeDto.builder()
                .authorId(authorId)
                .reason("Author not found")
                .build();
    }

    private static CatalogToolChangeDto skipAuthor(Author author, String reason) {
        return CatalogToolChangeDto.builder()
                .authorId(author.getId())
                .title(author.getName())
                .reason(reason)
                .build();
    }

    private static CatalogToolChangeDto skip(Book book, String reason) {
        return CatalogToolChangeDto.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .reason(reason)
                .build();
    }

    private static String chicagoPreviewMessage(int changes, int skipped) {
        String message = countMessage(changes, "title will change.", "titles will change.");
        if (skipped > 0) {
            message += " " + skipped + (skipped == 1 ? " title was" : " titles were")
                    + " left alone because the Chicago form is already used.";
        }
        return message;
    }

    private static String countMessage(int count, String one, String many) {
        return count + " " + (count == 1 ? one : many);
    }

    private static String applyMessage(String verb, int applied, String one, String many, String tail, int skipped) {
        String noun = applied == 1 ? one : many;
        String message = verb + " " + applied + " " + noun + (tail.startsWith(".") ? "" : " ") + tail;
        if (message.endsWith(" .")) {
            message = message.substring(0, message.length() - 2) + ".";
        }
        if (skipped > 0) {
            message += " " + skipped + (skipped == 1 ? " book was" : " books were") + " left alone.";
        }
        return message;
    }

    @FunctionalInterface
    private interface Restorer {
        /** @return a skip reason, or null when the book was restored */
        String restore(Book book, CatalogToolChangeDto change);
    }
}
