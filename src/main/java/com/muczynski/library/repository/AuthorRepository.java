/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);
    void deleteByReligiousAffiliation(String religiousAffiliation);
    List<Author> findAllByNameOrderByIdAsc(String name);

    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") Long id);

    // Lightweight projection for photo ZIP import — skips @Lob fields (briefBiography, etc.)
    List<AuthorZipImportProjection> findBy();

    /**
     * Find authors who have at least one book matching the active type filters (no labels).
     * Used by SearchService when any filter chip is active; mirrors the WHERE conditions in
     * BookRepository.findWithFilters so authors track the filtered book result set.
     */
    @Query("SELECT a FROM Author a WHERE EXISTS (" +
        "SELECT 1 FROM Book b WHERE b.author = a AND " +
        "(:query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
        "((:filterInLibrary = false AND :filterElectronic = false AND :filterFreeText = false AND :filterAudio = false) OR " +
        "(:filterInLibrary = true AND b.locNumber IS NOT NULL AND b.locNumber <> '') OR " +
        "(:filterElectronic = true AND b.electronicResource = true) OR " +
        "(:filterFreeText = true AND b.freeTextUrl IS NOT NULL) OR " +
        "(:filterAudio = true AND b.freeTextUrl IS NOT NULL AND LOWER(b.freeTextUrl) LIKE '%librivox%'))) " +
        "ORDER BY LOWER(a.name)")
    Page<Author> findAuthorsOfBooksMatchingFilters(
        @Param("query") String query,
        @Param("filterInLibrary") boolean filterInLibrary,
        @Param("filterElectronic") boolean filterElectronic,
        @Param("filterFreeText") boolean filterFreeText,
        @Param("filterAudio") boolean filterAudio,
        Pageable pageable);

    /**
     * Find authors who have at least one book matching the active type filters AND all specified labels.
     * Used by SearchService when any filter chip or label is active; mirrors the WHERE conditions in
     * BookRepository.findWithFiltersAndLabels so authors track the filtered book result set.
     */
    @Query("SELECT a FROM Author a WHERE EXISTS (" +
        "SELECT 1 FROM Book b WHERE b.author = a AND " +
        "(:query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
        "(SELECT COUNT(t) FROM Book b2 JOIN b2.tagsList t WHERE b2 = b AND t IN :labels) = :labelCount AND " +
        "((:filterInLibrary = false AND :filterElectronic = false AND :filterFreeText = false AND :filterAudio = false) OR " +
        "(:filterInLibrary = true AND b.locNumber IS NOT NULL AND b.locNumber <> '') OR " +
        "(:filterElectronic = true AND b.electronicResource = true) OR " +
        "(:filterFreeText = true AND b.freeTextUrl IS NOT NULL) OR " +
        "(:filterAudio = true AND b.freeTextUrl IS NOT NULL AND LOWER(b.freeTextUrl) LIKE '%librivox%'))) " +
        "ORDER BY LOWER(a.name)")
    Page<Author> findAuthorsOfBooksMatchingFiltersAndLabels(
        @Param("query") String query,
        @Param("filterInLibrary") boolean filterInLibrary,
        @Param("filterElectronic") boolean filterElectronic,
        @Param("filterFreeText") boolean filterFreeText,
        @Param("filterAudio") boolean filterAudio,
        @Param("labels") List<String> labels,
        @Param("labelCount") long labelCount,
        Pageable pageable);
}
