/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;

@Service
public class BookMapper {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PhotoRepository photoRepository;

    public BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }

        BookDto bookDto = new BookDto();
        bookDto.setId(book.getId());
        bookDto.setTitle(book.getTitle());
        bookDto.setPublicationYear(book.getPublicationYear());
        bookDto.setPublisher(book.getPublisher());
        bookDto.setPlotSummary(book.getPlotSummary());
        bookDto.setRelatedWorks(book.getRelatedWorks());
        bookDto.setDetailedDescription(book.getDetailedDescription());
        bookDto.setGrokipediaUrl(book.getGrokipediaUrl());
        bookDto.setFreeTextUrl(book.getFreeTextUrl());
        bookDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
        bookDto.setLastModified(book.getLastModified());
        bookDto.setStatus(book.getStatus());
        bookDto.setLocNumber(book.getLocNumber());
        bookDto.setElectronicResource(book.getElectronicResource());
        bookDto.setStatusReason(book.getStatusReason());
        if (book.getAuthor() != null) {
            bookDto.setAuthorId(book.getAuthor().getId());
            bookDto.setAuthor(book.getAuthor().getName()); // Set author name for display
        }
        if (book.getLibrary() != null) {
            bookDto.setLibraryId(book.getLibrary().getId());
            bookDto.setLibrary(book.getLibrary().getBranchName()); // Set library branch name for display
        }
        // Use efficient queries to get first photo ID and checksum without loading photos collection
        Long firstPhotoId = photoRepository.findFirstPhotoIdByBookId(book.getId());
        if (firstPhotoId != null) {
            bookDto.setFirstPhotoId(firstPhotoId);
            String firstPhotoChecksum = photoRepository.findFirstPhotoChecksumByBookId(book.getId());
            if (firstPhotoChecksum != null) {
                bookDto.setFirstPhotoChecksum(firstPhotoChecksum);
            }
        }

        bookDto.setLoanCount(loanRepository.countByBookIdAndReturnDateIsNull(book.getId()));

        // Map tags list
        if (book.getTagsList() != null) {
            bookDto.setTagsList(new java.util.ArrayList<>(book.getTagsList()));
        }

        return bookDto;
    }

    public Book toEntity(BookDto bookDto) {
        if (bookDto == null) {
            return null;
        }

        Book book = new Book();
        book.setId(bookDto.getId());
        book.setTitle(bookDto.getTitle());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotSummary(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setGrokipediaUrl(bookDto.getGrokipediaUrl());
        book.setFreeTextUrl(bookDto.getFreeTextUrl());
        book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
        book.setLastModified(bookDto.getLastModified());
        book.setStatus(bookDto.getStatus());
        book.setLocNumber(bookDto.getLocNumber());
        book.setElectronicResource(bookDto.getElectronicResource());
        book.setStatusReason(bookDto.getStatusReason());

        if (bookDto.getAuthorId() != null) {
            Author author = new Author();
            author.setId(bookDto.getAuthorId());
            book.setAuthor(author);
        }

        if (bookDto.getLibraryId() != null) {
            Library library = new Library();
            library.setId(bookDto.getLibraryId());
            book.setLibrary(library);
        }

        // Map tags list - normalize to lowercase with only allowed characters
        if (bookDto.getTagsList() != null) {
            book.setTagsList(bookDto.getTagsList().stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.toLowerCase().replaceAll("[^a-z0-9-]", ""))
                    .filter(tag -> !tag.isEmpty())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList()));
        }

        return book;
    }

    public BookSummaryDto toSummaryDto(Book book) {
        if (book == null) {
            return null;
        }

        BookSummaryDto summaryDto = new BookSummaryDto();
        summaryDto.setId(book.getId());
        summaryDto.setLastModified(book.getLastModified());
        return summaryDto;
    }
}
