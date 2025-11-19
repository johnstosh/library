/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByBookIdOrderByPhotoOrder(Long bookId);
    List<Photo> findByAuthorIdOrderByPhotoOrder(Long authorId);
    List<Photo> findByAuthorId(Long authorId);
    Optional<Photo> findByPermanentId(String permanentId);
    Optional<Photo> findByImageChecksum(String imageChecksum);
    List<Photo> findByBookIdAndPhotoOrderOrderByIdAsc(Long bookId, Integer photoOrder);
    List<Photo> findByAuthorIdAndBookIsNullAndPhotoOrderOrderByIdAsc(Long authorId, Integer photoOrder);

    @Query("SELECT DISTINCT p FROM Photo p LEFT JOIN FETCH p.book LEFT JOIN FETCH p.author")
    List<Photo> findAllWithBookAndAuthor();

    @Query("SELECT DISTINCT p FROM Photo p LEFT JOIN FETCH p.book LEFT JOIN FETCH p.author ORDER BY p.id")
    List<Photo> findAllWithBookAndAuthorOrderById();
}