/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.dto.BookSummaryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookPriceRepository extends JpaRepository<BookPrice, Long> {

    Optional<BookPrice> findByBook_IdAndCover(Long bookId, BookCoverType cover);

    List<BookPrice> findByBook_Id(Long bookId);

    @Query("SELECT p FROM BookPrice p JOIN FETCH p.book b LEFT JOIN FETCH b.author")
    List<BookPrice> findAllWithBookAndAuthor();

    /**
     * Fetch specific prices with JOINs for full DTOs (used by /by-ids).
     */
    @Query("SELECT p FROM BookPrice p JOIN FETCH p.book b LEFT JOIN FETCH b.author WHERE p.id IN :ids")
    List<BookPrice> findByIdsWithBookAndAuthor(@Param("ids") List<Long> ids);

    /**
     * Lightweight summaries (id + lastModified) for frontend caching.
     * Mirrors BookRepository.findAllSummaries().
     */
    @Query("SELECT p.id as id, p.lastModified as lastModified FROM BookPrice p")
    List<BookSummaryDto> findAllPriceSummaries();

    /**
     * Unique books with at least one saved listing that has a price and no lookup error.
     * Failed lookups (no matching listing, timeout, rate limit, HTTP errors) are excluded.
     */
    @Query("SELECT COUNT(DISTINCT p.book.id) FROM BookPrice p "
            + "WHERE p.priceDollars IS NOT NULL "
            + "AND (p.lookupError IS NULL OR p.lookupError = '')")
    long countDistinctBooksWithValidPrices();
}
