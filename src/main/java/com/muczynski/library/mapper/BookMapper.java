/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookTitles;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.domain.RejectedMatches;
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
        copyBookFields(book, bookDto);
        copyAuthorDisplayFields(book, bookDto);
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

    // Used by getBooksByIds() — pre-fetched data avoids N+1 repository calls
    public BookDto toDtoWithData(Book book, Long firstPhotoId, String firstPhotoChecksum, long loanCount) {
        if (book == null) {
            return null;
        }
        BookDto bookDto = new BookDto();
        copyBookFields(book, bookDto);
        copyAuthorDisplayFields(book, bookDto);
        if (book.getLibrary() != null) {
            bookDto.setLibraryId(book.getLibrary().getId());
            bookDto.setLibrary(book.getLibrary().getBranchName());
        }
        bookDto.setFirstPhotoId(firstPhotoId);
        bookDto.setFirstPhotoChecksum(firstPhotoChecksum);
        bookDto.setLoanCount(loanCount);
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
        book.setAlternateTitles(BookTitles.normalize(bookDto.getTitle(), bookDto.getAlternateTitles()));
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotEssay(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setGrokipediaUrl(bookDto.getGrokipediaUrl());
        book.setInvalidGrokipediaUrls(RejectedMatches.acceptEditedUrls(
                bookDto.getInvalidGrokipediaUrls(), null, com.muczynski.library.service.CurrentActor.username()));
        book.setFreeTextUrl(bookDto.getFreeTextUrl());
        book.setInvalidFreeTextUrls(RejectedMatches.acceptEditedUrls(
                bookDto.getInvalidFreeTextUrls(), null, com.muczynski.library.service.CurrentActor.username()));
        book.setInvalidFreeAudioUrls(RejectedMatches.acceptEditedUrls(
                bookDto.getInvalidFreeAudioUrls(), null, com.muczynski.library.service.CurrentActor.username()));
        book.setInvalidHoldings(RejectedMatches.acceptEditedHoldings(
                bookDto.getInvalidHoldings(), null, com.muczynski.library.service.CurrentActor.username()));
        book.setFreeTextLastSearched(bookDto.getFreeTextLastSearched());
        book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
        book.setLastModified(bookDto.getLastModified());
        book.setStatus(bookDto.getStatus());
        book.setLocNumber(bookDto.getLocNumber());
        book.setElectronicResource(bookDto.getElectronicResource());
        book.setStatusReason(bookDto.getStatusReason());
        book.setYdlAudioAvailable(bookDto.getYdlAudioAvailable());
        book.setYdlPaperAvailable(bookDto.getYdlPaperAvailable());
        book.setYdlEbookAvailable(bookDto.getYdlEbookAvailable());
        book.setYdlLastChecked(bookDto.getYdlLastChecked());
        book.setYdlLookupError(bookDto.getYdlLookupError());
        book.setEmuAudioAvailable(bookDto.getEmuAudioAvailable());
        book.setEmuPaperAvailable(bookDto.getEmuPaperAvailable());
        book.setEmuEbookAvailable(bookDto.getEmuEbookAvailable());
        book.setEmuLastChecked(bookDto.getEmuLastChecked());
        book.setEmuLookupError(bookDto.getEmuLookupError());
        book.setAclaAudioAvailable(bookDto.getAclaAudioAvailable());
        book.setAclaPaperAvailable(bookDto.getAclaPaperAvailable());
        book.setAclaEbookAvailable(bookDto.getAclaEbookAvailable());
        book.setAclaLastChecked(bookDto.getAclaLastChecked());
        book.setAclaLookupError(bookDto.getAclaLookupError());
        book.setWccAudioAvailable(bookDto.getWccAudioAvailable());
        book.setWccPaperAvailable(bookDto.getWccPaperAvailable());
        book.setWccEbookAvailable(bookDto.getWccEbookAvailable());
        book.setWccLastChecked(bookDto.getWccLastChecked());
        book.setWccLookupError(bookDto.getWccLookupError());
        book.setMsuAudioAvailable(bookDto.getMsuAudioAvailable());
        book.setMsuPaperAvailable(bookDto.getMsuPaperAvailable());
        book.setMsuEbookAvailable(bookDto.getMsuEbookAvailable());
        book.setMsuLastChecked(bookDto.getMsuLastChecked());
        book.setMsuLookupError(bookDto.getMsuLookupError());
        book.setReadingDifficulty(bookDto.getReadingDifficulty() != null ? bookDto.getReadingDifficulty() : ReadingDifficulty.UNSET);
        book.setBinding(BookCoverType.orUnknown(bookDto.getBinding()));
        book.setDesireToPurchase(bookDto.getDesireToPurchase());

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

    private static void copyBookFields(Book book, BookDto bookDto) {
        bookDto.setId(book.getId());
        bookDto.setTitle(book.getTitle());
        if (book.getAlternateTitles() == null || book.getAlternateTitles().isEmpty()) {
            bookDto.setAlternateTitles(null);
        } else {
            bookDto.setAlternateTitles(new java.util.ArrayList<>(book.getAlternateTitles()));
        }
        bookDto.setPublicationYear(book.getPublicationYear());
        bookDto.setPublisher(book.getPublisher());
        bookDto.setPlotSummary(book.getPlotEssay());
        bookDto.setRelatedWorks(book.getRelatedWorks());
        bookDto.setDetailedDescription(book.getDetailedDescription());
        bookDto.setGrokipediaUrl(book.getGrokipediaUrl());
        bookDto.setInvalidGrokipediaUrls(RejectedMatches.orEmptyUrls(book.getInvalidGrokipediaUrls()));
        bookDto.setFreeTextUrl(book.getFreeTextUrl());
        bookDto.setInvalidFreeTextUrls(RejectedMatches.orEmptyUrls(book.getInvalidFreeTextUrls()));
        bookDto.setInvalidFreeAudioUrls(RejectedMatches.orEmptyUrls(book.getInvalidFreeAudioUrls()));
        bookDto.setInvalidHoldings(RejectedMatches.orEmptyHoldings(book.getInvalidHoldings()));
        bookDto.setFreeTextLastSearched(book.getFreeTextLastSearched());
        bookDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
        bookDto.setLastModified(book.getLastModified());
        bookDto.setStatus(book.getStatus());
        bookDto.setLocNumber(book.getLocNumber());
        bookDto.setElectronicResource(book.getElectronicResource());
        bookDto.setStatusReason(book.getStatusReason());
        bookDto.setYdlAudioAvailable(book.getYdlAudioAvailable());
        bookDto.setYdlPaperAvailable(book.getYdlPaperAvailable());
        bookDto.setYdlEbookAvailable(book.getYdlEbookAvailable());
        bookDto.setYdlLastChecked(book.getYdlLastChecked());
        bookDto.setYdlLookupError(book.getYdlLookupError());
        bookDto.setEmuAudioAvailable(book.getEmuAudioAvailable());
        bookDto.setEmuPaperAvailable(book.getEmuPaperAvailable());
        bookDto.setEmuEbookAvailable(book.getEmuEbookAvailable());
        bookDto.setEmuLastChecked(book.getEmuLastChecked());
        bookDto.setEmuLookupError(book.getEmuLookupError());
        bookDto.setAclaAudioAvailable(book.getAclaAudioAvailable());
        bookDto.setAclaPaperAvailable(book.getAclaPaperAvailable());
        bookDto.setAclaEbookAvailable(book.getAclaEbookAvailable());
        bookDto.setAclaLastChecked(book.getAclaLastChecked());
        bookDto.setAclaLookupError(book.getAclaLookupError());
        bookDto.setWccAudioAvailable(book.getWccAudioAvailable());
        bookDto.setWccPaperAvailable(book.getWccPaperAvailable());
        bookDto.setWccEbookAvailable(book.getWccEbookAvailable());
        bookDto.setWccLastChecked(book.getWccLastChecked());
        bookDto.setWccLookupError(book.getWccLookupError());
        bookDto.setMsuAudioAvailable(book.getMsuAudioAvailable());
        bookDto.setMsuPaperAvailable(book.getMsuPaperAvailable());
        bookDto.setMsuEbookAvailable(book.getMsuEbookAvailable());
        bookDto.setMsuLastChecked(book.getMsuLastChecked());
        bookDto.setMsuLookupError(book.getMsuLookupError());
        bookDto.setReadingDifficulty(book.getReadingDifficulty());
        bookDto.setBinding(BookCoverType.orUnknown(book.getBinding()));
        bookDto.setDesireToPurchase(book.getDesireToPurchase());
    }

    private void copyAuthorDisplayFields(Book book, BookDto bookDto) {
        if (book.getAuthor() == null) {
            return;
        }
        bookDto.setAuthorId(book.getAuthor().getId());
        bookDto.setAuthor(book.getAuthor().getName());
        bookDto.setAuthorGrokipediaUrl(book.getAuthor().getGrokipediaUrl());
    }
}
