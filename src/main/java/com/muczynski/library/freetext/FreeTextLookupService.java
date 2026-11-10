/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import com.muczynski.library.domain.AuthorNames;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.FreeTextBulkLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.service.BooksFromFeedService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

        List<String> titles = Book.titlesToSearch(book);
        if (titles.isEmpty()) {
            return FreeTextBulkLookupResultDto.builder()
                    .bookId(bookId)
                    .bookTitle(book.getTitle())
                    .success(false)
                    .errorMessage("Book has no title")
                    .providersSearched(List.of())
                    .build();
        }

        // Temporary-title skip still based on primary only
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
        String primaryTitle = book.getTitle();
        try {
            Discovery discovery = discoverUrls(titles, AuthorNames.lookupNames(book.getAuthor()));
            if (discovery.getUrls() != null && !discovery.getUrls().isBlank()) {
                String merged = applyFoundUrls(book, discovery.getUrls());
                stampSearch(book);
                log.info("Found free text for book {}: {} via {}", bookId, merged, discovery.getProviderName());
                return FreeTextBulkLookupResultDto.builder()
                        .bookId(bookId)
                        .bookTitle(primaryTitle)
                        .authorName(authorName)
                        .success(true)
                        .freeTextUrl(merged)
                        .providerName(discovery.getProviderName())
                        .providersSearched(discovery.getProvidersSearched())
                        .build();
            }

            stampSearch(book);
            log.info("No free text found for book {} '{}' after searching {} providers with {} titles",
                    bookId, primaryTitle, discovery.getProvidersSearched().size(), titles.size());

            return FreeTextBulkLookupResultDto.builder()
                    .bookId(bookId)
                    .bookTitle(primaryTitle)
                    .authorName(authorName)
                    .success(false)
                    .errorMessage("Not found in any provider")
                    .providersSearched(discovery.getProvidersSearched())
                    .build();
        } catch (RuntimeException e) {
            log.warn("Free-text lookup failed for book {}: {}", bookId, e.getMessage());
            try {
                stampSearch(book);
            } catch (RuntimeException stampError) {
                log.warn("Could not stamp free-text search for book {}: {}", bookId, stampError.getMessage());
            }
            return FreeTextBulkLookupResultDto.builder()
                    .bookId(bookId)
                    .bookTitle(primaryTitle)
                    .authorName(authorName)
                    .success(false)
                    .errorMessage("Error: " + e.getMessage())
                    .providersSearched(List.of())
                    .build();
        }
    }

    /**
     * Commit one finished maintenance search in its own transaction.
     * A URL change and the timestamp commit together. A miss stores the timestamp
     * and leaves the URL alone. Call this through the Spring proxy so
     * {@code REQUIRES_NEW} actually starts. A blank or temporary primary title
     * is not stamped.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFreeTextSearch(Long bookId, String mergedUrl, boolean urlChanged) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (urlChanged) {
            bookRepository.updateFreeTextFound(bookId, mergedUrl, now);
        } else {
            bookRepository.updateFreeTextSearched(bookId, now);
        }
    }

    /**
     * Search the cache and providers without writing the book.
     * {@link Discovery#getUrls()} is null when nothing is found.
     * A call through the Spring proxy runs outside a transaction so the HTTP
     * work does not keep a database connection. {@link #lookupBook(Long)} still
     * saves after this returns.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Discovery discoverUrls(List<String> titles, List<String> authorNames) {
        List<String> names = authorNames == null || authorNames.isEmpty()
                ? List.of((String) null)
                : authorNames;
        if (titles == null || titles.isEmpty()) {
            return new Discovery(null, null, List.of());
        }

        // Only cached URLs count. A cached "not found" must not stop a later search.
        List<String> allCached = new ArrayList<>();
        for (String title : titles) {
            for (String lookupAuthor : names) {
                String cached = FreeTextLookupCache.lookup(lookupAuthor, title);
                if (cached != null && !cached.isBlank()) {
                    allCached.add(cached);
                }
            }
        }
        String combinedCache = String.join(" ", allCached).trim();
        if (!combinedCache.isBlank()) {
            return new Discovery(combinedCache, "Cache", List.of("Cache"));
        }

        List<String> searchedProviders = new ArrayList<>();
        for (FreeTextProvider provider : providers) {
            searchedProviders.add(provider.getProviderName());
            for (String searchTitle : titles) {
                for (String lookupAuthor : names) {
                    try {
                        log.debug("Searching {} for title '{}' by '{}'",
                                provider.getProviderName(), searchTitle, lookupAuthor);
                        FreeTextLookupResult result = provider.search(searchTitle, lookupAuthor);
                        if (result != null && result.isFound()) {
                            List<String> expectedDomains = provider.getExpectedDomains();
                            if (!expectedDomains.isEmpty()) {
                                String urlDomain = extractDomain(result.getUrl());
                                boolean domainMatches = expectedDomains.stream()
                                        .anyMatch(expected -> urlDomain != null && urlDomain.contains(expected));
                                if (!domainMatches) {
                                    log.error("Provider {} returned URL with unexpected domain: {} (expected one of: {})",
                                            provider.getProviderName(), result.getUrl(), expectedDomains);
                                    continue;
                                }
                            }
                            return new Discovery(result.getUrl(), provider.getProviderName(), List.copyOf(searchedProviders));
                        }
                        log.debug("Provider {} did not find '{}': {}",
                                provider.getProviderName(), searchTitle,
                                result == null ? "no result" : result.getErrorMessage());
                    } catch (Exception e) {
                        log.warn("Provider {} failed for title '{}': {}",
                                provider.getProviderName(), searchTitle, e.getMessage());
                    }
                }
            }
        }
        return new Discovery(null, null, List.copyOf(searchedProviders));
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

    /**
     * Union {@code foundUrls} into the stored field. Does not save. The caller
     * stamps {@code freeTextLastSearched} and saves once, including a miss.
     * Existing links are never dropped.
     *
     * @return the freeTextUrl value after the merge
     */
    private String applyFoundUrls(Book book, String foundUrls) {
        String merged = FreeTextLookupCache.mergeUrls(book.getFreeTextUrl(), foundUrls);
        if (!Objects.equals(merged, book.getFreeTextUrl())) {
            book.setFreeTextUrl(merged);
            book.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        }
        return merged;
    }

    /** Record that this search finished, then save the managed book. */
    private void stampSearch(Book book) {
        book.setFreeTextLastSearched(LocalDateTime.now(ZoneOffset.UTC));
        bookRepository.save(book);
    }

    /**
     * Extract the domain from a URL.
     *
     * @param url the URL to extract domain from
     * @return the domain (e.g., "www.gutenberg.org"), or null if extraction fails
     */
    private String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            log.warn("Failed to extract domain from URL: {}", url);
            return null;
        }
    }

    /**
     * URLs found for one book before they are merged into the stored field.
     */
    public static final class Discovery {
        private final String urls;
        private final String providerName;
        private final List<String> providersSearched;

        public Discovery(String urls, String providerName, List<String> providersSearched) {
            this.urls = urls;
            this.providerName = providerName;
            this.providersSearched = providersSearched == null ? List.of() : providersSearched;
        }

        public String getUrls() {
            return urls;
        }

        public String getProviderName() {
            return providerName;
        }

        public List<String> getProvidersSearched() {
            return providersSearched;
        }
    }
}
