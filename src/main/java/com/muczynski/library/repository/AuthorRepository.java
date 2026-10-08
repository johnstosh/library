/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.ReadingDifficulty;
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
    @Query("SELECT a FROM Author a WHERE " +
        "LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
        "LOWER(COALESCE(array_to_string(a.alternateNames, '||'), '')) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Author> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
    void deleteByReligiousAffiliation(String religiousAffiliation);
    List<Author> findAllByNameOrderByIdAsc(String name);

    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") Long id);

    // Lightweight projection for photo ZIP import — skips @Lob fields (biographicalEssay, etc.)
    List<AuthorZipImportProjection> findBy();

    /**
     * Find authors who have at least one book matching ALL active type filters (no labels).
     * Used by SearchService when any filter chip is active; mirrors the WHERE conditions in
     * BookRepository.findWithFilters so authors track the filtered book result set.
     * Type filters use AND logic: a book must satisfy every active filter.
     */
    @Query("SELECT a FROM Author a WHERE " +
        "(:filterFavoriteAuthors = false OR a.id IN :favoriteAuthorIds) AND EXISTS (" +
        "SELECT 1 FROM Book b WHERE b.author = a AND " +
        "(:query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "OR LOWER(COALESCE(b.alternateTitle, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
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
        @Param("filterStatusLost") boolean filterStatusLost,
        @Param("filterStatusWithdrawn") boolean filterStatusWithdrawn,
        @Param("filterStatusOnOrder") boolean filterStatusOnOrder,
        @Param("filterStatusRequested") boolean filterStatusRequested,
        @Param("filterWithoutFreeTextUrls") boolean filterWithoutFreeTextUrls,
        @Param("filterYdlAudio") boolean filterYdlAudio,
        @Param("filterYdlBook") boolean filterYdlBook,
        @Param("filterYdlEbook") boolean filterYdlEbook,
        @Param("filterEmuAudio") boolean filterEmuAudio,
        @Param("filterEmuBook") boolean filterEmuBook,
        @Param("filterEmuEbook") boolean filterEmuEbook,
        @Param("filterAclaAudio") boolean filterAclaAudio,
        @Param("filterAclaBook") boolean filterAclaBook,
        @Param("filterAclaEbook") boolean filterAclaEbook,
        @Param("filterWithGrokipedia") boolean filterWithGrokipedia,
        @Param("filterReadingDifficulty") boolean filterReadingDifficulty,
        @Param("readingDifficulties") List<ReadingDifficulty> readingDifficulties,
        @Param("includeUnsetReadingDifficulty") boolean includeUnsetReadingDifficulty,
        @Param("filterFavoriteBooks") boolean filterFavoriteBooks,
        @Param("favoriteBookIds") List<Long> favoriteBookIds,
        @Param("filterFavoriteAuthors") boolean filterFavoriteAuthors,
        @Param("favoriteAuthorIds") List<Long> favoriteAuthorIds,
        Pageable pageable);

    /**
     * Find authors who have at least one book matching ALL active type filters AND all specified labels.
     * Used by SearchService when any filter chip or label is active; mirrors the WHERE conditions in
     * BookRepository.findWithFiltersAndLabels so authors track the filtered book result set.
     * Type filters use AND logic: a book must satisfy every active filter.
     */
    @Query("SELECT a FROM Author a WHERE " +
        "(:filterFavoriteAuthors = false OR a.id IN :favoriteAuthorIds) AND EXISTS (" +
        "SELECT 1 FROM Book b WHERE b.author = a AND " +
        "(:query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "OR LOWER(COALESCE(b.alternateTitle, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
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
        @Param("filterStatusLost") boolean filterStatusLost,
        @Param("filterStatusWithdrawn") boolean filterStatusWithdrawn,
        @Param("filterStatusOnOrder") boolean filterStatusOnOrder,
        @Param("filterStatusRequested") boolean filterStatusRequested,
        @Param("filterWithoutFreeTextUrls") boolean filterWithoutFreeTextUrls,
        @Param("filterYdlAudio") boolean filterYdlAudio,
        @Param("filterYdlBook") boolean filterYdlBook,
        @Param("filterYdlEbook") boolean filterYdlEbook,
        @Param("filterEmuAudio") boolean filterEmuAudio,
        @Param("filterEmuBook") boolean filterEmuBook,
        @Param("filterEmuEbook") boolean filterEmuEbook,
        @Param("filterAclaAudio") boolean filterAclaAudio,
        @Param("filterAclaBook") boolean filterAclaBook,
        @Param("filterAclaEbook") boolean filterAclaEbook,
        @Param("filterWithGrokipedia") boolean filterWithGrokipedia,
        @Param("labels") List<String> labels,
        @Param("labelCount") long labelCount,
        @Param("filterReadingDifficulty") boolean filterReadingDifficulty,
        @Param("readingDifficulties") List<ReadingDifficulty> readingDifficulties,
        @Param("includeUnsetReadingDifficulty") boolean includeUnsetReadingDifficulty,
        @Param("filterFavoriteBooks") boolean filterFavoriteBooks,
        @Param("favoriteBookIds") List<Long> favoriteBookIds,
        @Param("filterFavoriteAuthors") boolean filterFavoriteAuthors,
        @Param("favoriteAuthorIds") List<Long> favoriteAuthorIds,
        Pageable pageable);

    @Query("SELECT a FROM Author a WHERE a.id IN :ids AND " +
        "(:query = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
        "LOWER(COALESCE(array_to_string(a.alternateNames, '||'), '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
        "ORDER BY LOWER(a.name)")
    Page<Author> findByIdsAndNameContaining(
        @Param("ids") List<Long> ids,
        @Param("query") String query,
        Pageable pageable);

    /**
     * Interface projection for author summaries (id + lastModified).
     * Avoids loading @Lob biography fields used by /summaries cache validation.
     */
    interface AuthorSummaryProjection {
        Long getId();
        LocalDateTime getLastModified();
    }

    /** Authors-page summaries include the name so the page can sort before loading full rows. */
    interface NamedAuthorSummaryProjection extends AuthorSummaryProjection {
        String getName();
    }

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a")
    List<AuthorSummaryProjection> findAllSummaries();

    /** biographicalEssay maps to brief_biography, a PostgreSQL OID LOB — only IS NULL is valid, never compare to ''. */
    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE a.biographicalEssay IS NULL")
    List<AuthorSummaryProjection> findSummariesWithoutDescription();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE NOT EXISTS (SELECT 1 FROM Book b WHERE b.author = a)")
    List<AuthorSummaryProjection> findSummariesWithZeroBooks();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE a.grokipediaUrl IS NULL OR a.grokipediaUrl = '' OR a.grokipediaUrl = '-'")
    List<AuthorSummaryProjection> findSummariesWithoutGrokipedia();

    @Query("SELECT a.id as id, a.lastModified as lastModified FROM Author a WHERE a.id IN :ids")
    List<AuthorSummaryProjection> findSummariesByIds(@Param("ids") List<Long> ids);

    /** Keyset id page for chunked JSON export. */
    @Query("SELECT a.id FROM Author a WHERE a.id > :lastId ORDER BY a.id ASC")
    List<Long> findAuthorIdsAfterId(@Param("lastId") Long lastId, Pageable pageable);

    /**
     * Summaries for the Authors page. Every active chip ANDs. mostRecent ids come
     * from books added on the catalog's latest day, the same set as /most-recent-day.
     * biographicalEssay is a PostgreSQL OID, so only IS NULL is valid.
     */
    @Query("SELECT a.id as id, a.lastModified as lastModified, a.name as name FROM Author a WHERE " +
        "(:filterMostRecent = false OR a.id IN :mostRecentAuthorIds) AND " +
        "(:filterWithoutDescription = false OR a.biographicalEssay IS NULL) AND " +
        "(:filterWithoutGrokipedia = false OR a.grokipediaUrl IS NULL OR a.grokipediaUrl = '' OR a.grokipediaUrl = '-') AND " +
        "(:filterWithGrokipedia = false OR (a.grokipediaUrl IS NOT NULL AND a.grokipediaUrl <> '' AND a.grokipediaUrl <> '-')) AND " +
        "(:filterZeroBooks = false OR NOT EXISTS (SELECT 1 FROM Book zeroBook WHERE zeroBook.author = a)) AND " +
        "(:filterWithoutPhotos = false OR NOT EXISTS (SELECT 1 FROM Photo noPhoto WHERE noPhoto.author = a AND noPhoto.book IS NULL)) AND " +
        "(:filterWithPhotos = false OR EXISTS (SELECT 1 FROM Photo hasPhoto WHERE hasPhoto.author = a AND hasPhoto.book IS NULL)) AND " +
        "(:filterWithoutBirthDate = false OR a.dateOfBirth IS NULL) AND " +
        "(:filterWithoutDeathDate = false OR a.dateOfDeath IS NULL) AND " +
        "(:filterFavorites = false OR a.id IN :favoriteAuthorIds) AND " +
        "(:filterYdlBook = false OR EXISTS (SELECT 1 FROM Book ydlBook WHERE ydlBook.author = a AND ydlBook.ydlPaperAvailable = true)) AND " +
        "(:filterYdlEbook = false OR EXISTS (SELECT 1 FROM Book ydlEbook WHERE ydlEbook.author = a AND ydlEbook.ydlEbookAvailable = true)) AND " +
        "(:filterYdlAudio = false OR EXISTS (SELECT 1 FROM Book ydlAudio WHERE ydlAudio.author = a AND ydlAudio.ydlAudioAvailable = true)) AND " +
        "(:filterEmuBook = false OR EXISTS (SELECT 1 FROM Book emuBook WHERE emuBook.author = a AND emuBook.emuPaperAvailable = true)) AND " +
        "(:filterEmuEbook = false OR EXISTS (SELECT 1 FROM Book emuEbook WHERE emuEbook.author = a AND emuEbook.emuEbookAvailable = true)) AND " +
        "(:filterEmuAudio = false OR EXISTS (SELECT 1 FROM Book emuAudio WHERE emuAudio.author = a AND emuAudio.emuAudioAvailable = true)) AND " +
        "(:filterAclaBook = false OR EXISTS (SELECT 1 FROM Book aclaBook WHERE aclaBook.author = a AND aclaBook.aclaPaperAvailable = true)) AND " +
        "(:filterAclaEbook = false OR EXISTS (SELECT 1 FROM Book aclaEbook WHERE aclaEbook.author = a AND aclaEbook.aclaEbookAvailable = true)) AND " +
        "(:filterAclaAudio = false OR EXISTS (SELECT 1 FROM Book aclaAudio WHERE aclaAudio.author = a AND aclaAudio.aclaAudioAvailable = true))")
    List<NamedAuthorSummaryProjection> findFilteredSummaries(
        @Param("filterMostRecent") boolean filterMostRecent,
        @Param("mostRecentAuthorIds") List<Long> mostRecentAuthorIds,
        @Param("filterWithoutDescription") boolean filterWithoutDescription,
        @Param("filterWithoutGrokipedia") boolean filterWithoutGrokipedia,
        @Param("filterWithGrokipedia") boolean filterWithGrokipedia,
        @Param("filterZeroBooks") boolean filterZeroBooks,
        @Param("filterWithoutPhotos") boolean filterWithoutPhotos,
        @Param("filterWithPhotos") boolean filterWithPhotos,
        @Param("filterWithoutBirthDate") boolean filterWithoutBirthDate,
        @Param("filterWithoutDeathDate") boolean filterWithoutDeathDate,
        @Param("filterFavorites") boolean filterFavorites,
        @Param("favoriteAuthorIds") List<Long> favoriteAuthorIds,
        @Param("filterYdlBook") boolean filterYdlBook,
        @Param("filterYdlEbook") boolean filterYdlEbook,
        @Param("filterYdlAudio") boolean filterYdlAudio,
        @Param("filterEmuBook") boolean filterEmuBook,
        @Param("filterEmuEbook") boolean filterEmuEbook,
        @Param("filterEmuAudio") boolean filterEmuAudio,
        @Param("filterAclaBook") boolean filterAclaBook,
        @Param("filterAclaEbook") boolean filterAclaEbook,
        @Param("filterAclaAudio") boolean filterAclaAudio);
}
