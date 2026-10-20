/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.domain.DuplicateTitleAction;
import com.muczynski.library.domain.Favorite;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.DuplicateTitleActionResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.DuplicateTitleActionRepository;
import com.muczynski.library.repository.FavoriteRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.service.AuthorMerger;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.AuthorMergePlan;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.AuthorSide;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.TitlePlan;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.TitleSide;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.AuthorState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.BookState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.FavoriteState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.LoanState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.Payload;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.PriceState;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Merge and fix actions for one Duplicate Titles pair.
 * These operations are not a general author or book merge. Each change inserts
 * a row in {@code duplicate_title_action}. Undo inserts another row and does
 * not delete the earlier one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateTitleActionService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookService bookService;
    private final DuplicateTitleActionRepository actionRepository;
    private final AuthorMerger authorMerger;
    private final LoanRepository loanRepository;
    private final PhotoRepository photoRepository;
    private final BookPriceRepository bookPriceRepository;
    private final FavoriteRepository favoriteRepository;
    private final DuplicateTitleActionRestore restore;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Transactional
    public DuplicateTitleActionResultDto mergeAuthors(Long bookAId, Long bookBId) {
        Book bookA = book(bookAId);
        Book bookB = book(bookBId);
        requireDistinct(bookA, bookB);
        Author authorA = bookA.getAuthor();
        Author authorB = bookB.getAuthor();
        AuthorMergePlan plan = DuplicateTitlePlans.authorMerge(side(authorA), side(authorB));
        if (!plan.available()) {
            throw new LibraryException(plan.summary());
        }
        Author keeper = authorA.getId().equals(plan.keeperId()) ? authorA : authorB;
        Author source = keeper == authorA ? authorB : authorA;
        Payload payload = new Payload();
        payload.setKind(DuplicateTitleAction.MERGE_AUTHORS);
        payload.setKeeperAuthor(AuthorState.from(keeper));
        payload.setSourceAuthor(AuthorState.from(source));
        payload.setMovedBookIds(bookRepository.findByAuthorIdOrderByTitleAsc(source.getId()).stream()
                .map(Book::getId).toList());
        payload.setMovedAuthorPhotoIds(photoRepository.findByAuthorId(source.getId()).stream()
                .map(Photo::getId).toList());
        payload.setMovedAuthorFavoriteIds(favoriteRepository.findByAuthor_Id(source.getId()).stream()
                .map(Favorite::getId).toList());

        if (plan.survivingName() != null && !plan.survivingName().equals(keeper.getName())
                && source.getName() != null
                && plan.survivingName().equalsIgnoreCase(source.getName().trim())) {
            source.setName(source.getName() + " (duplicate-title merge " + source.getId() + ")");
            authorRepository.saveAndFlush(source);
        }
        if (plan.survivingName() != null) {
            keeper.setName(plan.survivingName());
        }
        keeper.setAlternateNames(new ArrayList<>(plan.alternateNames()));
        authorMerger.merge(keeper, source);
        log.info("Duplicate Titles merged author {} into {}", source.getId(), keeper.getId());
        return record(DuplicateTitleAction.MERGE_AUTHORS, bookA.getId(), bookB.getId(),
                plan.summary(), payload, null);
    }

    @Transactional
    public DuplicateTitleActionResultDto mergeTitles(Long bookAId, Long bookBId) {
        Book bookA = book(bookAId);
        Book bookB = book(bookBId);
        requireDistinct(bookA, bookB);
        if (DuplicateCatalogLabels.hasCallNumber(bookA.getLocNumber())
                && DuplicateCatalogLabels.hasCallNumber(bookB.getLocNumber())) {
            throw new LibraryException(
                    "Both books have call numbers. Use Fix titles on this Duplicate Titles row instead of merging.");
        }
        TitlePlan plan = DuplicateTitlePlans.titles(side(bookA), side(bookB));
        if (!DuplicateTitlePlans.MERGE_TITLES.equals(plan.actionKind())) {
            throw new LibraryException("This Duplicate Titles pair is not a title merge.");
        }
        Book keeper = bookA.getId().equals(plan.keeperId()) ? bookA : bookB;
        Book dropped = keeper == bookA ? bookB : bookA;
        Payload payload = new Payload();
        payload.setKind(DuplicateTitleAction.MERGE_TITLES);
        payload.setKeeperBook(BookState.from(keeper));
        payload.setDeletedBook(BookState.from(dropped));
        moveBookChildren(keeper, dropped, payload);
        keeper.setFreeTextUrl(plan.keeperFreeTextUrl());
        keeper.setElectronicResource(plan.keeperElectronic());
        keeper.setAlternateTitle(plan.keeperAlternate());
        keeper.setTagsList(unionTags(payload.getKeeperBook().getTags(), payload.getDeletedBook().getTags()));
        entityManager.flush();
        if (Hibernate.isInitialized(dropped.getPhotos())) {
            entityManager.refresh(dropped);
        }
        bookRepository.delete(dropped);
        entityManager.flush();
        log.info("Duplicate Titles merged book {} into {}", dropped.getId(), keeper.getId());
        return record(DuplicateTitleAction.MERGE_TITLES, bookA.getId(), bookB.getId(),
                plan.summary(), payload, null);
    }

    @Transactional
    public DuplicateTitleActionResultDto fixTitles(Long bookAId, Long bookBId) {
        Book bookA = book(bookAId);
        Book bookB = book(bookBId);
        requireDistinct(bookA, bookB);
        if (!DuplicateCatalogLabels.hasCallNumber(bookA.getLocNumber())
                || !DuplicateCatalogLabels.hasCallNumber(bookB.getLocNumber())) {
            throw new LibraryException(
                    "Fix titles is only for a Duplicate Titles pair where both books have call numbers.");
        }
        TitlePlan plan = DuplicateTitlePlans.titles(side(bookA), side(bookB));
        if (!plan.changeNeeded()) {
            throw new LibraryException(plan.summary());
        }
        Payload payload = new Payload();
        payload.setKind(DuplicateTitleAction.FIX_TITLES);
        payload.setFixBookAId(bookA.getId());
        payload.setFixBookATitle(bookA.getTitle());
        payload.setFixBookAAlternate(bookA.getAlternateTitle());
        payload.setFixBookBId(bookB.getId());
        payload.setFixBookBTitle(bookB.getTitle());
        payload.setFixBookBAlternate(bookB.getAlternateTitle());

        BookService.FixCopyTitles copies = bookService.allocateFixTitles(
                plan.leftTitle(), bookA.getId(), plan.rightTitle(), bookB.getId());
        String titleA = copies.leftTitle();
        String titleB = copies.rightTitle();
        String alternateA = DuplicateTitlePlans.alternateAfterTitleChange(
                payload.getFixBookAAlternate(), payload.getFixBookATitle(), titleA);
        String alternateB = DuplicateTitlePlans.alternateAfterTitleChange(
                payload.getFixBookBAlternate(), payload.getFixBookBTitle(), titleB);
        parkTitle(bookA, titleA);
        parkTitle(bookB, titleB);
        bookRepository.flush();
        bookA.setTitle(titleA);
        bookB.setTitle(titleB);
        bookA.setAlternateTitle(alternateA);
        bookB.setAlternateTitle(alternateB);
        bookRepository.flush();
        String summary = DuplicateTitlePlans.fixSummary(
                side(bookA.getId(), payload.getFixBookATitle(), payload.getFixBookAAlternate(), bookA),
                side(bookB.getId(), payload.getFixBookBTitle(), payload.getFixBookBAlternate(), bookB),
                titleA, titleB, alternateA, alternateB);
        log.info("Duplicate Titles fixed titles on books {} and {}", bookA.getId(), bookB.getId());
        return record(DuplicateTitleAction.FIX_TITLES, bookA.getId(), bookB.getId(), summary, payload, null);
    }

    @Transactional
    public DuplicateTitleActionResultDto undoAuthorMerge(Long actionId) {
        DuplicateTitleAction action = forward(actionId, DuplicateTitleAction.MERGE_AUTHORS);
        Payload payload = read(action);
        AuthorState source = payload.getSourceAuthor();
        AuthorState before = payload.getKeeperAuthor();
        Author keeper = authorRepository.findById(before.getId())
                .orElseThrow(() -> new LibraryException("Kept author #" + before.getId() + " was not found."));
        if (authorRepository.existsById(source.getId())) {
            throw new LibraryException("Author #" + source.getId() + " is already in the catalog.");
        }
        if (source.getName() != null && source.getName().equalsIgnoreCase(keeper.getName())) {
            keeper.setName(keeper.getName() + " (duplicate-title undo " + action.getId() + ")");
            authorRepository.saveAndFlush(keeper);
        }
        restore.insertAuthor(source);
        applyAuthor(keeper, before);
        authorRepository.saveAndFlush(keeper);
        restore.pointBooksAtAuthor(payload.getMovedBookIds(), source.getId());
        restore.pointPhotosAtAuthor(payload.getMovedAuthorPhotoIds(), source.getId());
        restore.pointFavoritesAtAuthor(payload.getMovedAuthorFavoriteIds(), source.getId());
        String summary = "Undo merge authors restores author #" + source.getId()
                + " \"" + source.getName() + "\" and moves its books, portraits, and favorites back.";
        log.info("Duplicate Titles undid author merge action {}", action.getId());
        return record(DuplicateTitleAction.UNDO_MERGE_AUTHORS, action.getBookAId(), action.getBookBId(),
                summary, null, action.getId());
    }

    @Transactional
    public DuplicateTitleActionResultDto undoTitleMerge(Long actionId) {
        DuplicateTitleAction action = forward(actionId, DuplicateTitleAction.MERGE_TITLES);
        Payload payload = read(action);
        BookState deleted = payload.getDeletedBook();
        BookState before = payload.getKeeperBook();
        if (bookRepository.existsById(deleted.getId())) {
            throw new LibraryException("Book #" + deleted.getId() + " is already in the catalog.");
        }
        Book keeper = bookRepository.findById(before.getId())
                .orElseThrow(() -> new LibraryException("Kept book #" + before.getId() + " was not found."));
        applyKeeperBook(keeper, before);
        bookRepository.saveAndFlush(keeper);
        restore.insertBook(deleted);
        restore.pointPhotosAtBook(payload.getMovedPhotoIds(), deleted.getId());
        restore.pointLoansAtBook(payload.getMovedLoanIds(), deleted.getId());
        restore.pointPricesAtBook(payload.getMovedPriceIds(), deleted.getId());
        restore.pointFavoritesAtBook(payload.getMovedFavoriteIds(), deleted.getId());
        for (LoanState loan : payload.getRemovedLoans()) {
            restore.insertLoan(loan);
        }
        for (PriceState price : payload.getRemovedPrices()) {
            restore.insertPrice(price);
        }
        for (FavoriteState favorite : payload.getRemovedFavorites()) {
            restore.insertFavorite(favorite);
        }
        String summary = "Undo merge titles restores book #" + deleted.getId()
                + " \"" + deleted.getTitle() + "\" and puts book #" + before.getId()
                + " back the way it was, including its call number and free-text URL.";
        log.info("Duplicate Titles undid title merge action {}", action.getId());
        return record(DuplicateTitleAction.UNDO_MERGE_TITLES, action.getBookAId(), action.getBookBId(),
                summary, null, action.getId());
    }

    @Transactional
    public DuplicateTitleActionResultDto undoFixTitles(Long actionId) {
        DuplicateTitleAction action = forward(actionId, DuplicateTitleAction.FIX_TITLES);
        Payload payload = read(action);
        Book bookA = book(payload.getFixBookAId());
        Book bookB = book(payload.getFixBookBId());
        parkTitle(bookA, payload.getFixBookATitle());
        parkTitle(bookB, payload.getFixBookBTitle());
        bookRepository.flush();
        bookA.setTitle(payload.getFixBookATitle());
        bookA.setAlternateTitle(payload.getFixBookAAlternate());
        bookB.setTitle(payload.getFixBookBTitle());
        bookB.setAlternateTitle(payload.getFixBookBAlternate());
        bookRepository.flush();
        String summary = "Undo fix titles puts book #" + bookA.getId()
                + " title back to \"" + payload.getFixBookATitle()
                + "\" and book #" + bookB.getId() + " title back to \""
                + payload.getFixBookBTitle() + "\". Call numbers were not changed.";
        log.info("Duplicate Titles undid title fix action {}", action.getId());
        return record(DuplicateTitleAction.UNDO_FIX_TITLES, action.getBookAId(), action.getBookBId(),
                summary, null, action.getId());
    }

    private void moveBookChildren(Book keeper, Book dropped, Payload payload) {
        for (Photo photo : photoRepository.findByBookIdOrderByPhotoOrder(dropped.getId())) {
            photo.setBook(keeper);
            payload.getMovedPhotoIds().add(photo.getId());
        }
        for (Loan loan : loanRepository.findByBook_Id(dropped.getId())) {
            Optional<Loan> clash = loanRepository.findByBookIdAndUserIdAndLoanDate(
                    keeper.getId(), loan.getUser().getId(), loan.getLoanDate());
            if (clash.isPresent() && !clash.get().getId().equals(loan.getId())) {
                payload.getRemovedLoans().add(DuplicateTitleSnapshots.LoanState.from(loan));
                loanRepository.delete(loan);
            } else {
                loan.setBook(keeper);
                payload.getMovedLoanIds().add(loan.getId());
            }
        }
        for (BookPrice price : bookPriceRepository.findByBook_Id(dropped.getId())) {
            Optional<BookPrice> clash = bookPriceRepository.findByBook_IdAndCover(keeper.getId(), price.getCover());
            if (clash.isPresent() && !clash.get().getId().equals(price.getId())) {
                payload.getRemovedPrices().add(DuplicateTitleSnapshots.PriceState.from(price));
                bookPriceRepository.delete(price);
            } else {
                price.setBook(keeper);
                payload.getMovedPriceIds().add(price.getId());
            }
        }
        for (Favorite favorite : favoriteRepository.findByBook_Id(dropped.getId())) {
            Optional<Favorite> clash = favoriteRepository.findByUser_IdAndListNameAndBook_Id(
                    favorite.getUser().getId(), favorite.getListName(), keeper.getId());
            if (clash.isPresent() && !clash.get().getId().equals(favorite.getId())) {
                payload.getRemovedFavorites().add(DuplicateTitleSnapshots.FavoriteState.from(favorite));
                favoriteRepository.delete(favorite);
            } else {
                favorite.setBook(keeper);
                payload.getMovedFavoriteIds().add(favorite.getId());
            }
        }
        entityManager.flush();
    }

    private void applyAuthor(Author keeper, AuthorState before) {
        keeper.setName(before.getName());
        keeper.setAlternateNames(before.getAlternateNames() == null
                ? null : new ArrayList<>(before.getAlternateNames()));
        keeper.setDateOfBirth(before.getDateOfBirth());
        keeper.setDateOfDeath(before.getDateOfDeath());
        keeper.setReligiousAffiliation(before.getReligiousAffiliation());
        keeper.setBirthCountry(before.getBirthCountry());
        keeper.setNationality(before.getNationality());
        keeper.setBiographicalEssay(before.getBiographicalEssay());
        keeper.setGrokipediaUrl(before.getGrokipediaUrl());
    }

    private void applyKeeperBook(Book keeper, BookState before) {
        keeper.setFreeTextUrl(before.getFreeTextUrl());
        keeper.setElectronicResource(before.getElectronicResource());
        keeper.setAlternateTitle(before.getAlternateTitle());
        keeper.setTagsList(before.getTags() == null ? new ArrayList<>() : new ArrayList<>(before.getTags()));
    }

    private void parkTitle(Book book, String finalTitle) {
        if (finalTitle != null && finalTitle.equals(book.getTitle())) {
            return;
        }
        book.setTitle(book.getTitle() + " (duplicate-title temp " + book.getId() + ")");
    }

    private DuplicateTitleAction forward(Long actionId, String kind) {
        if (actionId == null) {
            throw new LibraryException("Choose the Duplicate Titles change to undo.");
        }
        DuplicateTitleAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new LibraryException("Duplicate Titles change #" + actionId + " was not found."));
        if (!kind.equals(action.getActionKind())) {
            throw new LibraryException("That change is not a Duplicate Titles " + kind.replace('_', ' ').toLowerCase() + ".");
        }
        if (actionRepository.existsByReversesActionId(action.getId())) {
            throw new LibraryException("That Duplicate Titles change was already undone.");
        }
        return action;
    }

    private Payload read(DuplicateTitleAction action) {
        try {
            return objectMapper.readValue(action.getSnapshotJson(), Payload.class);
        } catch (JsonProcessingException ex) {
            throw new LibraryException("Could not read the Duplicate Titles change record.", ex);
        }
    }

    private DuplicateTitleActionResultDto record(String kind, Long bookAId, Long bookBId, String summary,
                                                  Payload payload, Long reversesActionId) {
        DuplicateTitleAction row = new DuplicateTitleAction();
        row.setActionKind(kind);
        row.setBookAId(bookAId);
        row.setBookBId(bookBId);
        row.setSummary(summary);
        row.setReversesActionId(reversesActionId);
        if (payload != null) {
            try {
                row.setSnapshotJson(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException ex) {
                throw new LibraryException("Could not record the Duplicate Titles change.", ex);
            }
        }
        actionRepository.saveAndFlush(row);
        return DuplicateTitleActionResultDto.builder()
                .actionId(row.getId())
                .actionKind(kind)
                .summary(summary)
                .build();
    }

    private Book book(Long id) {
        if (id == null) {
            throw new LibraryException("Choose both books on the Duplicate Titles row.");
        }
        return bookRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Book #" + id + " was not found."));
    }

    private void requireDistinct(Book bookA, Book bookB) {
        if (bookA.getId().equals(bookB.getId())) {
            throw new LibraryException("Choose two different books on the Duplicate Titles row.");
        }
    }

    private static AuthorSide side(Author author) {
        if (author == null || author.getId() == null) {
            return new AuthorSide(0, null, List.of());
        }
        return new AuthorSide(author.getId(), author.getName(), author.getAlternateNames());
    }

    private static TitleSide side(Book book) {
        return new TitleSide(
                book.getId(),
                book.getTitle(),
                book.getAlternateTitle(),
                book.getLocNumber(),
                Boolean.TRUE.equals(book.getElectronicResource()),
                book.getFreeTextUrl());
    }

    private static TitleSide side(long id, String title, String alternate, Book book) {
        return new TitleSide(id, title, alternate, book.getLocNumber(),
                Boolean.TRUE.equals(book.getElectronicResource()), book.getFreeTextUrl());
    }

    private static List<String> unionTags(List<String> keeper, List<String> dropped) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (keeper != null) {
            tags.addAll(keeper);
        }
        if (dropped != null) {
            tags.addAll(dropped);
        }
        return new ArrayList<>(tags);
    }
}
