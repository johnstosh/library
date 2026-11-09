/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookLabels;
import com.muczynski.library.domain.CatalogMaintenanceAction;
import com.muczynski.library.dto.GenreChangeLineDto;
import com.muczynski.library.dto.GenreCleanupSnapshotDto;
import com.muczynski.library.dto.IllegalGenresMaintenanceDto;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.CatalogMaintenanceActionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for data maintenance operations, starting with illegal genre cleanup.
 * Librarian-only. Called from MaintenanceController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final BookRepository bookRepository;
    private final CatalogMaintenanceActionRepository actionRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int LIST_TITLES_UP_TO = 3;

    /**
     * Lists every genre change Recalc would make. Does not save.
     * The count is the number of books whose tags would change.
     */
    @Transactional(readOnly = true)
    public IllegalGenresMaintenanceDto countIllegalGenres() {
        Plan plan = buildPlan(bookRepository.findGenreMaintenanceRows());
        String message = plan.books.isEmpty()
                ? "All genres are clean. No books have illegal or mismatched tags."
                : plan.books.size() + " book(s) have illegal or mismatched genre tags.";
        return IllegalGenresMaintenanceDto.builder()
                .booksAffected(plan.books.size())
                .booksScanned(plan.scanned)
                .message(message)
                .changes(plan.lines)
                .build();
    }

    /**
     * Cleans illegal genres from all books in batches of ~50.
     * Records the previous tags so Undo can restore them.
     */
    @Transactional
    public IllegalGenresMaintenanceDto cleanupIllegalGenres() {
        Plan plan = buildPlan(bookRepository.findGenreMaintenanceRows());

        AtomicLong booksUpdated = new AtomicLong(0);
        AtomicLong pluralCorrections = new AtomicLong(plan.corrections);
        AtomicLong illegalRemoved = new AtomicLong(plan.removed);
        List<GenreCleanupSnapshotDto.BookTags> written = new ArrayList<>();

        int batchSize = DEFAULT_BATCH_SIZE;
        for (int i = 0; i < plan.books.size(); i += batchSize) {
            int end = Math.min(i + batchSize, plan.books.size());
            for (BookChange change : plan.books.subList(i, end)) {
                Book book = bookRepository.findById(change.bookId).orElse(null);
                if (book == null) {
                    continue;
                }
                book.setTagsList(new ArrayList<>(change.after));
                booksUpdated.incrementAndGet();
                written.add(GenreCleanupSnapshotDto.BookTags.builder()
                        .bookId(book.getId())
                        .title(change.title)
                        .before(new ArrayList<>(change.before))
                        .after(new ArrayList<>(change.after))
                        .build());
            }
            entityManager.flush();
            entityManager.clear();
        }

        Long actionId = null;
        if (!written.isEmpty()) {
            actionId = record(CatalogMaintenanceAction.GENRE_CLEANUP,
                    "Cleaned genres on " + written.size() + (written.size() == 1 ? " book." : " books."),
                    GenreCleanupSnapshotDto.builder().books(written).build(),
                    null);
        }

        long removed = illegalRemoved.get();
        String message = String.format(
                "Looked at %d books. Updated %d. Corrected %d plural or spelling %s. Removed %d %s that %s not on the standard list.",
                plan.scanned, booksUpdated.get(), pluralCorrections.get(),
                pluralCorrections.get() == 1 ? "variant" : "variants",
                removed, removed == 1 ? "tag" : "tags", removed == 1 ? "was" : "were");
        log.info("Genre maintenance completed: {}", message);

        return IllegalGenresMaintenanceDto.builder()
                .booksAffected(0)
                .booksScanned(plan.scanned)
                .booksUpdated(booksUpdated.get())
                .pluralCorrections(pluralCorrections.get())
                .illegalRemoved(illegalRemoved.get())
                .message(message)
                .actionId(actionId)
                .changes(plan.lines)
                .build();
    }

    /**
     * Puts back the tags Clean Genres replaced, when the book still has those tags.
     */
    @Transactional
    public IllegalGenresMaintenanceDto undoGenreCleanup(Long actionId) {
        if (actionId == null) {
            return error("Missing action.");
        }
        CatalogMaintenanceAction action = actionRepository.findById(actionId).orElse(null);
        if (action == null || !CatalogMaintenanceAction.GENRE_CLEANUP.equals(action.getActionKind())) {
            return error("That action cannot be undone.");
        }
        if (actionRepository.existsByReversesActionId(actionId)) {
            return error("Already undone.");
        }
        GenreCleanupSnapshotDto snapshot = read(action);
        List<GenreCleanupSnapshotDto.BookTags> restored = new ArrayList<>();
        int skipped = 0;
        for (GenreCleanupSnapshotDto.BookTags change : snapshot.getBooks()) {
            if (change == null || change.getBookId() == null) {
                continue;
            }
            Book book = bookRepository.findById(change.getBookId()).orElse(null);
            if (book == null || !sameTags(book.getTagsList(), change.getAfter())) {
                skipped++;
                continue;
            }
            book.setTagsList(change.getBefore() == null ? new ArrayList<>() : new ArrayList<>(change.getBefore()));
            restored.add(change);
        }
        if (restored.isEmpty()) {
            return IllegalGenresMaintenanceDto.builder()
                    .booksAffected(0)
                    .booksUpdated(0)
                    .message(skipped == 0 ? "Nothing to undo." : "Nothing was restored.")
                    .changes(List.of())
                    .build();
        }
        String message = "Restored " + restored.size() + (restored.size() == 1 ? " book." : " books.");
        if (skipped > 0) {
            message += " " + skipped + (skipped == 1 ? " book was" : " books were") + " left alone.";
        }
        Long undoId = record(CatalogMaintenanceAction.UNDO_GENRE_CLEANUP, message,
                GenreCleanupSnapshotDto.builder().books(restored).build(), actionId);
        return IllegalGenresMaintenanceDto.builder()
                .booksAffected(0)
                .booksUpdated(restored.size())
                .message(message)
                .actionId(undoId)
                .changes(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public IllegalGenresMaintenanceDto undoableGenreCleanup() {
        List<CatalogMaintenanceAction> rows = actionRepository.findUndoable(
                CatalogMaintenanceAction.GENRE_CLEANUP, PageRequest.of(0, 1));
        if (rows.isEmpty()) {
            return IllegalGenresMaintenanceDto.builder().build();
        }
        CatalogMaintenanceAction action = rows.get(0);
        return IllegalGenresMaintenanceDto.builder()
                .actionId(action.getId())
                .message(action.getSummary())
                .build();
    }

    private Plan buildPlan(List<Object[]> rows) {
        Map<Long, Assembled> byId = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row == null || row[0] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            String title = row[1] == null ? null : row[1].toString();
            Assembled book = byId.computeIfAbsent(id, ignored -> new Assembled(id, title));
            if (row[2] != null) {
                book.tags.add(row[2].toString());
            }
        }
        Map<String, Group> groups = new LinkedHashMap<>();
        List<BookChange> books = new ArrayList<>();
        long corrections = 0;
        long removed = 0;
        for (Assembled book : byId.values()) {
            List<String> original = book.tags;
            List<String> cleaned = BookLabels.cleanupTags(original);
            EditCounts edits = editCounts(original);
            corrections += edits.corrections;
            removed += edits.removed;
            if (sameContents(original, cleaned)) {
                continue;
            }
            String title = book.title == null || book.title.isBlank() ? "Book " + book.id : book.title;
            books.add(new BookChange(book.id, title, new ArrayList<>(original), cleaned));
            Set<String> seen = new HashSet<>();
            for (String tag : original) {
                if (tag == null || tag.isBlank()) {
                    continue;
                }
                String shown = tag.trim();
                String canonical = BookLabels.getCanonicalLabel(tag);
                String key;
                Group group;
                if (canonical == null) {
                    key = "DROP\0" + shown;
                    group = new Group("DROP", shown, null);
                } else if (!canonical.equals(shown)) {
                    key = "CHANGE\0" + shown + "\0" + canonical;
                    group = new Group("CHANGE", shown, canonical);
                } else {
                    continue;
                }
                if (!seen.add(key)) {
                    continue;
                }
                groups.computeIfAbsent(key, ignored -> group).titles.add(title);
            }
        }
        return new Plan(byId.size(), corrections, removed, books, lines(groups));
    }

    /**
     * A tag that is not on the standard list is a removal. A tag that maps to a
     * different canonical form is a spelling or plural correction. A removal is
     * not also a correction.
     */
    static EditCounts editCounts(List<String> tags) {
        long corrections = 0;
        long removed = 0;
        if (tags == null) {
            return new EditCounts(0, 0);
        }
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String canonical = BookLabels.getCanonicalLabel(tag);
            if (canonical == null) {
                removed++;
            } else if (!canonical.equals(tag.trim())) {
                corrections++;
            }
        }
        return new EditCounts(corrections, removed);
    }

    private static boolean sameContents(List<String> original, List<String> cleaned) {
        return counts(original).equals(counts(cleaned));
    }

    private static Map<String, Integer> counts(List<String> tags) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (tags == null) {
            return counts;
        }
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            counts.merge(tag, 1, Integer::sum);
        }
        return counts;
    }

    private static List<GenreChangeLineDto> lines(Map<String, Group> groups) {
        List<GenreChangeLineDto> lines = new ArrayList<>();
        for (Group group : groups.values()) {
            List<String> titles = new ArrayList<>(group.titles);
            titles.sort(String.CASE_INSENSITIVE_ORDER);
            int count = titles.size();
            String who = count <= LIST_TITLES_UP_TO ? joinTitles(titles) : count + " titles";
            boolean one = count == 1;
            String summary = "DROP".equals(group.kind)
                    ? who + (one ? " drops " : " drop ") + group.from
                    : who + (one ? " gets " : " get ") + group.from + " changed to " + group.to;
            lines.add(GenreChangeLineDto.builder()
                    .kind(group.kind)
                    .fromTag(group.from)
                    .toTag(group.to)
                    .titleCount(count)
                    .summary(summary)
                    .build());
        }
        lines.sort(Comparator
                .comparing((GenreChangeLineDto line) -> "DROP".equals(line.getKind()) ? 0 : 1)
                .thenComparing(GenreChangeLineDto::getTitleCount, Comparator.reverseOrder())
                .thenComparing(line -> line.getFromTag() == null ? "" : line.getFromTag(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(line -> line.getToTag() == null ? "" : line.getToTag(), String.CASE_INSENSITIVE_ORDER));
        return lines;
    }

    private static String joinTitles(List<String> titles) {
        if (titles.size() == 1) {
            return titles.get(0);
        }
        if (titles.size() == 2) {
            return titles.get(0) + " and " + titles.get(1);
        }
        return titles.get(0) + ", " + titles.get(1) + ", and " + titles.get(2);
    }

    private Long record(String kind, String summary, GenreCleanupSnapshotDto snapshot, Long reversesActionId) {
        CatalogMaintenanceAction action = new CatalogMaintenanceAction();
        action.setActionKind(kind);
        action.setSummary(summary);
        action.setReversesActionId(reversesActionId);
        try {
            action.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store the genre cleanup", e);
        }
        return actionRepository.save(action).getId();
    }

    private GenreCleanupSnapshotDto read(CatalogMaintenanceAction action) {
        try {
            GenreCleanupSnapshotDto snapshot = objectMapper.readValue(
                    action.getSnapshotJson(), GenreCleanupSnapshotDto.class);
            if (snapshot.getBooks() == null) {
                snapshot.setBooks(List.of());
            }
            return snapshot;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read the genre cleanup", e);
        }
    }

    private static boolean sameTags(List<String> current, List<String> expected) {
        List<String> left = current == null ? List.of() : new ArrayList<>(current);
        List<String> right = expected == null ? List.of() : new ArrayList<>(expected);
        return left.equals(right);
    }

    private static IllegalGenresMaintenanceDto error(String message) {
        return IllegalGenresMaintenanceDto.builder()
                .error(message)
                .message(message)
                .changes(List.of())
                .build();
    }

    private static final class Assembled {
        private final Long id;
        private final String title;
        private final List<String> tags = new ArrayList<>();

        private Assembled(Long id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    static final class EditCounts {
        final long corrections;
        final long removed;

        EditCounts(long corrections, long removed) {
            this.corrections = corrections;
            this.removed = removed;
        }
    }

    private static final class Group {
        private final String kind;
        private final String from;
        private final String to;
        private final Set<String> titles = new LinkedHashSet<>();

        private Group(String kind, String from, String to) {
            this.kind = kind;
            this.from = from;
            this.to = to;
        }
    }

    private static final class BookChange {
        private final Long bookId;
        private final String title;
        private final List<String> before;
        private final List<String> after;

        private BookChange(Long bookId, String title, List<String> before, List<String> after) {
            this.bookId = bookId;
            this.title = title;
            this.before = before;
            this.after = after;
        }
    }

    private static final class Plan {
        private final int scanned;
        private final long corrections;
        private final long removed;
        private final List<BookChange> books;
        private final List<GenreChangeLineDto> lines;

        private Plan(int scanned, long corrections, long removed, List<BookChange> books,
                     List<GenreChangeLineDto> lines) {
            this.scanned = scanned;
            this.corrections = corrections;
            this.removed = removed;
            this.books = books;
            this.lines = lines;
        }
    }
}
