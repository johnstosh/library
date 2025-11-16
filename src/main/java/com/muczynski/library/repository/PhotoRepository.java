/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByBookIdOrderByPhotoOrder(Long bookId);
    List<Photo> findByAuthorIdOrderByPhotoOrder(Long authorId);
    List<Photo> findByAuthorId(Long authorId);

    @Query("SELECT DISTINCT p FROM Photo p LEFT JOIN FETCH p.book LEFT JOIN FETCH p.author")
    List<Photo> findAllWithBookAndAuthor();

    @Query("SELECT DISTINCT p FROM Photo p LEFT JOIN FETCH p.book LEFT JOIN FETCH p.author ORDER BY p.id")
    List<Photo> findAllWithBookAndAuthorOrderById();
}