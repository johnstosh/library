/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser_Id(Long userId);

    List<Favorite> findByUser_IdAndBook_Id(Long userId, Long bookId);

    List<Favorite> findByUser_IdAndAuthor_Id(Long userId, Long authorId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndAuthor_Id(Long userId, Long authorId);

    @Query("SELECT DISTINCT f.listName FROM Favorite f WHERE f.user.id = :userId")
    List<String> findDistinctListNamesByUserId(Long userId);

    @Query("SELECT f.listName, COUNT(f) FROM Favorite f WHERE f.book IS NOT NULL GROUP BY f.listName")
    List<Object[]> countBooksGroupedByListName();

    @Query("SELECT f.listName, COUNT(f) FROM Favorite f WHERE f.author IS NOT NULL GROUP BY f.listName")
    List<Object[]> countAuthorsGroupedByListName();

    @Query("SELECT DISTINCT f FROM Favorite f JOIN FETCH f.user LEFT JOIN FETCH f.book LEFT JOIN FETCH f.book.author LEFT JOIN FETCH f.author")
    List<Favorite> findAllWithRefs();
}
