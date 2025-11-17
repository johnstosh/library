/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookLocStatusDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing book labels
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LabelsService {

    private final BookRepository bookRepository;
    private final LabelsPdfService labelsPdfService;

    /**
     * Get all books with LOC numbers, sorted by date added (newest first)
     */
    public List<BookLocStatusDto> getBooksForLabels() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .filter(book -> book.getLocNumber() != null && !book.getLocNumber().trim().isEmpty())
                .map(this::mapToBookLocStatusDto)
                .sorted(Comparator.comparing(BookLocStatusDto::getDateAdded,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Generate labels PDF for the specified books
     */
    public byte[] generateLabelsPdf(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new LibraryException("No books selected for labels");
        }

        List<Book> books = bookRepository.findAllById(bookIds);

        if (books.isEmpty()) {
            throw new LibraryException("No books found for the given IDs");
        }

        // Filter books that have LOC numbers
        List<Book> booksWithLoc = books.stream()
                .filter(book -> book.getLocNumber() != null && !book.getLocNumber().trim().isEmpty())
                .collect(Collectors.toList());

        if (booksWithLoc.isEmpty()) {
            throw new LibraryException("None of the selected books have LOC numbers");
        }

        log.info("Generating labels PDF for {} books", booksWithLoc.size());

        return labelsPdfService.generateLabelsPdf(booksWithLoc);
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
                .dateAdded(book.getDateAddedToLibrary() != null
                    ? book.getDateAddedToLibrary().toString()
                    : null)
                .firstPhotoId(book.getPhotos() != null && !book.getPhotos().isEmpty()
                    ? book.getPhotos().get(0).getId()
                    : null)
                .build();
    }
}
