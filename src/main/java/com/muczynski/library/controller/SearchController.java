/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.SearchResponseDto;
import com.muczynski.library.service.SearchService;
import com.muczynski.library.util.FavoriteListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<SearchResponseDto> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer bookPage,
            @RequestParam(required = false) Integer authorPage,
            @RequestParam int size,
            @RequestParam(defaultValue = "false") boolean filterInLibrary,
            @RequestParam(defaultValue = "false") boolean filterElectronic,
            @RequestParam(defaultValue = "false") boolean filterFreeText,
            @RequestParam(defaultValue = "false") boolean filterAudio,
            @RequestParam(defaultValue = "false") boolean filterMostRecent,
            @RequestParam(defaultValue = "false") boolean filterWithoutLoc,
            @RequestParam(defaultValue = "false") boolean filterThreeLetterLoc,
            @RequestParam(defaultValue = "false") boolean filterWithoutGrokipedia,
            @RequestParam(defaultValue = "false") boolean filterWithGrokipedia,
            @RequestParam(defaultValue = "false") boolean filterWithoutGenres,
            @RequestParam(defaultValue = "false") boolean filterNotActiveStatus,
            @RequestParam(defaultValue = "false") boolean filterWithoutFreeTextUrls,
            @RequestParam(defaultValue = "false") boolean filterYdlAudio,
            @RequestParam(defaultValue = "false") boolean filterYdlBook,
            @RequestParam(defaultValue = "false") boolean filterYdlEbook,
            @RequestParam(defaultValue = "false") boolean filterEmuAudio,
            @RequestParam(defaultValue = "false") boolean filterEmuBook,
            @RequestParam(defaultValue = "false") boolean filterEmuEbook,
            @RequestParam(defaultValue = "false") boolean filterAclaAudio,
            @RequestParam(defaultValue = "false") boolean filterAclaBook,
            @RequestParam(defaultValue = "false") boolean filterAclaEbook,
            @RequestParam(defaultValue = "false") boolean filterWithoutYdlAudio,
            @RequestParam(defaultValue = "false") boolean filterWithoutYdlBook,
            @RequestParam(defaultValue = "false") boolean filterWithoutYdlEbook,
            @RequestParam(defaultValue = "false") boolean filterWithoutEmuAudio,
            @RequestParam(defaultValue = "false") boolean filterWithoutEmuBook,
            @RequestParam(defaultValue = "false") boolean filterWithoutEmuEbook,
            @RequestParam(defaultValue = "false") boolean filterWithoutAclaAudio,
            @RequestParam(defaultValue = "false") boolean filterWithoutAclaBook,
            @RequestParam(defaultValue = "false") boolean filterWithoutAclaEbook,
            @RequestParam(defaultValue = "false") boolean filterWccAudio,
            @RequestParam(defaultValue = "false") boolean filterWccBook,
            @RequestParam(defaultValue = "false") boolean filterWccEbook,
            @RequestParam(defaultValue = "false") boolean filterWithoutWccAudio,
            @RequestParam(defaultValue = "false") boolean filterWithoutWccBook,
            @RequestParam(defaultValue = "false") boolean filterWithoutWccEbook,
            @RequestParam(defaultValue = "false") boolean filterMsuAudio,
            @RequestParam(defaultValue = "false") boolean filterMsuBook,
            @RequestParam(defaultValue = "false") boolean filterMsuEbook,
            @RequestParam(defaultValue = "false") boolean filterWithoutMsuAudio,
            @RequestParam(defaultValue = "false") boolean filterWithoutMsuBook,
            @RequestParam(defaultValue = "false") boolean filterWithoutMsuEbook,
            @RequestParam(required = false) String labels,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String readingDifficulty,
            @RequestParam(required = false) String favoriteLists,
            @RequestParam(required = false) String libraryCheckedOlder,
            @RequestParam(defaultValue = "90") int libraryCheckedOlderDays,
            @RequestParam(required = false) String libraryLookupError,
            Principal principal) {
        try {
            List<String> labelList = (labels == null || labels.isBlank())
                    ? null
                    : Arrays.stream(labels.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
            List<BookStatusFilter> statusList = BookStatusFilter.parseFilterValues(status);
            if (statusList.isEmpty()) {
                statusList = null;
            }
            List<ReadingDifficulty> readingDifficultyList = ReadingDifficulty.parseFilterValues(readingDifficulty);
            if (readingDifficultyList.isEmpty()) {
                readingDifficultyList = null;
            }
            List<String> parsedFavorites = FavoriteListParams.split(favoriteLists);
            List<String> favoriteList = parsedFavorites.isEmpty() ? null : parsedFavorites;
            Long userId = null;
            if (principal != null && principal.getName() != null) {
                try {
                    userId = Long.parseLong(principal.getName());
                } catch (NumberFormatException ignored) {
                    userId = null;
                }
            }
            int resolvedBookPage = bookPage != null ? bookPage : (page != null ? page : 0);
            int resolvedAuthorPage = authorPage != null ? authorPage : (page != null ? page : 0);
            SearchResponseDto results = searchService.search(query, resolvedBookPage, resolvedAuthorPage, size,
                    filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                    filterMostRecent, filterWithoutLoc, filterThreeLetterLoc,
                    filterWithoutGrokipedia, filterWithoutGenres, filterNotActiveStatus,
                    filterWithoutFreeTextUrls,
                    filterYdlAudio, filterYdlBook, filterYdlEbook,
                    filterEmuAudio, filterEmuBook, filterEmuEbook,
                    filterAclaAudio, filterAclaBook, filterAclaEbook,
                    filterWithGrokipedia,
                    filterWithoutYdlAudio, filterWithoutYdlBook, filterWithoutYdlEbook,
                    filterWithoutEmuAudio, filterWithoutEmuBook, filterWithoutEmuEbook,
                    filterWithoutAclaAudio, filterWithoutAclaBook, filterWithoutAclaEbook,
                    filterWccAudio, filterWccBook, filterWccEbook,
                    filterWithoutWccAudio, filterWithoutWccBook, filterWithoutWccEbook,
                    filterMsuAudio, filterMsuBook, filterMsuEbook,
                    filterWithoutMsuAudio, filterWithoutMsuBook, filterWithoutMsuEbook,
                    labelList,
                    statusList,
                    readingDifficultyList,
                    userId,
                    favoriteList,
                    libraryCheckedOlder,
                    libraryCheckedOlderDays,
                    libraryLookupError);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.warn("Failed to perform search with query '{}', bookPage {}, authorPage {}, size {}: {}",
                    query, bookPage, authorPage, size, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
