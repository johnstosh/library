/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.RejectedMatches;
import com.muczynski.library.domain.RejectedUrl;
import com.muczynski.library.dto.GrokipediaLookupResultDto;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Service for looking up Grokipedia URLs for books and authors.
 * Quick lookup checks the generated name/title URL only.
 * Slow lookup does the same, then asks Grok for candidate URLs and keeps
 * those that return 2xx. "-" is saved only when no working URL is found.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrokipediaLookupService {

    static final String GROKIPEDIA_BASE_URL = "https://grokipedia.com/page/";
    static final String NOT_AVAILABLE = "-";

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final RestTemplate restTemplate;
    private final AskGrok askGrok;

    /**
     * Lookup and update Grokipedia URL for a single book (quick).
     */
    public GrokipediaLookupResultDto lookupBook(Long bookId) {
        return lookupBook(bookId, false);
    }

    /**
     * Lookup and update Grokipedia URL for a single book.
     *
     * @param slow when true, ask Grok if the usual URL is not a 2xx
     */
    public GrokipediaLookupResultDto lookupBook(Long bookId, boolean slow) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new LibraryException("Book not found: " + bookId));

        return performBookLookup(book, slow);
    }

    /**
     * Lookup and update Grokipedia URLs for multiple books (quick).
     */
    public List<GrokipediaLookupResultDto> lookupBooks(List<Long> bookIds) {
        return lookupBooks(bookIds, false);
    }

    /**
     * Lookup and update Grokipedia URLs for multiple books.
     */
    public List<GrokipediaLookupResultDto> lookupBooks(List<Long> bookIds, boolean slow) {
        List<GrokipediaLookupResultDto> results = new ArrayList<>();

        for (Long bookId : bookIds) {
            try {
                GrokipediaLookupResultDto result = lookupBook(bookId, slow);
                results.add(result);
            } catch (Exception e) {
                log.error("Error looking up Grokipedia URL for book {}: {}", bookId, e.getMessage());
                results.add(GrokipediaLookupResultDto.builder()
                        .bookId(bookId)
                        .success(false)
                        .errorMessage("Error: " + e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * Quick lookup only. Returns the first usual page URL that answers 2xx, or null.
     * Does not save a URL and does not save "-".
     * A call through the Spring proxy runs outside a transaction.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String findQuickUrl(List<String> titles) {
        return findQuickUrl(titles, List.of());
    }

    /**
     * Quick lookup only. Skips URLs in {@code rejected}. Does not save.
     */
    public String findQuickUrl(List<String> titles, List<RejectedUrl> rejected) {
        if (titles == null) {
            return null;
        }
        for (String title : titles) {
            if (title == null || title.isBlank()) {
                continue;
            }
            String usualUrl = generateGrokipediaUrl(title);
            if (RejectedMatches.rejectsUrl(rejected, usualUrl)) {
                continue;
            }
            if (checkUrlExists(usualUrl)) {
                return usualUrl;
            }
        }
        return null;
    }

    /**
     * Quick lookup for every name. Returns each usual page URL that answers 2xx.
     * Does not stop at the first hit, does not save a URL, and does not save "-".
     * A call through the Spring proxy runs outside a transaction.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<String> findQuickUrls(List<String> titles) {
        return findQuickUrls(titles, List.of());
    }

    /**
     * Quick lookup for every name. Skips URLs in {@code rejected}. Does not save.
     */
    public List<String> findQuickUrls(List<String> titles, List<RejectedUrl> rejected) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        if (titles == null) {
            return List.of();
        }
        for (String title : titles) {
            if (title == null || title.isBlank()) {
                continue;
            }
            String usualUrl = generateGrokipediaUrl(title);
            if (RejectedMatches.rejectsUrl(rejected, usualUrl)) {
                continue;
            }
            if (checkUrlExists(usualUrl)) {
                found.add(usualUrl);
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * Lookup and update Grokipedia URL for a single author (quick).
     */
    public GrokipediaLookupResultDto lookupAuthor(Long authorId) {
        return lookupAuthor(authorId, false);
    }

    /**
     * Lookup and update Grokipedia URL for a single author.
     *
     * @param slow when true, ask Grok if the usual URL is not a 2xx
     */
    public GrokipediaLookupResultDto lookupAuthor(Long authorId, boolean slow) {
        Author author = (slow
                ? authorRepository.findByIdWithBooks(authorId)
                : authorRepository.findById(authorId))
                .orElseThrow(() -> new LibraryException("Author not found: " + authorId));

        return performAuthorLookup(author, slow);
    }

    /**
     * Lookup and update Grokipedia URLs for multiple authors (quick).
     */
    public List<GrokipediaLookupResultDto> lookupAuthors(List<Long> authorIds) {
        return lookupAuthors(authorIds, false);
    }

    /**
     * Lookup and update Grokipedia URLs for multiple authors.
     */
    public List<GrokipediaLookupResultDto> lookupAuthors(List<Long> authorIds, boolean slow) {
        List<GrokipediaLookupResultDto> results = new ArrayList<>();

        for (Long authorId : authorIds) {
            try {
                GrokipediaLookupResultDto result = lookupAuthor(authorId, slow);
                results.add(result);
            } catch (Exception e) {
                log.error("Error looking up Grokipedia URL for author {}: {}", authorId, e.getMessage());
                results.add(GrokipediaLookupResultDto.builder()
                        .authorId(authorId)
                        .success(false)
                        .errorMessage("Error: " + e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * Perform Grokipedia URL lookup for a book.
     */
    private GrokipediaLookupResultDto performBookLookup(Book book, boolean slow) {
        List<String> titles = Book.titlesToSearch(book);
        if (titles.isEmpty()) {
            return GrokipediaLookupResultDto.builder()
                    .bookId(book.getId())
                    .name(book.getTitle())
                    .success(false)
                    .errorMessage("Book has no title")
                    .build();
        }

        String primaryTitle = book.getTitle();
        String foundUrl = null;
        List<RejectedUrl> rejected = book.getInvalidGrokipediaUrls();
        boolean[] sawRejected = new boolean[1];

        for (String lookupTitle : titles) {
            String usualUrl = generateGrokipediaUrl(lookupTitle);
            if (RejectedMatches.rejectsUrl(rejected, usualUrl)) {
                sawRejected[0] = true;
                continue;
            }
            if (checkUrlExists(usualUrl)) {
                foundUrl = usualUrl;
                break;
            }
        }

        if (foundUrl != null) {
            return saveBookUrl(book, primaryTitle, foundUrl);
        }

        if (!slow) {
            if (sawRejected[0]) {
                return rejectedGrokipedia(book.getId(), null, primaryTitle);
            }
            log.info("No Grokipedia page found for book '{}' at usual URLs", primaryTitle);
            return GrokipediaLookupResultDto.builder()
                    .bookId(book.getId())
                    .name(primaryTitle)
                    .success(false)
                    .errorMessage("No Grokipedia page found")
                    .build();
        }

        // slow path: try Grok suggest on primary (per spec: primary first)
        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;
        List<String> grokUrls;
        try {
            grokUrls = askGrok.suggestGrokipediaUrlsForBook(Book.stripCopySuffix(primaryTitle), authorName);
        } catch (Exception e) {
            log.error("Grok Grokipedia lookup failed for book {}: {}", book.getId(), e.getMessage());
            return GrokipediaLookupResultDto.builder()
                    .bookId(book.getId())
                    .name(primaryTitle)
                    .success(false)
                    .errorMessage(grokFailureMessage(e))
                    .build();
        }

        String workingUrl = firstWorkingUrl(grokUrls, rejected, sawRejected);
        if (workingUrl != null) {
            return saveBookUrl(book, primaryTitle, workingUrl);
        }

        for (int index = 1; index < titles.size(); index++) {
            String altTitle = titles.get(index);
            try {
                grokUrls = askGrok.suggestGrokipediaUrlsForBook(altTitle, authorName);
                workingUrl = firstWorkingUrl(grokUrls, rejected, sawRejected);
                if (workingUrl != null) {
                    return saveBookUrl(book, primaryTitle, workingUrl);
                }
            } catch (Exception e) {
                log.warn("Grok lookup with alternate title failed for book {}: {}", book.getId(), e.getMessage());
            }
        }

        if (sawRejected[0]) {
            return rejectedGrokipedia(book.getId(), null, primaryTitle);
        }
        return saveBookNotAvailable(book, primaryTitle);
    }

    /** Plain out-of-credits message as-is; other Grok failures keep the "Grok lookup failed:" prefix. */
    private static String grokFailureMessage(Exception e) {
        if (e instanceof GrokCreditsExhaustedException) {
            return e.getMessage();
        }
        return "Grok lookup failed: " + e.getMessage();
    }

    /**
     * Perform Grokipedia URL lookup for an author.
     */
    private GrokipediaLookupResultDto performAuthorLookup(Author author, boolean slow) {
        String name = author.getName();
        if (name == null || name.trim().isEmpty()) {
            return GrokipediaLookupResultDto.builder()
                    .authorId(author.getId())
                    .name(name)
                    .success(false)
                    .errorMessage("Author has no name")
                    .build();
        }

        List<RejectedUrl> rejected = author.getInvalidGrokipediaUrls();
        boolean[] sawRejected = new boolean[1];
        String usualUrl = generateGrokipediaUrl(name);
        if (RejectedMatches.rejectsUrl(rejected, usualUrl)) {
            sawRejected[0] = true;
        } else if (checkUrlExists(usualUrl)) {
            return saveAuthorUrl(author, name, usualUrl);
        }

        if (!slow) {
            if (sawRejected[0]) {
                return rejectedGrokipedia(null, author.getId(), name);
            }
            log.info("No Grokipedia page found for author '{}' at URL: {}", name, usualUrl);
            return GrokipediaLookupResultDto.builder()
                    .authorId(author.getId())
                    .name(name)
                    .success(false)
                    .errorMessage("No Grokipedia page found at " + usualUrl)
                    .build();
        }

        String bookTitle = firstBookTitle(author);
        List<String> grokUrls;
        try {
            grokUrls = askGrok.suggestGrokipediaUrlsForAuthor(name, bookTitle);
        } catch (Exception e) {
            log.error("Grok Grokipedia lookup failed for author {}: {}", author.getId(), e.getMessage());
            return GrokipediaLookupResultDto.builder()
                    .authorId(author.getId())
                    .name(name)
                    .success(false)
                    .errorMessage(grokFailureMessage(e))
                    .build();
        }

        String workingUrl = firstWorkingUrl(grokUrls, rejected, sawRejected);
        if (workingUrl != null) {
            return saveAuthorUrl(author, name, workingUrl);
        }

        if (sawRejected[0]) {
            return rejectedGrokipedia(null, author.getId(), name);
        }
        return saveAuthorNotAvailable(author, name);
    }

    private GrokipediaLookupResultDto saveBookUrl(Book book, String title, String grokipediaUrl) {
        book.setGrokipediaUrl(grokipediaUrl);
        book.setLastModified(LocalDateTime.now());
        bookRepository.save(book);

        log.info("Successfully found Grokipedia URL for book {}: {}", book.getId(), grokipediaUrl);

        return GrokipediaLookupResultDto.builder()
                .bookId(book.getId())
                .name(title)
                .success(true)
                .grokipediaUrl(grokipediaUrl)
                .build();
    }

    private GrokipediaLookupResultDto saveBookNotAvailable(Book book, String title) {
        book.setGrokipediaUrl(NOT_AVAILABLE);
        book.setLastModified(LocalDateTime.now());
        bookRepository.save(book);

        log.info("No working Grokipedia URL for book {}; saved as N/A", book.getId());

        return GrokipediaLookupResultDto.builder()
                .bookId(book.getId())
                .name(title)
                .success(false)
                .grokipediaUrl(NOT_AVAILABLE)
                .errorMessage("No Grokipedia page found; saved as N/A")
                .build();
    }

    private GrokipediaLookupResultDto saveAuthorUrl(Author author, String name, String grokipediaUrl) {
        author.setGrokipediaUrl(grokipediaUrl);
        authorRepository.save(author);

        log.info("Successfully found Grokipedia URL for author {}: {}", author.getId(), grokipediaUrl);

        return GrokipediaLookupResultDto.builder()
                .authorId(author.getId())
                .name(name)
                .success(true)
                .grokipediaUrl(grokipediaUrl)
                .build();
    }

    private GrokipediaLookupResultDto saveAuthorNotAvailable(Author author, String name) {
        author.setGrokipediaUrl(NOT_AVAILABLE);
        authorRepository.save(author);

        log.info("No working Grokipedia URL for author {}; saved as N/A", author.getId());

        return GrokipediaLookupResultDto.builder()
                .authorId(author.getId())
                .name(name)
                .success(false)
                .grokipediaUrl(NOT_AVAILABLE)
                .errorMessage("No Grokipedia page found; saved as N/A")
                .build();
    }

    /**
     * Return the first candidate URL that returns 2xx. 4xx URLs are discarded.
     * "-" is not saved here; the caller saves N/A only when this returns null.
     */
    private GrokipediaLookupResultDto rejectedGrokipedia(Long bookId, Long authorId, String name) {
        log.info("Every Grokipedia candidate for '{}' was already rejected; leaving the stored URL", name);
        return GrokipediaLookupResultDto.builder()
                .bookId(bookId)
                .authorId(authorId)
                .name(name)
                .success(false)
                .errorMessage("Rejected Grokipedia page")
                .build();
    }

    /**
     * Return the first candidate URL that returns 2xx and is not rejected.
     * A rejected candidate sets {@code sawRejected} and is not written.
     */
    private String firstWorkingUrl(List<String> candidateUrls, List<RejectedUrl> rejected, boolean[] sawRejected) {
        if (candidateUrls == null) {
            return null;
        }
        for (String candidate : candidateUrls) {
            if (candidate == null || candidate.isBlank() || NOT_AVAILABLE.equals(candidate.trim())) {
                continue;
            }
            String url = candidate.trim();
            if (!isGrokipediaPageUrl(url)) {
                continue;
            }
            if (RejectedMatches.rejectsUrl(rejected, url)) {
                sawRejected[0] = true;
                continue;
            }
            if (checkUrlExists(url)) {
                return url;
            }
        }
        return null;
    }

    private String firstBookTitle(Author author) {
        if (author.getBooks() == null) {
            return null;
        }
        for (Book book : author.getBooks()) {
            if (book != null && book.getTitle() != null && !book.getTitle().isBlank()) {
                return Book.stripCopySuffix(book.getTitle());
            }
        }
        return null;
    }

    static boolean isGrokipediaPageUrl(String url) {
        return url.contains("grokipedia.com/page/");
    }

    /**
     * Generate a Grokipedia URL from a name or title.
     * Converts spaces to underscores.
     */
    String generateGrokipediaUrl(String nameOrTitle) {
        String normalized = nameOrTitle.trim().replace(" ", "_");
        return GROKIPEDIA_BASE_URL + normalized;
    }

    /**
     * Check if a URL exists by making a HEAD request.
     * Returns true if status is 2xx, false for 4xx/5xx.
     */
    boolean checkUrlExists(String url) {
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    null,
                    Void.class
            );
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            return status.is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            // 4xx errors indicate the page doesn't exist
            log.debug("URL check returned {}: {}", e.getStatusCode(), url);
            return false;
        } catch (Exception e) {
            log.warn("Error checking URL {}: {}", url, e.getMessage());
            return false;
        }
    }
}
