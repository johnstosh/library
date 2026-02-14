package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookLocStatusDto;
import com.muczynski.library.dto.LocLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.model.LocCallNumberResponse;
import com.muczynski.library.model.LocSearchRequest;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import edu.byu.hbll.callnumber.CallNumber;
import edu.byu.hbll.callnumber.CallNumberParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for bulk LOC call number lookup operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LocBulkLookupService {

    private final BookRepository bookRepository;
    private final PhotoRepository photoRepository;
    private final LocCatalogService locCatalogService;
    private final AskGrok askGrok;

    /**
     * Get all books with their current LOC status, sorted by date added (most recent first)
     */
    public List<BookLocStatusDto> getAllBooksWithLocStatus() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .map(this::mapToBookLocStatusDto)
                .sorted(createDateAddedComparator())
                .collect(Collectors.toList());
    }

    /**
     * Get books that don't have LOC numbers, sorted by date added (most recent first)
     */
    public List<BookLocStatusDto> getBooksWithMissingLoc() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .filter(book -> book.getLocNumber() == null || book.getLocNumber().trim().isEmpty())
                .map(this::mapToBookLocStatusDto)
                .sorted(createDateAddedComparator())
                .collect(Collectors.toList());
    }

    /**
     * Get books added on the most recent date, sorted by datetime (most recent first)
     */
    public List<BookLocStatusDto> getBooksFromMostRecentDate() {
        LocalDateTime mostRecentDateTime = bookRepository.findMaxDateAddedToLibrary();
        if (mostRecentDateTime == null) {
            return Collections.emptyList();
        }

        // Get the start and end of the day for the most recent datetime
        LocalDate mostRecentDate = mostRecentDateTime.toLocalDate();
        LocalDateTime startOfDay = mostRecentDate.atStartOfDay();
        LocalDateTime endOfDay = mostRecentDate.plusDays(1).atStartOfDay();

        List<Book> books = bookRepository.findByDateAddedToLibraryBetweenOrderByDateAddedDesc(startOfDay, endOfDay);
        return books.stream()
                .map(this::mapToBookLocStatusDto)
                .collect(Collectors.toList());
    }

    /**
     * Lookup and update LOC number for a single book
     */
    public LocLookupResultDto lookupAndUpdateBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new LibraryException("Book not found: " + bookId));

        return performLocLookup(book);
    }

    /**
     * Lookup and update LOC numbers for multiple books
     */
    public List<LocLookupResultDto> lookupAndUpdateBooks(List<Long> bookIds) {
        List<LocLookupResultDto> results = new ArrayList<>();

        for (Long bookId : bookIds) {
            try {
                LocLookupResultDto result = lookupAndUpdateBook(bookId);
                results.add(result);
            } catch (Exception e) {
                log.error("Error looking up LOC for book {}: {}", bookId, e.getMessage());
                results.add(LocLookupResultDto.builder()
                        .bookId(bookId)
                        .success(false)
                        .errorMessage("Error: " + e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * Lookup and update all books that are missing LOC numbers
     */
    public List<LocLookupResultDto> lookupAllMissingLoc() {
        List<Book> booksWithMissingLoc = bookRepository.findAll().stream()
                .filter(book -> book.getLocNumber() == null || book.getLocNumber().trim().isEmpty())
                .collect(Collectors.toList());

        List<LocLookupResultDto> results = new ArrayList<>();

        for (Book book : booksWithMissingLoc) {
            try {
                LocLookupResultDto result = performLocLookup(book);
                results.add(result);
            } catch (Exception e) {
                log.error("Error looking up LOC for book {}: {}", book.getId(), e.getMessage());
                results.add(LocLookupResultDto.builder()
                        .bookId(book.getId())
                        .success(false)
                        .errorMessage("Error: " + e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * Perform LOC lookup for a book and update if found.
     * Tries multiple strategies in order:
     * 1. Title + author (if author exists)
     * 2. Title-only (fallback if #1 fails)
     * 3. Truncated title at colon + author (if title contains ":")
     * 4. Truncated title at colon only (final fallback)
     * Skips temporary titles (those starting with date pattern).
     */
    private LocLookupResultDto performLocLookup(Book book) {
        // Skip temporary titles (those starting with date pattern like 2025-01-15_...)
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            log.info("Skipping LOC lookup for temporary title: {}", book.getTitle());
            return LocLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Not Ready - Temporary title")
                    .build();
        }

        String originalTitle = book.getTitle();
        boolean hasAuthor = book.getAuthor() != null;
        String authorName = hasAuthor ? book.getAuthor().getName() : null;

        // Strategy 1: Try with original title + author (if author exists)
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle(originalTitle);
        if (hasAuthor) {
            request.setAuthor(authorName);
        }

        try {
            LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);

            // Update the book with the found LOC number
            book.setLocNumber(response.getCallNumber());
            book.setLastModified(LocalDateTime.now());
            bookRepository.save(book);

            log.info("Successfully updated LOC number for book {}: {}", book.getId(), response.getCallNumber());

            return LocLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(true)
                    .locNumber(response.getCallNumber())
                    .matchCount(response.getMatchCount())
                    .build();

        } catch (ResponseStatusException e) {
            // Strategy 2: If lookup with author failed and we have an author, try title-only
            if (hasAuthor) {
                log.info("LOC lookup with title + author failed for book {}, trying title-only fallback", book.getId());

                try {
                    LocSearchRequest titleOnlyRequest = new LocSearchRequest();
                    titleOnlyRequest.setTitle(originalTitle);

                    LocCallNumberResponse response = locCatalogService.getLocCallNumber(titleOnlyRequest);

                    // Update the book with the found LOC number
                    book.setLocNumber(response.getCallNumber());
                    book.setLastModified(LocalDateTime.now());
                    bookRepository.save(book);

                    log.info("Successfully updated LOC number for book {} using title-only fallback: {}", book.getId(), response.getCallNumber());

                    return LocLookupResultDto.builder()
                            .bookId(book.getId())
                            .success(true)
                            .locNumber(response.getCallNumber())
                            .matchCount(response.getMatchCount())
                            .build();

                } catch (Exception titleOnlyException) {
                    log.warn("LOC title-only fallback also failed for book {}: {}", book.getId(), titleOnlyException.getMessage());
                    // Continue to colon truncation strategy below
                }
            }

            // Strategy 3 & 4: If title contains colon, try with truncated title
            if (originalTitle.contains(":")) {
                String truncatedTitle = originalTitle.substring(0, originalTitle.indexOf(":")).trim();
                log.info("Title contains colon, trying with truncated title: '{}' -> '{}'", originalTitle, truncatedTitle);

                // Strategy 3: Try truncated title + author (if author exists)
                if (hasAuthor) {
                    try {
                        LocSearchRequest truncatedWithAuthorRequest = new LocSearchRequest();
                        truncatedWithAuthorRequest.setTitle(truncatedTitle);
                        truncatedWithAuthorRequest.setAuthor(authorName);

                        LocCallNumberResponse response = locCatalogService.getLocCallNumber(truncatedWithAuthorRequest);

                        // Update the book with the found LOC number
                        book.setLocNumber(response.getCallNumber());
                        book.setLastModified(LocalDateTime.now());
                        bookRepository.save(book);

                        log.info("Successfully updated LOC number for book {} using truncated title + author: {}", book.getId(), response.getCallNumber());

                        return LocLookupResultDto.builder()
                                .bookId(book.getId())
                                .success(true)
                                .locNumber(response.getCallNumber())
                                .matchCount(response.getMatchCount())
                                .build();

                    } catch (Exception truncatedAuthorException) {
                        log.warn("LOC truncated title + author fallback failed for book {}: {}", book.getId(), truncatedAuthorException.getMessage());
                        // Continue to final fallback
                    }
                }

                // Strategy 4: Final fallback - try truncated title only
                try {
                    LocSearchRequest truncatedOnlyRequest = new LocSearchRequest();
                    truncatedOnlyRequest.setTitle(truncatedTitle);

                    LocCallNumberResponse response = locCatalogService.getLocCallNumber(truncatedOnlyRequest);

                    // Update the book with the found LOC number
                    book.setLocNumber(response.getCallNumber());
                    book.setLastModified(LocalDateTime.now());
                    bookRepository.save(book);

                    log.info("Successfully updated LOC number for book {} using truncated title-only: {}", book.getId(), response.getCallNumber());

                    return LocLookupResultDto.builder()
                            .bookId(book.getId())
                            .success(true)
                            .locNumber(response.getCallNumber())
                            .matchCount(response.getMatchCount())
                            .build();

                } catch (Exception truncatedOnlyException) {
                    log.warn("LOC truncated title-only fallback also failed for book {}: {}", book.getId(), truncatedOnlyException.getMessage());

                    // Strategy 5: Try AI suggestion as last resort
                    LocLookupResultDto aiResult = attemptAiSuggest(book);
                    if (aiResult != null) {
                        return aiResult;
                    }

                    return LocLookupResultDto.builder()
                            .bookId(book.getId())
                            .success(false)
                            .errorMessage("All lookup strategies failed (including truncated title and AI suggest)")
                            .build();
                }
            }

            // No colon in title, all strategies exhausted - try AI suggestion
            log.warn("LOC lookup failed for book {}: {}", book.getId(), e.getReason());

            // Strategy 5: Try AI suggestion as last resort
            LocLookupResultDto aiResult = attemptAiSuggest(book);
            if (aiResult != null) {
                return aiResult;
            }

            return LocLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage(e.getReason())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during LOC lookup for book {}: {}", book.getId(), e.getMessage());
            return LocLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Strategy 5: Attempt AI-based LOC suggestion using AskGrok as a last resort.
     * Returns a successful result if AI provides a suggestion, or null if AI also fails.
     */
    private LocLookupResultDto attemptAiSuggest(Book book) {
        try {
            String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;
            String suggestion = askGrok.suggestLocNumber(book.getTitle(), authorName);

            if (suggestion != null && !suggestion.trim().isEmpty()) {
                book.setLocNumber(suggestion.trim());
                book.setLastModified(LocalDateTime.now());
                bookRepository.save(book);

                log.info("AI suggested LOC number for book {}: {}", book.getId(), suggestion.trim());

                return LocLookupResultDto.builder()
                        .bookId(book.getId())
                        .success(true)
                        .locNumber(suggestion.trim())
                        .aiSuggested(true)
                        .build();
            }
        } catch (Exception aiException) {
            log.warn("AI LOC suggestion also failed for book {}: {}", book.getId(), aiException.getMessage());
        }
        return null;
    }

    /**
     * Map Book entity to BookLocStatusDto
     */
    private BookLocStatusDto mapToBookLocStatusDto(Book book) {
        String dateAddedStr = null;
        if (book.getDateAddedToLibrary() != null) {
            dateAddedStr = book.getDateAddedToLibrary().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        return BookLocStatusDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthor() != null ? book.getAuthor().getName() : null)
                .currentLocNumber(book.getLocNumber())
                .hasLocNumber(book.getLocNumber() != null && !book.getLocNumber().trim().isEmpty())
                .publicationYear(book.getPublicationYear())
                // Use efficient query to get first photo ID without loading photos collection
                .firstPhotoId(photoRepository.findFirstPhotoIdByBookId(book.getId()))
                .dateAdded(dateAddedStr)
                .build();
    }

    /**
     * Create a comparator for sorting by date added to library
     * Most recent dates first, nulls last, ties broken by title
     */
    private Comparator<BookLocStatusDto> createDateAddedComparator() {
        return (book1, book2) -> {
            String date1 = book1.getDateAdded();
            String date2 = book2.getDateAdded();

            // Handle null dates - they should appear at the end
            if (date1 == null && date2 == null) {
                return book1.getTitle().compareToIgnoreCase(book2.getTitle());
            }
            if (date1 == null) {
                return 1; // book1 comes after
            }
            if (date2 == null) {
                return -1; // book2 comes after
            }

            // Both have dates, compare in descending order (most recent first)
            int dateComparison = date2.compareTo(date1);
            if (dateComparison != 0) {
                return dateComparison;
            }

            // Dates are equal, sort by title
            return book1.getTitle().compareToIgnoreCase(book2.getTitle());
        };
    }

    /**
     * Create a comparator for sorting by call number
     * Books with missing/blank call numbers are sorted to the top
     */
    private Comparator<BookLocStatusDto> createCallNumberComparator() {
        return (book1, book2) -> {
            String loc1 = book1.getCurrentLocNumber();
            String loc2 = book2.getCurrentLocNumber();

            // Handle null or blank call numbers - they should appear at the top
            boolean isBlank1 = loc1 == null || loc1.trim().isEmpty();
            boolean isBlank2 = loc2 == null || loc2.trim().isEmpty();

            if (isBlank1 && isBlank2) {
                // Both blank, sort by title
                return book1.getTitle().compareToIgnoreCase(book2.getTitle());
            }
            if (isBlank1) {
                return -1; // book1 comes first
            }
            if (isBlank2) {
                return 1; // book2 comes first
            }

            // Both have call numbers, use BYU library to compare
            try {
                CallNumberParser parser = CallNumberParser.SYMPHONY_NONSTRICT;
                CallNumber cn1 = parser.parse(loc1);
                CallNumber cn2 = parser.parse(loc2);
                return cn1.compareTo(cn2);
            } catch (Exception e) {
                // If parsing fails, fall back to string comparison
                log.warn("Failed to parse call numbers for comparison: '{}' vs '{}': {}",
                        loc1, loc2, e.getMessage());
                return loc1.compareToIgnoreCase(loc2);
            }
        };
    }
}
