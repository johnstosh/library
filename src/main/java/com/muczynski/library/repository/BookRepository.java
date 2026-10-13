/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Interface projection for efficient saved book queries.
     * Avoids N+1 by using a single query with joins.
     */
    interface SavedBookProjection {
        Long getId();
        String getTitle();
        Long getAuthorId();
        String getAuthorName();
        String getLibraryName();
        Long getPhotoCount();
        String getLocNumber();
        String getStatus();
        String getGrokipediaUrl();
    }

    /**
     * Interface projection for book summaries (id + lastModified).
     * Used for cache validation with filter endpoints.
     */
    interface BookSummaryProjection {
        Long getId();
        LocalDateTime getLastModified();
    }

    /** Filtered list summaries, including the date the books page sorts on. */
    interface DatedBookSummaryProjection extends BookSummaryProjection {
        LocalDateTime getDateAddedToLibrary();
    }

    /**
     * Find books from most recent 2 days OR with temporary titles (date-pattern titles).
     * Uses native query for regex support and efficient projection.
     * Temporary titles match pattern: YYYY-M-D or YYYY-MM-DD at start of title.
     */
    @Query(value = """
        WITH most_recent_date AS (
            SELECT DATE(MAX(date_added_to_library)) as max_date FROM book
        )
        SELECT
            b.id as id,
            b.title as title,
            a.id as authorId,
            a.name as authorName,
            l.name as libraryName,
            COALESCE(p.photo_count, 0) as photoCount,
            b.loc_number as locNumber,
            b.status as status,
            b.grokipedia_url as grokipediaUrl
        FROM book b
        LEFT JOIN author a ON b.author_id = a.id
        LEFT JOIN library l ON b.library_id = l.id
        LEFT JOIN (
            SELECT book_id, COUNT(*) as photo_count
            FROM photo
            WHERE book_id IS NOT NULL
            GROUP BY book_id
        ) p ON b.id = p.book_id
        CROSS JOIN most_recent_date mrd
        WHERE DATE(b.date_added_to_library) >= mrd.max_date - INTERVAL '1 day'
           OR b.title ~ '^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}'
        ORDER BY b.date_added_to_library DESC
        """, nativeQuery = true)
    List<SavedBookProjection> findSavedBooksWithProjection();

    /**
     * Find book IDs with temporary titles (date-pattern titles like YYYY-M-D).
     * Efficient query that only returns IDs for batch processing.
     */
    @Query(value = "SELECT id FROM book WHERE title ~ '^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}'", nativeQuery = true)
    List<Long> findBookIdsWithTemporaryTitles();

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library")
    List<Book> findAllWithAuthorAndLibrary();

    /**
     * Batch query for streaming export: books with id > :lastId, ordered by id.
     * Two-step to safely combine keyset pagination with JOIN FETCH (avoids Hibernate limit issues).
     * First finds IDs (Pageable limit applied), then fetches full entities.
     */
    @Query("SELECT b.id FROM Book b WHERE b.id > :lastId ORDER BY b.id ASC")
    List<Long> findBookIdsAfterId(@Param("lastId") Long lastId, Pageable pageable);

    /**
     * Fetch full books (with JOIN FETCH) for given IDs. Used in streaming export batching.
     */
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.id IN :ids ORDER BY b.id ASC")
    List<Book> findBooksByIdsWithAuthorAndLibrary(@Param("ids") List<Long> ids);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCaseAndFreeTextUrlIsNotNull(String title, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCaseAndElectronicResourceTrue(String title, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCaseAndLocNumberIsNotNull(String title, Pageable pageable);
    void deleteByPublisher(String publisher);
    long countByAuthorId(Long authorId);
    List<Book> findByAuthorIdOrderByTitleAsc(Long authorId);
    /** @deprecated Use findAllByTitleAndAuthor_NameOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Book> findByTitleAndAuthor_Name(String title, String authorName);
    List<Book> findAllByTitleAndAuthor_NameOrderByIdAsc(String title, String authorName);
    /** @deprecated Use findAllByTitleAndAuthorIsNullOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Book> findByTitleAndAuthorIsNull(String title);
    List<Book> findAllByTitleAndAuthorIsNullOrderByIdAsc(String title);
    List<Book> findAllByTitleOrderByIdAsc(String title);

    @Modifying
    @Query("UPDATE Book b SET b.lastModified = :now WHERE b.lastModified IS NULL")
    int backfillLastModified(@Param("now") LocalDateTime now);

    @Query("SELECT MAX(b.dateAddedToLibrary) FROM Book b")
    LocalDateTime findMaxDateAddedToLibrary();

    @Query("SELECT b FROM Book b WHERE b.dateAddedToLibrary >= :startOfDay AND b.dateAddedToLibrary < :endOfDay ORDER BY b.dateAddedToLibrary DESC")
    List<Book> findByDateAddedToLibraryBetweenOrderByDateAddedDesc(LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.status = com.muczynski.library.domain.BookStatus.ACTIVE AND (b.locNumber IS NULL OR b.locNumber = '') AND (b.electronicResource IS NULL OR b.electronicResource = false)")
    List<Book> findBooksWithoutLocNumber();

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.grokipediaUrl IS NULL OR b.grokipediaUrl = '' OR b.grokipediaUrl = '-'")
    List<Book> findBooksWithoutGrokipediaUrl();

    boolean existsByTitle(String title);
    boolean existsByTitleAndIdNot(String title, Long id);

    @Query("SELECT b.title FROM Book b WHERE b.title = :title OR b.title LIKE :titlePattern")
    List<String> findTitlesByBasePattern(String title, String titlePattern);

    long countByLibraryId(Long libraryId);
    List<Book> findAllByLibraryId(Long libraryId);

    /**
     * Get summaries (id + lastModified) for all books.
     * Avoids loading full entities — used by /summaries endpoint for cache validation.
     */
    @Query("SELECT b.id as id, b.lastModified as lastModified FROM Book b")
    List<BookSummaryProjection> findAllSummaries();

    /**
     * Get summaries (id + lastModified) for ACTIVE books without LOC number.
     * Excludes electronic resources (not shelved) and non-ACTIVE statuses
     * (lost, withdrawn, on order, requested) per issue #337.
     */
    @Query("SELECT b.id as id, b.lastModified as lastModified FROM Book b WHERE b.status = com.muczynski.library.domain.BookStatus.ACTIVE AND (b.locNumber IS NULL OR b.locNumber = '') AND (b.electronicResource IS NULL OR b.electronicResource = false)")
    List<BookSummaryProjection> findSummariesWithoutLocNumber();

    /**
     * Get summaries (id + lastModified) for books without Grokipedia URL.
     */
    @Query("SELECT b.id as id, b.lastModified as lastModified FROM Book b WHERE b.grokipediaUrl IS NULL OR b.grokipediaUrl = '' OR b.grokipediaUrl = '-'")
    List<BookSummaryProjection> findSummariesWithoutGrokipediaUrl();

    /**
     * Active books for the checkout form. Each searched field adds 10 when the
     * text contains the term and 20 more when it is an exact match. Terms are
     * already trimmed and lowercased; the call-number term has no whitespace.
     * A zero flag ignores that field. At most {@code matchLimit} rows, best score first.
     */
    interface CheckoutMatchProjection {
        Long getId();
        String getTitle();
        String getAuthor();
        String getLocNumber();
        String getStatus();
    }

    @Query(value = """
        SELECT
            b.id AS "id",
            b.title AS "title",
            a.name AS "author",
            b.loc_number AS "locNumber",
            b.status AS "status"
        FROM book b
        LEFT JOIN author a ON a.id = b.author_id
        WHERE b.status = 'ACTIVE'
          AND (
            (:useTitle = 1 AND strpos(lower(b.title), :title) > 0)
            OR (:useAuthor = 1 AND (
                (a.name IS NOT NULL AND strpos(lower(a.name), :author) > 0)
                OR EXISTS (
                    SELECT 1 FROM unnest(a.alternate_names) AS alt
                    WHERE alt IS NOT NULL AND strpos(lower(alt), :author) > 0
                )
            ))
            OR (:useLoc = 1 AND strpos(regexp_replace(lower(coalesce(b.loc_number, '')), '\\s+', '', 'g'), :loc) > 0)
          )
        ORDER BY (
            (CASE WHEN :useTitle = 1 AND strpos(lower(b.title), :title) > 0 THEN 10 ELSE 0 END)
            + (CASE WHEN :useTitle = 1 AND lower(b.title) = :title THEN 20 ELSE 0 END)
            + (CASE WHEN :useAuthor = 1 AND (
                (a.name IS NOT NULL AND strpos(lower(a.name), :author) > 0)
                OR EXISTS (
                    SELECT 1 FROM unnest(a.alternate_names) AS alt
                    WHERE alt IS NOT NULL AND strpos(lower(alt), :author) > 0
                )
            ) THEN 10 ELSE 0 END)
            + (CASE WHEN :useAuthor = 1 AND (
                (a.name IS NOT NULL AND lower(a.name) = :author)
                OR EXISTS (
                    SELECT 1 FROM unnest(a.alternate_names) AS alt
                    WHERE alt IS NOT NULL AND lower(alt) = :author
                )
            ) THEN 20 ELSE 0 END)
            + (CASE WHEN :useLoc = 1 AND strpos(regexp_replace(lower(coalesce(b.loc_number, '')), '\\s+', '', 'g'), :loc) > 0 THEN 10 ELSE 0 END)
            + (CASE WHEN :useLoc = 1 AND regexp_replace(lower(coalesce(b.loc_number, '')), '\\s+', '', 'g') = :loc THEN 20 ELSE 0 END)
        ) DESC, b.title ASC, b.id ASC
        LIMIT :matchLimit
        """, nativeQuery = true)
    List<CheckoutMatchProjection> findCheckoutMatches(
            @Param("useTitle") int useTitle,
            @Param("title") String title,
            @Param("useAuthor") int useAuthor,
            @Param("author") String author,
            @Param("useLoc") int useLoc,
            @Param("loc") String loc,
            @Param("matchLimit") int matchLimit);

    /**
     * Get summaries (id + lastModified) for books from most recent 2 days OR with temporary titles.
     * Uses native query for regex support and PostgreSQL compatibility.
     */
    @Query(value = """
        WITH most_recent_date AS (
            SELECT DATE(MAX(date_added_to_library)) as max_date FROM book
        )
        SELECT b.id as id, b.last_modified as lastModified
        FROM book b
        CROSS JOIN most_recent_date mrd
        WHERE DATE(b.date_added_to_library) >= mrd.max_date - INTERVAL '1 day'
           OR b.title ~ '^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}'
        """, nativeQuery = true)
    List<BookSummaryProjection> findSummariesFromMostRecentDay();

    @Query("SELECT b.id, b.plotEssay, b.detailedDescription FROM Book b WHERE b.id IN :ids")
    List<Object[]> findPlotFieldsByIds(@Param("ids") List<Long> ids);

    @Query("SELECT b.id, b.title, a.name FROM Book b LEFT JOIN b.author a WHERE b.id IN :ids")
    List<Object[]> findTitleAndAuthorByIds(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT b.author.id FROM Book b WHERE b.author IS NOT NULL AND b.dateAddedToLibrary >= :start AND b.dateAddedToLibrary < :end")
    List<Long> findAuthorIdsAddedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count books that have the specified tag in their tagsList.
     */
    @Query("SELECT COUNT(DISTINCT b) FROM Book b JOIN b.tagsList t WHERE t = :tag")
    long countByTag(@Param("tag") String tag);

    long countByElectronicResourceTrue();

    /**
     * Count books with a non-blank LOC call number, excluding WITHDRAWN and REQUESTED.
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.locNumber IS NOT NULL AND TRIM(b.locNumber) <> '' "
            + "AND b.status <> com.muczynski.library.domain.BookStatus.WITHDRAWN "
            + "AND b.status <> com.muczynski.library.domain.BookStatus.REQUESTED")
    long countWithCallNumber();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.freeTextUrl IS NOT NULL AND TRIM(b.freeTextUrl) <> ''")
    long countWithFreeOnlineText();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.freeTextUrl IS NOT NULL AND LOWER(b.freeTextUrl) LIKE '%librivox%'")
    long countWithFreeOnlineAudio();

    long countByStatus(BookStatus status);

    long countByYdlPaperAvailableTrue();

    long countByYdlEbookAvailableTrue();

    long countByYdlAudioAvailableTrue();

    long countByEmuPaperAvailableTrue();

    long countByEmuEbookAvailableTrue();

    long countByEmuAudioAvailableTrue();

    long countByAclaPaperAvailableTrue();

    long countByAclaEbookAvailableTrue();

    long countByAclaAudioAvailableTrue();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.ydlPaperAvailable = true OR b.ydlEbookAvailable = true OR b.ydlAudioAvailable = true")
    long countAvailableAtYdl();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.emuPaperAvailable = true OR b.emuEbookAvailable = true OR b.emuAudioAvailable = true")
    long countAvailableAtEmu();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.aclaPaperAvailable = true OR b.aclaEbookAvailable = true OR b.aclaAudioAvailable = true")
    long countAvailableAtAcla();

    /**
     * Per-author counts of books held as paper/ebook/audio at YDL, EMU, and ACLA.
     * Authors with no matching books are omitted (treat missing as all false).
     */
    interface AuthorAvailabilityProjection {
        Long getAuthorId();
        Long getYdlPaperCount();
        Long getYdlEbookCount();
        Long getYdlAudioCount();
        Long getEmuPaperCount();
        Long getEmuEbookCount();
        Long getEmuAudioCount();
        Long getAclaPaperCount();
        Long getAclaEbookCount();
        Long getAclaAudioCount();
    }

    @Query("""
            SELECT b.author.id as authorId,
                   SUM(CASE WHEN b.ydlPaperAvailable = true THEN 1 ELSE 0 END) as ydlPaperCount,
                   SUM(CASE WHEN b.ydlEbookAvailable = true THEN 1 ELSE 0 END) as ydlEbookCount,
                   SUM(CASE WHEN b.ydlAudioAvailable = true THEN 1 ELSE 0 END) as ydlAudioCount,
                   SUM(CASE WHEN b.emuPaperAvailable = true THEN 1 ELSE 0 END) as emuPaperCount,
                   SUM(CASE WHEN b.emuEbookAvailable = true THEN 1 ELSE 0 END) as emuEbookCount,
                   SUM(CASE WHEN b.emuAudioAvailable = true THEN 1 ELSE 0 END) as emuAudioCount,
                   SUM(CASE WHEN b.aclaPaperAvailable = true THEN 1 ELSE 0 END) as aclaPaperCount,
                   SUM(CASE WHEN b.aclaEbookAvailable = true THEN 1 ELSE 0 END) as aclaEbookCount,
                   SUM(CASE WHEN b.aclaAudioAvailable = true THEN 1 ELSE 0 END) as aclaAudioCount
            FROM Book b
            WHERE b.author IS NOT NULL
            GROUP BY b.author.id
            """)
    List<AuthorAvailabilityProjection> countAvailabilityByAuthor();

    // Lightweight projection for photo ZIP import — skips @Lob fields (plotEssay, etc.)
    List<BookZipImportProjection> findBy();

    /**
     * Lightweight projection for maintenance genre cleanup.
     * Loads only id and tagsList (ElementCollection). Avoids all @Lob columns
     * (plotEssay, detailedDescription, freeTextUrl, etc.) and associations.
     * Critical for memory safety with ~2k book catalog.
     */
    interface IllegalGenreTagsProjection {
        Long getId();
        List<String> getTagsList();
    }

    /**
     * Returns all books' id + tagsList only. Used by MaintenanceService to avoid
     * loading LOBs or full entities. See comment on BookZipImportProjection.
     */
    @Query("SELECT b.id as id, b.tagsList as tagsList FROM Book b")
    List<IllegalGenreTagsProjection> findAllForGenreMaintenance();

    /**
     * Lightweight projection for duplicate-title scan (Issue #351).
     * Loads only id, title, alternateTitle, author name, and status — never @Lob fields.
     */
    interface DuplicateTitleProjection {
        Long getId();
        String getTitle();
        String getAlternateTitle();
        String getAuthorName();
        BookStatus getStatus();
    }

    /**
     * All books for duplicate-title matching. Avoids LOBs and full entities.
     */
    @Query("SELECT b.id as id, b.title as title, b.alternateTitle as alternateTitle, a.name as authorName, b.status as status FROM Book b LEFT JOIN b.author a")
    List<DuplicateTitleProjection> findAllForDuplicateTitleScan();
}
