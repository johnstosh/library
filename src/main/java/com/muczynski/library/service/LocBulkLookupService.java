package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookLocStatusDto;
import com.muczynski.library.dto.LocLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.model.LocCallNumberResponse;
import com.muczynski.library.model.LocSearchRequest;
import com.muczynski.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
    private final LocCatalogService locCatalogService;

    /**
     * Get all books with their current LOC status
     */
    public List<BookLocStatusDto> getAllBooksWithLocStatus() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .map(this::mapToBookLocStatusDto)
                .collect(Collectors.toList());
    }

    /**
     * Get books that don't have LOC numbers
     */
    public List<BookLocStatusDto> getBooksWithMissingLoc() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .filter(book -> book.getLocNumber() == null || book.getLocNumber().trim().isEmpty())
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
     * Perform LOC lookup for a book and update if found
     */
    private LocLookupResultDto performLocLookup(Book book) {
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle(book.getTitle());

        if (book.getAuthor() != null) {
            request.setAuthor(book.getAuthor().getName());
        }

        try {
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
        return BookLocStatusDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthor() != null ? book.getAuthor().getName() : null)
                .currentLocNumber(book.getLocNumber())
                .hasLocNumber(book.getLocNumber() != null && !book.getLocNumber().trim().isEmpty())
                .publicationYear(book.getPublicationYear())
                .firstPhotoId(book.getPhotos() != null && !book.getPhotos().isEmpty()
                    ? book.getPhotos().get(0).getId()
                    : null)
                .build();
    }
}
