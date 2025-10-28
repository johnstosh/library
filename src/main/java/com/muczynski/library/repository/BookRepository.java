// (c) Copyright 2025 by Muczynski
package com.muczynski.library.repository;

import com.muczynski.library.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    @Modifying
    @Query("DELETE FROM Book b WHERE b.publisher = ?1")
    void deleteByPublisher(String publisher);
    long countByAuthorId(Long authorId);
}
