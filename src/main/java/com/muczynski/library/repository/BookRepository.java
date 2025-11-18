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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    void deleteByPublisher(String publisher);
    long countByAuthorId(Long authorId);
    Optional<Book> findByTitleAndAuthor_Name(String title, String authorName);
    List<Book> findAllByTitleAndAuthor_NameOrderByIdAsc(String title, String authorName);
    Optional<Book> findByTitleAndAuthorIsNull(String title);
    List<Book> findAllByTitleAndAuthorIsNullOrderByIdAsc(String title);

    List<Book> findByDateAddedToLibraryOrderByTitleAsc(LocalDate dateAddedToLibrary);

    @Query("SELECT MAX(b.dateAddedToLibrary) FROM Book b")
    LocalDate findMaxDateAddedToLibrary();
}
