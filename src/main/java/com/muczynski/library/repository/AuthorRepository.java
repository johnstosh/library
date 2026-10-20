/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    void deleteByReligiousAffiliation(String religiousAffiliation);
    List<Author> findAllByNameOrderByIdAsc(String name);

    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") Long id);

    // Lightweight projection for photo ZIP import — skips @Lob fields (biographicalEssay, etc.)
    List<AuthorZipImportProjection> findBy();

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

    /**
     * Names for a Duplicate Titles pair. Skips biography and the other large text columns.
     */
    @Query(value = "SELECT id, name, alternate_names FROM author WHERE id IN (:ids)", nativeQuery = true)
    List<Object[]> findDuplicateTitleAuthorNames(@Param("ids") Collection<Long> ids);

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
     * Authors for the book form. The term is already trimmed and lowercased.
     * A canonical or alternate name that contains it scores 10, and an exact
     * match scores 20 more. At most {@code matchLimit} rows, best score first.
     */
    interface AuthorNameMatchProjection {
        Long getId();
        String getName();
    }

    @Query(value = """
        SELECT a.id AS "id", a.name AS "name"
        FROM author a
        WHERE (a.name IS NOT NULL AND strpos(lower(a.name), :term) > 0)
           OR EXISTS (
                SELECT 1 FROM unnest(a.alternate_names) AS alt
                WHERE alt IS NOT NULL AND strpos(lower(alt), :term) > 0
           )
        ORDER BY (
            (CASE WHEN a.name IS NOT NULL AND strpos(lower(a.name), :term) > 0 THEN 10 ELSE 0 END)
            + (CASE WHEN a.name IS NOT NULL AND lower(a.name) = :term THEN 20 ELSE 0 END)
            + (CASE WHEN EXISTS (
                SELECT 1 FROM unnest(a.alternate_names) AS alt
                WHERE alt IS NOT NULL AND strpos(lower(alt), :term) > 0
            ) THEN 10 ELSE 0 END)
            + (CASE WHEN EXISTS (
                SELECT 1 FROM unnest(a.alternate_names) AS alt
                WHERE alt IS NOT NULL AND lower(alt) = :term
            ) THEN 20 ELSE 0 END)
        ) DESC, a.name ASC, a.id ASC
        LIMIT :matchLimit
        """, nativeQuery = true)
    List<AuthorNameMatchProjection> findNameMatches(
            @Param("term") String term,
            @Param("matchLimit") int matchLimit);

}
