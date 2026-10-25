/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.BookListing;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookListingRepository extends JpaRepository<BookListing, Long> {

    void deleteByBook_Id(Long bookId);

    Optional<BookListing> findByAbeListingId(Long abeListingId);

    void deleteByAbeListingIdIn(Collection<Long> abeListingIds);

    @Query("SELECT l FROM BookListing l JOIN FETCH l.book b LEFT JOIN FETCH b.author JOIN FETCH l.seller")
    List<BookListing> findAllWithRefs();

    @Query("SELECT l FROM BookListing l JOIN FETCH l.book b LEFT JOIN FETCH b.author "
            + "JOIN FETCH l.seller WHERE l.book.id IN :bookIds")
    List<BookListing> findByBookIdsWithSeller(@Param("bookIds") List<Long> bookIds);

    @Query("SELECT l.seller.id, COUNT(l) FROM BookListing l GROUP BY l.seller.id")
    List<Object[]> countBySellerId();

    @Query("SELECT l.id FROM BookListing l WHERE l.id > :lastId ORDER BY l.id ASC")
    List<Long> findIdsAfterId(@Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT l FROM BookListing l JOIN FETCH l.book b LEFT JOIN FETCH b.author "
            + "JOIN FETCH l.seller WHERE l.id IN :ids")
    List<BookListing> findByIdsWithRefs(@Param("ids") List<Long> ids);
}
