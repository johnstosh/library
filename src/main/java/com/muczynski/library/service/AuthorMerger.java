/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.AuthorNames;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Favorite;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.FavoriteRepository;
import com.muczynski.library.repository.PhotoRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Folds one author into another. The keeper stays. Books, portraits, and
 * favorites move onto the keeper, then the source row is deleted.
 * <p>
 * A blank field is filled from the other author. When both sides have text
 * and the text differs, the longer text is kept. A date is filled when the
 * keeper's date is missing, and otherwise stays with the keeper. The keeper's
 * name is not replaced, and the source's name is not copied into alternate names.
 */
@Service
public class AuthorMerger {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Moves {@code source} onto {@code keeper} and deletes {@code source}.
     * Both authors must already be persistent. The keeper's name is left unchanged.
     */
    public void merge(Author keeper, Author source) {
        if (keeper == null || source == null || keeper.getId() == null || source.getId() == null) {
            throw new IllegalArgumentException("Both authors must already be saved");
        }
        if (keeper.getId().equals(source.getId())) {
            return;
        }
        mergeFields(keeper, source);
        authorRepository.saveAndFlush(keeper);
        moveBooks(keeper, source);
        movePhotos(keeper, source);
        moveFavorites(keeper, source);
        entityManager.flush();
        // Author.photos is orphanRemoval. A portrait still sitting in the source
        // collection would be deleted with that row, even after its author id moved.
        if (Hibernate.isInitialized(source.getPhotos())) {
            entityManager.refresh(source);
        }
        authorRepository.delete(source);
        entityManager.flush();
    }

    /**
     * Copies fields onto {@code keeper}. Does not move books or delete {@code source}.
     */
    public static void mergeFields(Author keeper, Author source) {
        keeper.setReligiousAffiliation(preferLonger(keeper.getReligiousAffiliation(), source.getReligiousAffiliation()));
        keeper.setBirthCountry(preferLonger(keeper.getBirthCountry(), source.getBirthCountry()));
        keeper.setNationality(preferLonger(keeper.getNationality(), source.getNationality()));
        keeper.setBiographicalEssay(preferLonger(keeper.getBiographicalEssay(), source.getBiographicalEssay()));
        keeper.setGrokipediaUrl(preferLonger(keeper.getGrokipediaUrl(), source.getGrokipediaUrl()));
        keeper.setDateOfBirth(preferDate(keeper.getDateOfBirth(), source.getDateOfBirth()));
        keeper.setDateOfDeath(preferDate(keeper.getDateOfDeath(), source.getDateOfDeath()));
        List<String> combined = new ArrayList<>();
        if (keeper.getAlternateNames() != null) {
            combined.addAll(keeper.getAlternateNames());
        }
        if (source.getAlternateNames() != null) {
            combined.addAll(source.getAlternateNames());
        }
        keeper.setAlternateNames(AuthorNames.normalize(keeper.getName(), combined));
    }

    static String preferLonger(String keeper, String source) {
        String keeperText = blankToNull(keeper);
        String sourceText = blankToNull(source);
        if (keeperText == null) {
            return sourceText;
        }
        if (sourceText == null || sourceText.length() <= keeperText.length()) {
            return keeperText;
        }
        return sourceText;
    }

    static LocalDate preferDate(LocalDate keeper, LocalDate source) {
        return keeper != null ? keeper : source;
    }

    private void moveBooks(Author keeper, Author source) {
        for (Book book : bookRepository.findByAuthorIdOrderByTitleAsc(source.getId())) {
            book.setAuthor(keeper);
        }
    }

    private void movePhotos(Author keeper, Author source) {
        int next = 0;
        for (Photo photo : photoRepository.findByAuthorId(keeper.getId())) {
            if (photo.getPhotoOrder() != null && photo.getPhotoOrder() > next) {
                next = photo.getPhotoOrder();
            }
        }
        for (Photo photo : photoRepository.findByAuthorIdOrderByPhotoOrder(source.getId())) {
            photo.setPhotoOrder(++next);
            photo.setAuthor(keeper);
        }
    }

    private void moveFavorites(Author keeper, Author source) {
        for (Favorite favorite : new ArrayList<>(favoriteRepository.findByAuthor_Id(source.getId()))) {
            Optional<Favorite> existing = favoriteRepository.findByUser_IdAndListNameAndAuthor_Id(
                    favorite.getUser().getId(), favorite.getListName(), keeper.getId());
            if (existing.isPresent() && !existing.get().getId().equals(favorite.getId())) {
                favoriteRepository.delete(favorite);
            } else {
                favorite.setAuthor(keeper);
            }
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
