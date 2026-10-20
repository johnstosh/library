/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.domain.Favorite;
import com.muczynski.library.domain.Loan;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Before-state stored on a Duplicate Titles action row so Undo can rebuild it.
 */
public final class DuplicateTitleSnapshots {

    private DuplicateTitleSnapshots() {
    }

    @Data
    @NoArgsConstructor
    public static class Payload {
        private String kind;
        private AuthorState keeperAuthor;
        private AuthorState sourceAuthor;
        private List<Long> movedBookIds = new ArrayList<>();
        private List<Long> movedAuthorPhotoIds = new ArrayList<>();
        private List<Long> movedAuthorFavoriteIds = new ArrayList<>();
        private BookState keeperBook;
        private BookState deletedBook;
        private List<Long> movedPhotoIds = new ArrayList<>();
        private List<Long> movedLoanIds = new ArrayList<>();
        private List<Long> movedPriceIds = new ArrayList<>();
        private List<Long> movedFavoriteIds = new ArrayList<>();
        private List<LoanState> removedLoans = new ArrayList<>();
        private List<PriceState> removedPrices = new ArrayList<>();
        private List<FavoriteState> removedFavorites = new ArrayList<>();
        private Long fixBookAId;
        private String fixBookATitle;
        private String fixBookAAlternate;
        private Long fixBookBId;
        private String fixBookBTitle;
        private String fixBookBAlternate;
    }

    @Data
    @NoArgsConstructor
    public static class AuthorState {
        private Long id;
        private String name;
        private List<String> alternateNames;
        private LocalDate dateOfBirth;
        private LocalDate dateOfDeath;
        private String religiousAffiliation;
        private String birthCountry;
        private String nationality;
        private String biographicalEssay;
        private String grokipediaUrl;

        static AuthorState from(Author author) {
            AuthorState state = new AuthorState();
            state.id = author.getId();
            state.name = author.getName();
            state.alternateNames = author.getAlternateNames() == null
                    ? null : new ArrayList<>(author.getAlternateNames());
            state.dateOfBirth = author.getDateOfBirth();
            state.dateOfDeath = author.getDateOfDeath();
            state.religiousAffiliation = author.getReligiousAffiliation();
            state.birthCountry = author.getBirthCountry();
            state.nationality = author.getNationality();
            state.biographicalEssay = author.getBiographicalEssay();
            state.grokipediaUrl = author.getGrokipediaUrl();
            return state;
        }
    }

    @Data
    @NoArgsConstructor
    public static class BookState {
        private Long id;
        private String title;
        private String alternateTitle;
        private Integer publicationYear;
        private String publisher;
        private String plotEssay;
        private String relatedWorks;
        private String detailedDescription;
        private String grokipediaUrl;
        private String freeTextUrl;
        private LocalDateTime dateAddedToLibrary;
        private LocalDateTime lastModified;
        private String status;
        private String locNumber;
        private Boolean electronicResource;
        private String statusReason;
        private Boolean ydlAudioAvailable;
        private Boolean ydlPaperAvailable;
        private Boolean ydlEbookAvailable;
        private LocalDateTime ydlLastChecked;
        private String ydlLookupError;
        private Boolean emuAudioAvailable;
        private Boolean emuPaperAvailable;
        private Boolean emuEbookAvailable;
        private LocalDateTime emuLastChecked;
        private String emuLookupError;
        private Boolean aclaAudioAvailable;
        private Boolean aclaPaperAvailable;
        private Boolean aclaEbookAvailable;
        private LocalDateTime aclaLastChecked;
        private String aclaLookupError;
        private String readingDifficulty;
        private String binding;
        private Integer desireToPurchase;
        private Long authorId;
        private Long libraryId;
        private List<String> tags = new ArrayList<>();

        static BookState from(Book book) {
            BookState state = new BookState();
            state.id = book.getId();
            state.title = book.getTitle();
            state.alternateTitle = book.getAlternateTitle();
            state.publicationYear = book.getPublicationYear();
            state.publisher = book.getPublisher();
            state.plotEssay = book.getPlotEssay();
            state.relatedWorks = book.getRelatedWorks();
            state.detailedDescription = book.getDetailedDescription();
            state.grokipediaUrl = book.getGrokipediaUrl();
            state.freeTextUrl = book.getFreeTextUrl();
            state.dateAddedToLibrary = book.getDateAddedToLibrary();
            state.lastModified = book.getLastModified();
            state.status = book.getStatus() == null ? null : book.getStatus().name();
            state.locNumber = book.getLocNumber();
            state.electronicResource = book.getElectronicResource();
            state.statusReason = book.getStatusReason();
            state.ydlAudioAvailable = book.getYdlAudioAvailable();
            state.ydlPaperAvailable = book.getYdlPaperAvailable();
            state.ydlEbookAvailable = book.getYdlEbookAvailable();
            state.ydlLastChecked = book.getYdlLastChecked();
            state.ydlLookupError = book.getYdlLookupError();
            state.emuAudioAvailable = book.getEmuAudioAvailable();
            state.emuPaperAvailable = book.getEmuPaperAvailable();
            state.emuEbookAvailable = book.getEmuEbookAvailable();
            state.emuLastChecked = book.getEmuLastChecked();
            state.emuLookupError = book.getEmuLookupError();
            state.aclaAudioAvailable = book.getAclaAudioAvailable();
            state.aclaPaperAvailable = book.getAclaPaperAvailable();
            state.aclaEbookAvailable = book.getAclaEbookAvailable();
            state.aclaLastChecked = book.getAclaLastChecked();
            state.aclaLookupError = book.getAclaLookupError();
            state.readingDifficulty = book.getReadingDifficulty() == null
                    ? null : book.getReadingDifficulty().getKey();
            state.binding = book.getBinding() == null ? null : book.getBinding().name();
            state.desireToPurchase = book.getDesireToPurchase();
            state.authorId = book.getAuthor() == null ? null : book.getAuthor().getId();
            state.libraryId = book.getLibrary() == null ? null : book.getLibrary().getId();
            state.tags = book.getTagsList() == null ? new ArrayList<>() : new ArrayList<>(book.getTagsList());
            return state;
        }
    }

    @Data
    @NoArgsConstructor
    public static class LoanState {
        private Long id;
        private Long bookId;
        private Long userId;
        private LocalDate loanDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private LocalDateTime lastModified;

        static LoanState from(Loan loan) {
            LoanState state = new LoanState();
            state.id = loan.getId();
            state.bookId = loan.getBook() == null ? null : loan.getBook().getId();
            state.userId = loan.getUser() == null ? null : loan.getUser().getId();
            state.loanDate = loan.getLoanDate();
            state.dueDate = loan.getDueDate();
            state.returnDate = loan.getReturnDate();
            state.lastModified = loan.getLastModified();
            return state;
        }
    }

    @Data
    @NoArgsConstructor
    public static class PriceState {
        private Long id;
        private Long bookId;
        private String cover;
        private BigDecimal priceDollars;
        private BigDecimal shippingDollars;
        private String condition;
        private LocalDateTime lookedUpAt;
        private String detailsUrl;
        private String lookupError;
        private LocalDateTime lastModified;

        static PriceState from(BookPrice price) {
            PriceState state = new PriceState();
            state.id = price.getId();
            state.bookId = price.getBook() == null ? null : price.getBook().getId();
            state.cover = price.getCover() == null ? null : price.getCover().name();
            state.priceDollars = price.getPriceDollars();
            state.shippingDollars = price.getShippingDollars();
            state.condition = price.getCondition();
            state.lookedUpAt = price.getLookedUpAt();
            state.detailsUrl = price.getDetailsUrl();
            state.lookupError = price.getLookupError();
            state.lastModified = price.getLastModified();
            return state;
        }
    }

    @Data
    @NoArgsConstructor
    public static class FavoriteState {
        private Long id;
        private Long userId;
        private Long bookId;
        private Long authorId;
        private String listName;
        private LocalDateTime lastModified;

        static FavoriteState from(Favorite favorite) {
            FavoriteState state = new FavoriteState();
            state.id = favorite.getId();
            state.userId = favorite.getUser() == null ? null : favorite.getUser().getId();
            state.bookId = favorite.getBook() == null ? null : favorite.getBook().getId();
            state.authorId = favorite.getAuthor() == null ? null : favorite.getAuthor().getId();
            state.listName = favorite.getListName();
            state.lastModified = favorite.getLastModified();
            return state;
        }
    }
}
