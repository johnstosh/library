/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.FreeTextBulkLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.service.BooksFromFeedService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service that orchestrates free text lookup across multiple providers.
 * Tries providers in priority order until one finds a match.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FreeTextLookupService {

    private final BookRepository bookRepository;
    private final List<FreeTextProvider> providers;

    @PostConstruct
    public void init() {
        // Sort providers by priority (lower = higher priority)
        providers.sort(Comparator.comparingInt(FreeTextProvider::getPriority));
        log.info("Initialized {} free text providers: {}",
                providers.size(),
                providers.stream().map(FreeTextProvider::getProviderName).toList());
        log.info("Free text cache contains {} authors with {} books",
                FreeTextLookupCache.getAuthorCount(),
                FreeTextLookupCache.getBookCount());
    }

    /**
     * Look up free online text for a single book.
     * First checks the global cache, then tries all providers in priority order until one finds a match.
     *
     * @param bookId the book ID to look up
     * @return result with URL if found, or error message if not
     */
    public FreeTextBulkLookupResultDto lookupBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new LibraryException("Book not found: " + bookId));

        // Skip temporary titles
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            return FreeTextBulkLookupResultDto.builder()
                    .bookId(bookId)
                    .bookTitle(book.getTitle())
                    .success(false)
                    .errorMessage("Temporary title - skipped")
                    .providersSearched(List.of())
                    .build();
        }

        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;

        // Check global cache first (may return multiple space-separated URLs)
        String cachedUrls = FreeTextLookupCache.lookup(authorName, book.getTitle());
        if (cachedUrls != null) {
            // Update the book with all cached URLs (space-separated)
            book.setFreeTextUrl(cachedUrls);
            book.setLastModified(LocalDateTime.now());
            bookRepository.save(book);

            log.info("Found free text for book {} in cache: {}", bookId, cachedUrls);

            return FreeTextBulkLookupResultDto.builder()
                    .bookId(bookId)
                    .bookTitle(book.getTitle())
                    .authorName(authorName)
                    .success(true)
                    .freeTextUrl(cachedUrls)
                    .providerName("Cache")
                    .providersSearched(List.of("Cache"))
                    .build();
        }

        List<String> searchedProviders = new ArrayList<>();

        for (FreeTextProvider provider : providers) {
            searchedProviders.add(provider.getProviderName());

            try {
                log.debug("Searching {} for book '{}' by '{}'",
                        provider.getProviderName(), book.getTitle(), authorName);

                FreeTextLookupResult result = provider.search(book.getTitle(), authorName);

                if (result.isFound()) {
                    // Update the book with the found URL
                    book.setFreeTextUrl(result.getUrl());
                    book.setLastModified(LocalDateTime.now());
                    bookRepository.save(book);

                    log.info("Found free text for book {}: {} via {}",
                            bookId, result.getUrl(), provider.getProviderName());

                    return FreeTextBulkLookupResultDto.builder()
                            .bookId(bookId)
                            .bookTitle(book.getTitle())
                            .authorName(authorName)
                            .success(true)
                            .freeTextUrl(result.getUrl())
                            .providerName(provider.getProviderName())
                            .providersSearched(searchedProviders)
                            .build();
                } else {
                    log.debug("Provider {} did not find book: {}",
                            provider.getProviderName(), result.getErrorMessage());
                }
            } catch (Exception e) {
                log.warn("Provider {} failed for book {}: {}",
                        provider.getProviderName(), bookId, e.getMessage());
            }
        }

        // No provider found a match
        log.info("No free text found for book {} '{}' after searching {} providers",
                bookId, book.getTitle(), searchedProviders.size());

        return FreeTextBulkLookupResultDto.builder()
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .authorName(authorName)
                .success(false)
                .errorMessage("Not found in any provider")
                .providersSearched(searchedProviders)
                .build();
    }

    /**
     * Look up free online text for multiple books.
     *
     * @param bookIds list of book IDs to look up
     * @return list of results for each book
     */
    public List<FreeTextBulkLookupResultDto> lookupBooks(List<Long> bookIds) {
        List<FreeTextBulkLookupResultDto> results = new ArrayList<>();

        for (Long bookId : bookIds) {
            try {
                results.add(lookupBook(bookId));
            } catch (Exception e) {
                log.error("Error looking up free text for book {}: {}", bookId, e.getMessage());
                results.add(FreeTextBulkLookupResultDto.builder()
                        .bookId(bookId)
                        .success(false)
                        .errorMessage("Error: " + e.getMessage())
                        .providersSearched(List.of())
                        .build());
            }
        }

        return results;
    }

    /**
     * Get the list of available provider names in priority order.
     *
     * @return list of provider names
     */
    public List<String> getProviderNames() {
        return providers.stream()
                .map(FreeTextProvider::getProviderName)
                .toList();
    }
}
