/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.AuthorSummaryDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.util.CanonicalAuthorName;
import com.muczynski.library.util.ChicagoTitleCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-side catalog filters for the Books, Prices, and Authors pages.
 * Search already filters in the database. These list pages used to download
 * every summary and filter in the browser (issue #358, first half).
 */
@Service
public class CatalogFilterService {

    /** Matches the Books page PROPER_TEXT_MIN_CHARS. */
    static final int PROPER_TEXT_MIN_CHARS = 400;

    private static final int DEFAULT_PRICE_OLDER_DAYS = 90;
    private static final int ID_CHUNK = 500;
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime UNUSED_MOST_RECENT_CUTOFF = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime UNMATCHABLE_MOST_RECENT_CUTOFF = LocalDateTime.of(9999, 12, 31, 0, 0);
    private static final List<Long> NO_IDS = List.of(-1L);
    private static final List<Integer> NO_DESIRE = List.of(-1);
    private static final List<ReadingDifficulty> UNUSED_READING_DIFFICULTIES = List.of(ReadingDifficulty.UNSET);
    private static final List<BookCoverType> UNUSED_BINDINGS = List.of(BookCoverType.UNKNOWN);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private FavoriteService favoriteService;

    /**
     * True when a field is missing or shorter than {@link #PROPER_TEXT_MIN_CHARS}
     * after trim. A book fails the "without proper plot or description" filter
     * only when both fields are proper.
     */
    static boolean isProperText(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && trimmed.length() >= PROPER_TEXT_MIN_CHARS;
    }

    static boolean lacksProperPlotOrDescription(String plot, String description) {
        return !(isProperText(plot) && isProperText(description));
    }

    @Transactional(readOnly = true)
    public List<BookSummaryDto> bookSummaries(BookCatalogFilter filter, Long userId, boolean includeRequested) {
        String query = filter.query == null ? "" : filter.query.trim();
        boolean hasLabels = filter.labels != null && !filter.labels.isEmpty();
        long labelCount = hasLabels ? filter.labels.size() : 0;
        List<String> labels = hasLabels ? filter.labels : List.of("");

        boolean hasReading = filter.readingDifficulties != null && !filter.readingDifficulties.isEmpty();
        boolean includeUnsetReading = hasReading && filter.readingDifficulties.contains(ReadingDifficulty.UNSET);
        List<ReadingDifficulty> readingParam = hasReading ? filter.readingDifficulties : UNUSED_READING_DIFFICULTIES;

        boolean hasBinding = filter.bindings != null && !filter.bindings.isEmpty();
        boolean includeUnknownBinding = hasBinding && filter.bindings.contains(BookCoverType.UNKNOWN);
        List<BookCoverType> bindingParam = hasBinding ? filter.bindings : UNUSED_BINDINGS;

        boolean hasDesire = (filter.desireValues != null && !filter.desireValues.isEmpty()) || filter.desireUnset;
        List<Integer> desireParam = filter.desireValues == null || filter.desireValues.isEmpty()
                ? NO_DESIRE
                : filter.desireValues;

        LocalDateTime mostRecentCutoff = UNUSED_MOST_RECENT_CUTOFF;
        List<Long> tempTitleIds = NO_IDS;
        if (filter.mostRecent) {
            LocalDateTime max = bookRepository.findMaxDateAddedToLibrary();
            mostRecentCutoff = max != null
                    ? max.toLocalDate().minusDays(1).atStartOfDay()
                    : UNMATCHABLE_MOST_RECENT_CUTOFF;
            List<Long> tempIds = bookRepository.findBookIdsWithTemporaryTitles();
            tempTitleIds = tempIds == null || tempIds.isEmpty() ? NO_IDS : tempIds;
        }

        boolean filterFavorites = false;
        List<Long> favoriteBookIds = NO_IDS;
        if (userId != null && filter.favoriteLists != null && !filter.favoriteLists.isEmpty()) {
            List<Long> resolved = favoriteService.resolveIds(userId, filter.favoriteLists).bookIds();
            if (resolved != null && !resolved.isEmpty()) {
                filterFavorites = true;
                favoriteBookIds = resolved;
            }
        }

        List<BookStatusFilter> statuses = filter.statuses == null ? List.of() : filter.statuses;
        boolean hasStatus = !statuses.isEmpty();
        boolean inLibrary = hasStatus && statuses.contains(BookStatusFilter.IN_LIBRARY);
        boolean electronic = hasStatus && statuses.contains(BookStatusFilter.ELECTRONIC_RESOURCE);
        boolean withoutLoc = hasStatus && statuses.contains(BookStatusFilter.WITHOUT_LOC);
        boolean lost = hasStatus && statuses.contains(BookStatusFilter.LOST);
        boolean withdrawn = hasStatus && statuses.contains(BookStatusFilter.WITHDRAWN);
        boolean onOrder = hasStatus && statuses.contains(BookStatusFilter.ON_ORDER);
        boolean requested = hasStatus && statuses.contains(BookStatusFilter.REQUESTED);

        int days = filter.priceOlderDays > 0 ? filter.priceOlderDays : DEFAULT_PRICE_OLDER_DAYS;
        LocalDateTime priceOlderCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(days);

        List<BookRepository.DatedBookSummaryProjection> rows = hasLabels
                ? bookRepository.findFilteredSummariesByAllLabels(
                query, inLibrary, electronic, filter.freeText, filter.audio,
                filter.mostRecent, mostRecentCutoff, tempTitleIds,
                withoutLoc, false, filter.withoutGrokipedia, filter.withoutGenres,
                lost, withdrawn, onOrder, requested, filter.withoutFreeTextUrls,
                filter.ydlAudio, filter.ydlBook, filter.ydlEbook,
                filter.emuAudio, filter.emuBook, filter.emuEbook,
                filter.aclaAudio, filter.aclaBook, filter.aclaEbook,
                filter.withGrokipedia, labels, labelCount,
                hasReading, readingParam, includeUnsetReading,
                filterFavorites, favoriteBookIds,
                hasBinding, bindingParam, includeUnknownBinding,
                hasDesire, desireParam, filter.desireUnset,
                !includeRequested,
                filter.withPrices, filter.noPrices, filter.priceOlder, priceOlderCutoff, EPOCH,
                filter.lookupErrors)
                : bookRepository.findFilteredSummaries(
                query, inLibrary, electronic, filter.freeText, filter.audio,
                filter.mostRecent, mostRecentCutoff, tempTitleIds,
                withoutLoc, false, filter.withoutGrokipedia, filter.withoutGenres,
                lost, withdrawn, onOrder, requested, filter.withoutFreeTextUrls,
                filter.ydlAudio, filter.ydlBook, filter.ydlEbook,
                filter.emuAudio, filter.emuBook, filter.emuEbook,
                filter.aclaAudio, filter.aclaBook, filter.aclaEbook,
                filter.withGrokipedia,
                hasReading, readingParam, includeUnsetReading,
                filterFavorites, favoriteBookIds,
                hasBinding, bindingParam, includeUnknownBinding,
                hasDesire, desireParam, filter.desireUnset,
                !includeRequested,
                filter.withPrices, filter.noPrices, filter.priceOlder, priceOlderCutoff, EPOCH,
                filter.lookupErrors);

        if (filter.withoutProperPlotOrDescription && !rows.isEmpty()) {
            Set<Long> keep = plotIdsToKeep(rows.stream().map(BookRepository.BookSummaryProjection::getId).toList());
            rows = rows.stream().filter(row -> keep.contains(row.getId())).toList();
        }
        if ((filter.titleNotChicago || filter.authorNotCanonical) && !rows.isEmpty()) {
            Set<Long> keep = namingIdsToKeep(
                    rows.stream().map(BookRepository.BookSummaryProjection::getId).toList(),
                    filter.titleNotChicago,
                    filter.authorNotCanonical);
            rows = rows.stream().filter(row -> keep.contains(row.getId())).toList();
        }
        return rows.stream().map(this::toBookSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorSummaryDto> authorSummaries(AuthorCatalogFilter filter, Long userId) {
        String query = filter.query == null ? "" : filter.query.trim();
        boolean filterFavorites = false;
        List<Long> favoriteAuthorIds = NO_IDS;
        if (userId != null && filter.favoriteLists != null && !filter.favoriteLists.isEmpty()) {
            List<Long> resolved = favoriteService.resolveIds(userId, filter.favoriteLists).authorIds();
            if (resolved != null && !resolved.isEmpty()) {
                filterFavorites = true;
                favoriteAuthorIds = resolved;
            }
        }

        List<Long> mostRecentIds = NO_IDS;
        if (filter.mostRecent) {
            List<Long> resolved = mostRecentAuthorIds();
            mostRecentIds = resolved.isEmpty() ? NO_IDS : resolved;
        }

        List<AuthorRepository.NamedAuthorSummaryProjection> rows = authorRepository.findFilteredSummaries(
                filter.mostRecent, mostRecentIds,
                filter.withoutDescription,
                filter.withoutGrokipedia, filter.withGrokipedia,
                filter.zeroBooks,
                filter.withoutPhotos, filter.withPhotos,
                filter.withoutBirthDate, filter.withoutDeathDate,
                filterFavorites, favoriteAuthorIds,
                filter.ydlBook, filter.ydlEbook, filter.ydlAudio,
                filter.emuBook, filter.emuEbook, filter.emuAudio,
                filter.aclaBook, filter.aclaEbook, filter.aclaAudio,
                query);
        if (filter.notCanonical) {
            rows = rows.stream()
                    .filter(row -> CanonicalAuthorName.needsWork(row.getName()))
                    .toList();
        }
        return rows.stream().map(this::toAuthorSummary).toList();
    }

    private List<Long> mostRecentAuthorIds() {
        LocalDateTime max = bookRepository.findMaxDateAddedToLibrary();
        if (max == null) {
            return List.of();
        }
        LocalDateTime start = max.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Long> ids = bookRepository.findAuthorIdsAddedBetween(start, end);
        return ids == null ? List.of() : ids;
    }

    private Set<Long> plotIdsToKeep(List<Long> ids) {
        Set<Long> keep = new HashSet<>();
        for (int offset = 0; offset < ids.size(); offset += ID_CHUNK) {
            List<Long> chunk = ids.subList(offset, Math.min(offset + ID_CHUNK, ids.size()));
            for (Object[] row : bookRepository.findPlotFieldsByIds(chunk)) {
                Long id = ((Number) row[0]).longValue();
                String plot = row[1] == null ? null : row[1].toString();
                String description = row[2] == null ? null : row[2].toString();
                if (lacksProperPlotOrDescription(plot, description)) {
                    keep.add(id);
                }
            }
        }
        return keep;
    }

    /**
     * Keeps books whose title or author fails the naming rules. When both
     * chips are on, a book must fail both (the chips AND together).
     */
    private Set<Long> namingIdsToKeep(List<Long> ids, boolean titleNotChicago, boolean authorNotCanonical) {
        Set<Long> keep = new HashSet<>();
        for (int offset = 0; offset < ids.size(); offset += ID_CHUNK) {
            List<Long> chunk = ids.subList(offset, Math.min(offset + ID_CHUNK, ids.size()));
            for (Object[] row : bookRepository.findTitleAndAuthorByIds(chunk)) {
                Long id = ((Number) row[0]).longValue();
                String title = row[1] == null ? null : row[1].toString();
                String author = row[2] == null ? null : row[2].toString();
                boolean titleBad = titleNotChicago && ChicagoTitleCase.needsWork(title);
                boolean authorBad = authorNotCanonical && CanonicalAuthorName.needsWork(author);
                if (titleNotChicago && authorNotCanonical) {
                    if (titleBad && authorBad) {
                        keep.add(id);
                    }
                } else if (titleBad || authorBad) {
                    keep.add(id);
                }
            }
        }
        return keep;
    }

    private BookSummaryDto toBookSummary(BookRepository.DatedBookSummaryProjection projection) {
        BookSummaryDto dto = new BookSummaryDto();
        dto.setId(projection.getId());
        dto.setLastModified(projection.getLastModified());
        dto.setDateAddedToLibrary(projection.getDateAddedToLibrary());
        return dto;
    }

    private AuthorSummaryDto toAuthorSummary(AuthorRepository.NamedAuthorSummaryProjection projection) {
        AuthorSummaryDto dto = new AuthorSummaryDto();
        dto.setId(projection.getId());
        dto.setLastModified(projection.getLastModified());
        dto.setName(projection.getName());
        return dto;
    }

    /** Parsed Books/Prices catalog filter. Empty lists mean "no constraint". */
    public static final class BookCatalogFilter {
        public String query = "";
        public boolean freeText;
        public boolean audio;
        public boolean mostRecent;
        public boolean withoutGrokipedia;
        public boolean withGrokipedia;
        public boolean withoutGenres;
        public boolean withoutFreeTextUrls;
        public boolean withoutProperPlotOrDescription;
        public boolean titleNotChicago;
        public boolean authorNotCanonical;
        public boolean ydlAudio;
        public boolean ydlBook;
        public boolean ydlEbook;
        public boolean emuAudio;
        public boolean emuBook;
        public boolean emuEbook;
        public boolean aclaAudio;
        public boolean aclaBook;
        public boolean aclaEbook;
        public boolean withPrices;
        public boolean noPrices;
        public boolean priceOlder;
        public boolean lookupErrors;
        public int priceOlderDays = DEFAULT_PRICE_OLDER_DAYS;
        public List<String> labels = List.of();
        public List<BookStatusFilter> statuses = List.of();
        public List<ReadingDifficulty> readingDifficulties = List.of();
        public List<BookCoverType> bindings = List.of();
        public List<Integer> desireValues = List.of();
        public boolean desireUnset;
        public List<String> favoriteLists = List.of();
    }

    /** Parsed Authors page filter. */
    public static final class AuthorCatalogFilter {
        /** Case-insensitive partial match on the name or an alternate name. Blank matches every author. */
        public String query = "";
        public boolean mostRecent;
        public boolean withoutDescription;
        public boolean withoutGrokipedia;
        public boolean withGrokipedia;
        public boolean zeroBooks;
        public boolean withoutPhotos;
        public boolean withPhotos;
        public boolean withoutBirthDate;
        public boolean withoutDeathDate;
        /** Author name is comma-inverted, has years, expandable initials, a catalog note, or uniform case. */
        public boolean notCanonical;
        public boolean ydlBook;
        public boolean ydlEbook;
        public boolean ydlAudio;
        public boolean emuBook;
        public boolean emuEbook;
        public boolean emuAudio;
        public boolean aclaBook;
        public boolean aclaEbook;
        public boolean aclaAudio;
        public List<String> favoriteLists = List.of();
    }
}
