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

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library")
    List<Book> findAllWithAuthorAndLibrary();
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    void deleteByPublisher(String publisher);
    long countByAuthorId(Long authorId);
    List<Book> findByAuthorIdOrderByTitleAsc(Long authorId);
    Optional<Book> findByTitleAndAuthor_Name(String title, String authorName);
    List<Book> findAllByTitleAndAuthor_NameOrderByIdAsc(String title, String authorName);
    Optional<Book> findByTitleAndAuthorIsNull(String title);
    List<Book> findAllByTitleAndAuthorIsNullOrderByIdAsc(String title);

    @Query("SELECT MAX(b.dateAddedToLibrary) FROM Book b")
    LocalDateTime findMaxDateAddedToLibrary();

    @Query("SELECT b FROM Book b WHERE b.dateAddedToLibrary >= :startOfDay AND b.dateAddedToLibrary < :endOfDay ORDER BY b.dateAddedToLibrary DESC")
    List<Book> findByDateAddedToLibraryBetweenOrderByDateAddedDesc(LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.library WHERE b.locNumber IS NULL OR b.locNumber = ''")
    List<Book> findBooksWithoutLocNumber();

    long countByLibraryId(Long libraryId);
}
