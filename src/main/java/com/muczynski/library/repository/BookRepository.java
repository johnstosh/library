/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCaseAndFreeTextUrlIsNotNull(String title, Pageable pageable);
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

    @Query("SELECT MAX(b.dateAddedToLibrary) FROM Book b")
    LocalDateTime findMaxDateAddedToLibrary();

    @Query("SELECT b FROM Book b WHERE b.dateAddedToLibrary >= :startOfDay AND b.dateAddedToLibrary < :endOfDay ORDER BY b.dateAddedToLibrary DESC")
    List<Book> findByDateAddedToLibraryBetweenOrderByDateAddedDesc(LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.locNumber IS NOT NULL AND b.locNumber != '' AND LENGTH(SUBSTRING(b.locNumber, 1, 3)) = 3 AND SUBSTRING(b.locNumber, 1, 1) BETWEEN 'A' AND 'Z' AND SUBSTRING(b.locNumber, 2, 1) BETWEEN 'A' AND 'Z' AND SUBSTRING(b.locNumber, 3, 1) BETWEEN 'A' AND 'Z' ORDER BY b.dateAddedToLibrary DESC")
    List<Book> findBooksWith3LetterLocStart();

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.locNumber IS NULL OR b.locNumber = ''")
    List<Book> findBooksWithoutLocNumber();

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.grokipediaUrl IS NULL OR b.grokipediaUrl = ''")
    List<Book> findBooksWithoutGrokipediaUrl();

    boolean existsByTitle(String title);
    boolean existsByTitleAndIdNot(String title, Long id);

    @Query("SELECT b.title FROM Book b WHERE b.title = :title OR b.title LIKE :titlePattern")
    List<String> findTitlesByBasePattern(String title, String titlePattern);

    long countByLibraryId(Long libraryId);
    List<Book> findAllByLibraryId(Long libraryId);

    /**
     * Get summaries (id + lastModified) for books without LOC number.
     */
    @Query("SELECT b.id as id, b.lastModified as lastModified FROM Book b WHERE b.locNumber IS NULL OR b.locNumber = ''")
    List<BookSummaryProjection> findSummariesWithoutLocNumber();

    /**
     * Get summaries (id + lastModified) for books with 3-letter LOC start.
     */
    @Query("SELECT b.id as id, b.lastModified as lastModified FROM Book b WHERE b.locNumber IS NOT NULL AND b.locNumber != '' AND LENGTH(SUBSTRING(b.locNumber, 1, 3)) = 3 AND SUBSTRING(b.locNumber, 1, 1) BETWEEN 'A' AND 'Z' AND SUBSTRING(b.locNumber, 2, 1) BETWEEN 'A' AND 'Z' AND SUBSTRING(b.locNumber, 3, 1) BETWEEN 'A' AND 'Z'")
    List<BookSummaryProjection> findSummariesWith3LetterLocStart();

    /**
     * Get summaries (id + lastModified) for books without Grokipedia URL.
     */
    @Query("SELECT b.id as id, b.lastModified as lastModified FROM Book b WHERE b.grokipediaUrl IS NULL OR b.grokipediaUrl = ''")
    List<BookSummaryProjection> findSummariesWithoutGrokipediaUrl();

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
}
