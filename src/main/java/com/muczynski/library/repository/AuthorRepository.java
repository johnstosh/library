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

import java.time.LocalDateTime;
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
     * Find authors who have at least one book matching ALL active type filters (no labels).
     * Used by SearchService when any filter chip is active; mirrors the WHERE conditions in
     * BookRepository.findWithFilters so authors track the filtered book result set.
     * Type filters use AND logic: a book must satisfy every active filter.
     */
    @Query("SELECT a FROM Author a WHERE EXISTS (" +
        "SELECT 1 FROM Book b WHERE b.author = a AND " +
        "(:query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
        BookRepository.SEARCH_CHIP_PREDICATE + ") " +
        "ORDER BY LOWER(a.name)")
    Page<Author> findAuthorsOfBooksMatchingFilters(
        @Param("query") String query,
        @Param("filterInLibrary") boolean filterInLibrary,
        @Param("filterElectronic") boolean filterElectronic,
        @Param("filterFreeText") boolean filterFreeText,
        @Param("filterAudio") boolean filterAudio,
        @Param("filterMostRecent") boolean filterMostRecent,
        @Param("mostRecentCutoff") LocalDateTime mostRecentCutoff,
        @Param("mostRecentTempTitleIds") List<Long> mostRecentTempTitleIds,
        @Param("filterWithoutLoc") boolean filterWithoutLoc,
        @Param("filterThreeLetterLoc") boolean filterThreeLetterLoc,
        @Param("filterWithoutGrokipedia") boolean filterWithoutGrokipedia,
        @Param("filterWithoutGenres") boolean filterWithoutGenres,
        @Param("filterNotActiveStatus") boolean filterNotActiveStatus,
        @Param("filterWithoutFreeTextUrls") boolean filterWithoutFreeTextUrls,
        Pageable pageable);

    /**
     * Find authors who have at least one book matching ALL active type filters AND all specified labels.
     * Used by SearchService when any filter chip or label is active; mirrors the WHERE conditions in
     * BookRepository.findWithFiltersAndLabels so authors track the filtered book result set.
     * Type filters use AND logic: a book must satisfy every active filter.
     */
    @Query("SELECT a FROM Author a WHERE EXISTS (" +
        "SELECT 1 FROM Book b WHERE b.author = a AND " +
        "(:query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
        "(SELECT COUNT(t) FROM Book b2 JOIN b2.tagsList t WHERE b2 = b AND t IN :labels) = :labelCount AND " +
        BookRepository.SEARCH_CHIP_PREDICATE + ") " +
        "ORDER BY LOWER(a.name)")
    Page<Author> findAuthorsOfBooksMatchingFiltersAndLabels(
        @Param("query") String query,
        @Param("filterInLibrary") boolean filterInLibrary,
        @Param("filterElectronic") boolean filterElectronic,
        @Param("filterFreeText") boolean filterFreeText,
        @Param("filterAudio") boolean filterAudio,
        @Param("filterMostRecent") boolean filterMostRecent,
        @Param("mostRecentCutoff") LocalDateTime mostRecentCutoff,
        @Param("mostRecentTempTitleIds") List<Long> mostRecentTempTitleIds,
        @Param("filterWithoutLoc") boolean filterWithoutLoc,
        @Param("filterThreeLetterLoc") boolean filterThreeLetterLoc,
        @Param("filterWithoutGrokipedia") boolean filterWithoutGrokipedia,
        @Param("filterWithoutGenres") boolean filterWithoutGenres,
        @Param("filterNotActiveStatus") boolean filterNotActiveStatus,
        @Param("filterWithoutFreeTextUrls") boolean filterWithoutFreeTextUrls,
        @Param("labels") List<String> labels,
        @Param("labelCount") long labelCount,
        Pageable pageable);

    /**
     * Interface projection for author summaries (id + lastModified).
     * Avoids loading @Lob biography fields used by /summaries cache validation.
     */
    interface AuthorSummaryProjection {
        Long getId();
        LocalDateTime getLastModified();
    }

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a")
    List<AuthorSummaryProjection> findAllSummaries();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE a.briefBiography IS NULL OR a.briefBiography = ''")
    List<AuthorSummaryProjection> findSummariesWithoutDescription();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE NOT EXISTS (SELECT 1 FROM Book b WHERE b.author = a)")
    List<AuthorSummaryProjection> findSummariesWithZeroBooks();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE a.grokipediaUrl IS NULL OR a.grokipediaUrl = ''")
    List<AuthorSummaryProjection> findSummariesWithoutGrokipedia();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE a.id IN :ids")
    List<AuthorSummaryProjection> findSummariesByIds(@Param("ids") List<Long> ids);
}
