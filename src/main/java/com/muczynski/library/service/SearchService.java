/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
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

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorMapper authorMapper;

    /**
     * Search books and authors with AND-combined type filters.
     * A book must satisfy ALL active type filters (not any one of them).
     * notActiveStatus always constrains books: off hides WITHDRAWN; on excludes ACTIVE.
     *
     * @param query          title search text (empty = match all)
     * @param page           zero-based page number
     * @param size           results per page
     * @param filterInLibrary limit to books with a LOC call number (physical collection)
     * @param filterElectronic limit to books with electronicResource = true
     * @param filterFreeText  limit to books with a free online text URL
     * @param filterAudio     limit to books whose free text URL contains "librivox"
     * @param filterMostRecent limit to books added on the most recent day UTC, or temp-title regex
     * @param filterWithoutLoc limit to books with no LOC call number
     * @param filterThreeLetterLoc limit to locNumbers starting with three uppercase letters
     * @param filterWithoutGrokipedia limit to books with no grokipedia URL
     * @param filterWithGrokipedia limit to books with a grokipedia URL
     * @param filterWithoutGenres limit to books with no genre tags
     * @param filterNotActiveStatus when true, only non-ACTIVE statuses; when false, hide WITHDRAWN
     * @param filterWithoutFreeTextUrls limit to books with no free text URL
     * @param filterYdlAudio limit to books with YDL audio
     * @param filterYdlBook limit to books with YDL paper
     * @param filterYdlEbook limit to books with YDL ebook
     * @param filterEmuAudio limit to books with EMU audio
     * @param filterEmuBook limit to books with EMU paper
     * @param filterEmuEbook limit to books with EMU ebook
     * @param labels          label tags that books must ALL have (null/empty = no label filter)
     */
    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, int page, int size,
            boolean filterInLibrary, boolean filterElectronic,
            boolean filterFreeText, boolean filterAudio,
            boolean filterMostRecent, boolean filterWithoutLoc,
            boolean filterThreeLetterLoc, boolean filterWithoutGrokipedia,
            boolean filterWithoutGenres, boolean filterNotActiveStatus,
            boolean filterWithoutFreeTextUrls,
            boolean filterYdlAudio, boolean filterYdlBook, boolean filterYdlEbook,
            boolean filterEmuAudio, boolean filterEmuBook, boolean filterEmuEbook,
            boolean filterWithGrokipedia,
            List<String> labels) {

        String trimmedQuery = (query == null) ? "" : query.trim();
        Pageable pageable = PageRequest.of(page, size);
        boolean hasLabels = labels != null && !labels.isEmpty();
        long labelCount = hasLabels ? labels.size() : 0;

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

        Page<Book> bookPage;
        if (hasLabels) {
            bookPage = bookRepository.findWithFiltersAndLabels(
                    trimmedQuery, filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                    filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                    filterWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                    filterWithoutGenres, filterNotActiveStatus, filterWithoutFreeTextUrls,
                    filterYdlAudio, filterYdlBook, filterYdlEbook,
                    filterEmuAudio, filterEmuBook, filterEmuEbook,
                    filterWithGrokipedia,
                    labels, labelCount, pageable);
        } else {
            bookPage = bookRepository.findWithFilters(
                    trimmedQuery, filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                    filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                    filterWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                    filterWithoutGenres, filterNotActiveStatus, filterWithoutFreeTextUrls,
                    filterYdlAudio, filterYdlBook, filterYdlEbook,
                    filterEmuAudio, filterEmuBook, filterEmuEbook,
                    filterWithGrokipedia,
                    pageable);
        }

        // Optional chips and labels switch authors to "authors of matching books".
        // Withdrawn hide (notActiveStatus off) always applies to the book WHERE, including
        // those author-of-books queries, but does not by itself switch away from name search.
        boolean hasFilters = filterInLibrary || filterElectronic || filterFreeText || filterAudio
                || filterMostRecent || filterWithoutLoc || filterThreeLetterLoc
                || filterWithoutGrokipedia || filterWithGrokipedia || filterWithoutGenres || filterNotActiveStatus
                || filterWithoutFreeTextUrls
                || filterYdlAudio || filterYdlBook || filterYdlEbook
                || filterEmuAudio || filterEmuBook || filterEmuEbook
                || hasLabels;
        Page<Author> authorPage;
        if (hasFilters) {
            if (hasLabels) {
                authorPage = authorRepository.findAuthorsOfBooksMatchingFiltersAndLabels(
                        trimmedQuery, filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                        filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                        filterWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                        filterWithoutGenres, filterNotActiveStatus, filterWithoutFreeTextUrls,
                        filterYdlAudio, filterYdlBook, filterYdlEbook,
                        filterEmuAudio, filterEmuBook, filterEmuEbook,
                        filterWithGrokipedia,
                        labels, labelCount, pageable);
            } else {
                authorPage = authorRepository.findAuthorsOfBooksMatchingFilters(
                        trimmedQuery, filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                        filterMostRecent, mostRecentCutoff, mostRecentTempTitleIds,
                        filterWithoutLoc, filterThreeLetterLoc, filterWithoutGrokipedia,
                        filterWithoutGenres, filterNotActiveStatus, filterWithoutFreeTextUrls,
                        filterYdlAudio, filterYdlBook, filterYdlEbook,
                        filterEmuAudio, filterEmuBook, filterEmuEbook,
                        filterWithGrokipedia,
                        pageable);
            }
        } else if (!trimmedQuery.isEmpty()) {
            authorPage = authorRepository.findByNameContainingIgnoreCase(trimmedQuery, pageable);
        } else {
            authorPage = authorRepository.findAll(pageable);
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
