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

    /**
     * Get all books with their current LOC status, sorted by call number
     * (blank/missing call numbers at the top)
     */
    public List<BookLocStatusDto> getAllBooksWithLocStatus() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .map(this::mapToBookLocStatusDto)
                .sorted(createCallNumberComparator())
                .collect(Collectors.toList());
    }

    /**
     * Get books that don't have LOC numbers, sorted by title
     */
    public List<BookLocStatusDto> getBooksWithMissingLoc() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .filter(book -> book.getLocNumber() == null || book.getLocNumber().trim().isEmpty())
                .map(this::mapToBookLocStatusDto)
                .sorted(Comparator.comparing(BookLocStatusDto::getTitle, String.CASE_INSENSITIVE_ORDER))
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
     * Tries title + author first, then falls back to title-only if that fails.
     */
    private LocLookupResultDto performLocLookup(Book book) {
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle(book.getTitle());

        boolean hasAuthor = book.getAuthor() != null;
        if (hasAuthor) {
            request.setAuthor(book.getAuthor().getName());
        }

        try {
            // Try with title + author (if author exists)
            LocCallNumberResponse response = locCatalogService.getLocCallNumber(request);

            // Update the book with the found LOC number
            book.setLocNumber(response.getCallNumber());
            bookRepository.save(book);

            log.info("Successfully updated LOC number for book {}: {}", book.getId(), response.getCallNumber());

            return LocLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(true)
                    .locNumber(response.getCallNumber())
                    .matchCount(response.getMatchCount())
                    .build();

        } catch (ResponseStatusException e) {
            // If lookup with author failed and we have an author, try title-only as fallback
            if (hasAuthor) {
                log.info("LOC lookup with title + author failed for book {}, trying title-only fallback", book.getId());

                try {
                    LocSearchRequest titleOnlyRequest = new LocSearchRequest();
                    titleOnlyRequest.setTitle(book.getTitle());

                    LocCallNumberResponse response = locCatalogService.getLocCallNumber(titleOnlyRequest);

                    // Update the book with the found LOC number
                    book.setLocNumber(response.getCallNumber());
                    bookRepository.save(book);

                    log.info("Successfully updated LOC number for book {} using title-only fallback: {}", book.getId(), response.getCallNumber());

                    return LocLookupResultDto.builder()
                            .bookId(book.getId())
                            .success(true)
                            .locNumber(response.getCallNumber())
                            .matchCount(response.getMatchCount())
                            .build();

                } catch (Exception fallbackException) {
                    log.warn("LOC title-only fallback also failed for book {}: {}", book.getId(), fallbackException.getMessage());
                    return LocLookupResultDto.builder()
                            .bookId(book.getId())
                            .success(false)
                            .errorMessage("Failed with title+author and title-only: " + fallbackException.getMessage())
                            .build();
                }
            }

            // Original failure without author or fallback didn't help
            log.warn("LOC lookup failed for book {}: {}", book.getId(), e.getReason());
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
