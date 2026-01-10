/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.GrokipediaLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for looking up Grokipedia URLs for books and authors.
 * Generates URLs by converting names/titles to the Grokipedia format
 * (spaces become underscores) and verifies they exist via HTTP request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrokipediaLookupService {

    private static final String GROKIPEDIA_BASE_URL = "https://grokipedia.com/page/";

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final RestTemplate restTemplate;

    /**
     * Lookup and update Grokipedia URL for a single book
     */
    public GrokipediaLookupResultDto lookupBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new LibraryException("Book not found: " + bookId));

        return performBookLookup(book);
    }

    /**
     * Lookup and update Grokipedia URLs for multiple books
     */
    public List<GrokipediaLookupResultDto> lookupBooks(List<Long> bookIds) {
        List<GrokipediaLookupResultDto> results = new ArrayList<>();

        for (Long bookId : bookIds) {
            try {
                GrokipediaLookupResultDto result = lookupBook(bookId);
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
     * Lookup and update Grokipedia URL for a single author
     */
    public GrokipediaLookupResultDto lookupAuthor(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new LibraryException("Author not found: " + authorId));

        return performAuthorLookup(author);
    }

    /**
     * Lookup and update Grokipedia URLs for multiple authors
     */
    public List<GrokipediaLookupResultDto> lookupAuthors(List<Long> authorIds) {
        List<GrokipediaLookupResultDto> results = new ArrayList<>();

        for (Long authorId : authorIds) {
            try {
                GrokipediaLookupResultDto result = lookupAuthor(authorId);
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
     * Perform Grokipedia URL lookup for a book
     */
    private GrokipediaLookupResultDto performBookLookup(Book book) {
        String title = book.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return GrokipediaLookupResultDto.builder()
                    .bookId(book.getId())
                    .name(title)
                    .success(false)
                    .errorMessage("Book has no title")
                    .build();
        }

        String grokipediaUrl = generateGrokipediaUrl(title);
        boolean exists = checkUrlExists(grokipediaUrl);

        if (exists) {
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
        } else {
            log.info("No Grokipedia page found for book '{}' at URL: {}", title, grokipediaUrl);
            return GrokipediaLookupResultDto.builder()
                    .bookId(book.getId())
                    .name(title)
                    .success(false)
                    .errorMessage("No Grokipedia page found at " + grokipediaUrl)
                    .build();
        }
    }

    /**
     * Perform Grokipedia URL lookup for an author
     */
    private GrokipediaLookupResultDto performAuthorLookup(Author author) {
        String name = author.getName();
        if (name == null || name.trim().isEmpty()) {
            return GrokipediaLookupResultDto.builder()
                    .authorId(author.getId())
                    .name(name)
                    .success(false)
                    .errorMessage("Author has no name")
                    .build();
        }

        String grokipediaUrl = generateGrokipediaUrl(name);
        boolean exists = checkUrlExists(grokipediaUrl);

        if (exists) {
            author.setGrokipediaUrl(grokipediaUrl);
            authorRepository.save(author);

            log.info("Successfully found Grokipedia URL for author {}: {}", author.getId(), grokipediaUrl);

            return GrokipediaLookupResultDto.builder()
                    .authorId(author.getId())
                    .name(name)
                    .success(true)
                    .grokipediaUrl(grokipediaUrl)
                    .build();
        } else {
            log.info("No Grokipedia page found for author '{}' at URL: {}", name, grokipediaUrl);
            return GrokipediaLookupResultDto.builder()
                    .authorId(author.getId())
                    .name(name)
                    .success(false)
                    .errorMessage("No Grokipedia page found at " + grokipediaUrl)
                    .build();
        }
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
