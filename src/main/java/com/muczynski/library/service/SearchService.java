/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PageInfoDto;
import com.muczynski.library.dto.SearchResponseDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final LocalDateTime UNUSED_MOST_RECENT_CUTOFF = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime UNMATCHABLE_MOST_RECENT_CUTOFF = LocalDateTime.of(9999, 12, 31, 0, 0);
    private static final List<Long> NO_TEMP_TITLE_IDS = List.of(-1L);
    private static final List<Long> NO_FAVORITE_IDS = List.of(-1L);
    private static final List<ReadingDifficulty> UNUSED_READING_DIFFICULTIES = List.of(ReadingDifficulty.UNSET);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorMapper authorMapper;

    @Autowired
    private FavoriteService favoriteService;

    /**
     * Search books and authors with AND-combined type filters.
     * A book must satisfy ALL active type filters (not any one of them).
     * Selected status values OR together; when none are selected, WITHDRAWN and REQUESTED stay hidden.
     *
     * @param query          title search text (empty = match all)
     * @param bookPage       zero-based page number for book results
     * @param authorPage     zero-based page number for author results
     * @param size           results per page
     * @param filterInLibrary legacy: Active in-library when {@code statusFilters} is empty
     * @param filterElectronic legacy: Active electronic-resource when {@code statusFilters} is empty
     * @param filterFreeText  limit to books with a free online text URL
     * @param filterAudio     limit to books whose free text URL contains "librivox"
     * @param filterMostRecent limit to books added on the most recent day UTC, or temp-title regex
     * @param filterWithoutLoc legacy: without-loc status when {@code statusFilters} is empty
     * @param filterThreeLetterLoc limit to locNumbers starting with three uppercase letters
     * @param filterWithoutGrokipedia limit to books with no grokipedia URL
     * @param filterWithGrokipedia limit to books with a grokipedia URL
     * @param filterWithoutGenres limit to books with no genre tags
     * @param filterNotActiveStatus legacy: Lost/Withdrawn/On Order/Requested when {@code statusFilters} is empty
     * @param filterWithoutFreeTextUrls limit to books with no free text URL
     * @param filterYdlAudio limit to books with YDL audio
     * @param filterYdlBook limit to books with YDL paper
     * @param filterYdlEbook limit to books with YDL ebook
     * @param filterEmuAudio limit to books with EMU audio
     * @param filterEmuBook limit to books with EMU paper
     * @param filterEmuEbook limit to books with EMU ebook
     * @param filterAclaAudio limit to books with ACLA audio
     * @param filterAclaBook limit to books with ACLA paper
     * @param filterAclaEbook limit to books with ACLA ebook
     * @param labels          label tags that books must ALL have (null/empty = no label filter)
     * @param statusFilters   selected status-filter values; a book matches ANY of them (OR).
     *                        null/empty falls back to the legacy boolean flags, then the default hide.
     * @param readingDifficulties selected reading-difficulty values; a book matches ANY of them (OR).
     *                        null/empty = no reading-difficulty filter. Unset also matches null or blank.
     */
    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, int bookPageNumber, int authorPageNumber, int size,
            boolean filterInLibrary, boolean filterElectronic,
            boolean filterFreeText, boolean filterAudio,
            boolean filterMostRecent, boolean filterWithoutLoc,
            boolean filterThreeLetterLoc, boolean filterWithoutGrokipedia,
            boolean filterWithoutGenres, boolean filterNotActiveStatus,
            boolean filterWithoutFreeTextUrls,
            boolean filterYdlAudio, boolean filterYdlBook, boolean filterYdlEbook,
            boolean filterEmuAudio, boolean filterEmuBook, boolean filterEmuEbook,
            boolean filterAclaAudio, boolean filterAclaBook, boolean filterAclaEbook,
            boolean filterWithGrokipedia,
            List<String> labels,
            List<BookStatusFilter> statusFilters,
            List<ReadingDifficulty> readingDifficulties,
            Long userId,
            List<String> favoriteLists) {

        String trimmedQuery = (query == null) ? "" : query.trim();
        Pageable bookPageable = PageRequest.of(bookPageNumber, size);
        Pageable authorPageable = PageRequest.of(authorPageNumber, size);
        boolean hasLabels = labels != null && !labels.isEmpty();
        long labelCount = hasLabels ? labels.size() : 0;
        boolean hasReadingDifficulties = readingDifficulties != null && !readingDifficulties.isEmpty();
        boolean includeUnsetReadingDifficulty = hasReadingDifficulties
                && readingDifficulties.contains(ReadingDifficulty.UNSET);
        List<ReadingDifficulty> readingDifficultyParam = hasReadingDifficulties
                ? readingDifficulties
                : UNUSED_READING_DIFFICULTIES;

        LocalDateTime mostRecentCutoff = UNUSED_MOST_RECENT_CUTOFF;
        List<Long> mostRecentTempTitleIds = NO_TEMP_TITLE_IDS;
        if (filterMostRecent) {
            LocalDateTime max = bookRepository.findMaxDateAddedToLibrary();
            mostRecentCutoff = (max != null)
                    ? max.toLocalDate().minusDays(1).atStartOfDay()
                    : UNMATCHABLE_MOST_RECENT_CUTOFF;
            List<Long> tempIds = bookRepository.findBookIdsWithTemporaryTitles();
            mostRecentTempTitleIds = (tempIds == null || tempIds.isEmpty()) ? NO_TEMP_TITLE_IDS : tempIds;
        }

        boolean hasFavoriteLists = userId != null && favoriteLists != null && !favoriteLists.isEmpty();
        FavoriteService.FavoriteIdSets favoriteIds = hasFavoriteLists
                ? favoriteService.resolveIds(userId, favoriteLists)
                : new FavoriteService.FavoriteIdSets(List.of(), List.of());
        boolean filterFavoriteBooks = hasFavoriteLists;
        boolean filterFavoriteAuthors = hasFavoriteLists;
        List<Long> favoriteBookIds = favoriteIds.bookIds().isEmpty() ? NO_FAVORITE_IDS : favoriteIds.bookIds();
        List<Long> favoriteAuthorIds = favoriteIds.authorIds().isEmpty() ? NO_FAVORITE_IDS : favoriteIds.authorIds();

        boolean hasStatusFilters = statusFilters != null && !statusFilters.isEmpty();
        boolean statusInLibrary = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.IN_LIBRARY) : filterInLibrary;
        boolean statusElectronic = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.ELECTRONIC_RESOURCE) : filterElectronic;
        boolean statusLost = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.LOST) : filterNotActiveStatus;
        boolean statusWithdrawn = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.WITHDRAWN) : filterNotActiveStatus;
        boolean statusOnOrder = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.ON_ORDER) : filterNotActiveStatus;
        boolean statusRequested = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.REQUESTED) : filterNotActiveStatus;
        boolean statusWithoutLoc = hasStatusFilters
                ? statusFilters.contains(BookStatusFilter.WITHOUT_LOC) : filterWithoutLoc;
        boolean hasStatusConstraint = statusInLibrary || statusElectronic || statusWithoutLoc
                || statusLost || statusWithdrawn || statusOnOrder || statusRequested;

        Page<Book> bookPage;
        if (hasLabels) {
            bookPage = bookRepository.findWithFiltersAndLabels(
                    trimmedQuery, statusInLibrary, statusElectronic, filterFreeText, filterAudio,
                    filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                    statusWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                    filterWithoutGenres, statusLost, statusWithdrawn, statusOnOrder, statusRequested,
                    filterWithoutFreeTextUrls,
                    filterYdlAudio, filterYdlBook, filterYdlEbook,
                    filterEmuAudio, filterEmuBook, filterEmuEbook,
                    filterAclaAudio, filterAclaBook, filterAclaEbook,
                    filterWithGrokipedia,
                    labels, labelCount,
                    hasReadingDifficulties, readingDifficultyParam, includeUnsetReadingDifficulty,
                    filterFavoriteBooks, favoriteBookIds,
                    bookPageable);
        } else {
            bookPage = bookRepository.findWithFilters(
                    trimmedQuery, statusInLibrary, statusElectronic, filterFreeText, filterAudio,
                    filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                    statusWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                    filterWithoutGenres, statusLost, statusWithdrawn, statusOnOrder, statusRequested,
                    filterWithoutFreeTextUrls,
                    filterYdlAudio, filterYdlBook, filterYdlEbook,
                    filterEmuAudio, filterEmuBook, filterEmuEbook,
                    filterAclaAudio, filterAclaBook, filterAclaEbook,
                    filterWithGrokipedia,
                    hasReadingDifficulties, readingDifficultyParam, includeUnsetReadingDifficulty,
                    filterFavoriteBooks, favoriteBookIds,
                    bookPageable);
        }

        // Optional chips and labels switch authors to "authors of matching books".
        // Default withdrawn/requested hide always applies to the book WHERE, including
        // those author-of-books queries, but does not by itself switch away from name search.
        boolean hasBookFilters = hasStatusConstraint || filterFreeText || filterAudio
                || filterMostRecent || filterThreeLetterLoc
                || filterWithoutGrokipedia || filterWithGrokipedia || filterWithoutGenres
                || filterWithoutFreeTextUrls
                || filterYdlAudio || filterYdlBook || filterYdlEbook
                || filterEmuAudio || filterEmuBook || filterEmuEbook
                || filterAclaAudio || filterAclaBook || filterAclaEbook
                || hasLabels || hasReadingDifficulties;
        Page<Author> authorPage;
        if (hasBookFilters) {
            if (hasLabels) {
                authorPage = authorRepository.findAuthorsOfBooksMatchingFiltersAndLabels(
                        trimmedQuery, statusInLibrary, statusElectronic, filterFreeText, filterAudio,
                        filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                        statusWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                        filterWithoutGenres, statusLost, statusWithdrawn, statusOnOrder, statusRequested,
                        filterWithoutFreeTextUrls,
                        filterYdlAudio, filterYdlBook, filterYdlEbook,
                        filterEmuAudio, filterEmuBook, filterEmuEbook,
                        filterAclaAudio, filterAclaBook, filterAclaEbook,
                        filterWithGrokipedia,
                        labels, labelCount,
                        hasReadingDifficulties, readingDifficultyParam, includeUnsetReadingDifficulty,
                        filterFavoriteBooks, favoriteBookIds, filterFavoriteAuthors, favoriteAuthorIds,
                        authorPageable);
            } else {
                authorPage = authorRepository.findAuthorsOfBooksMatchingFilters(
                        trimmedQuery, statusInLibrary, statusElectronic, filterFreeText, filterAudio,
                        filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                        statusWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                        filterWithoutGenres, statusLost, statusWithdrawn, statusOnOrder, statusRequested,
                        filterWithoutFreeTextUrls,
                        filterYdlAudio, filterYdlBook, filterYdlEbook,
                        filterEmuAudio, filterEmuBook, filterEmuEbook,
                        filterAclaAudio, filterAclaBook, filterAclaEbook,
                        filterWithGrokipedia,
                        hasReadingDifficulties, readingDifficultyParam, includeUnsetReadingDifficulty,
                        filterFavoriteBooks, favoriteBookIds, filterFavoriteAuthors, favoriteAuthorIds,
                        authorPageable);
            }
        } else if (hasFavoriteLists) {
            authorPage = authorRepository.findByIdsAndNameContaining(
                    favoriteAuthorIds, trimmedQuery, authorPageable);
        } else if (!trimmedQuery.isEmpty()) {
            authorPage = authorRepository.findByNameContainingIgnoreCase(trimmedQuery, authorPageable);
        } else {
            authorPage = authorRepository.findAll(authorPageable);
        }

        List<BookDto> books = bookPage.getContent().stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
        List<AuthorDto> authors = authorPage.getContent().stream()
                .map(authorMapper::toDto)
                .collect(Collectors.toList());

        PageInfoDto bookPageInfo = new PageInfoDto(
                bookPage.getTotalPages(),
                bookPage.getTotalElements(),
                bookPage.getNumber(),
                bookPage.getSize());

        PageInfoDto authorPageInfo = new PageInfoDto(
                authorPage.getTotalPages(),
                authorPage.getTotalElements(),
                authorPage.getNumber(),
                authorPage.getSize());

        return new SearchResponseDto(books, authors, bookPageInfo, authorPageInfo);
    }
}
