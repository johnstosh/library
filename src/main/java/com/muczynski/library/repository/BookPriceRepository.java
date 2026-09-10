/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookPriceRepository extends JpaRepository<BookPrice, Long> {

    Optional<BookPrice> findByBook_IdAndCover(Long bookId, BookCoverType cover);

    List<BookPrice> findByBook_Id(Long bookId);

    @Query("SELECT p FROM BookPrice p JOIN FETCH p.book b LEFT JOIN FETCH b.author")
    List<BookPrice> findAllWithBookAndAuthor();
}
