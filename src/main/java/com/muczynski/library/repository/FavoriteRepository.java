/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser_Id(Long userId);

    List<Favorite> findByUser_IdAndBook_Id(Long userId, Long bookId);

    List<Favorite> findByUser_IdAndAuthor_Id(Long userId, Long authorId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndAuthor_Id(Long userId, Long authorId);

    /**
     * Native so book_id/author_id are read as columns (no join that would drop
     * author-only or book-only rows).
     */
    @Query(value = "SELECT list_name, book_id, author_id FROM favorites WHERE user_id = :userId", nativeQuery = true)
    List<Object[]> findMembershipRowsByUserId(@Param("userId") Long userId);

    @Query("SELECT f.listName FROM Favorite f WHERE f.user.id = :userId AND f.book.id = :bookId")
    List<String> findListNamesByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT f.listName FROM Favorite f WHERE f.user.id = :userId AND f.author.id = :authorId")
    List<String> findListNamesByUserIdAndAuthorId(@Param("userId") Long userId, @Param("authorId") Long authorId);

    @Query("SELECT DISTINCT f.listName FROM Favorite f WHERE f.user.id = :userId")
    List<String> findDistinctListNamesByUserId(Long userId);

    @Query("SELECT f.listName, COUNT(f) FROM Favorite f WHERE f.book IS NOT NULL GROUP BY f.listName")
    List<Object[]> countBooksGroupedByListName();

    @Query("SELECT f.listName, COUNT(f) FROM Favorite f WHERE f.author IS NOT NULL GROUP BY f.listName")
    List<Object[]> countAuthorsGroupedByListName();

    @Query("SELECT DISTINCT f FROM Favorite f JOIN FETCH f.user LEFT JOIN FETCH f.book LEFT JOIN FETCH f.book.author LEFT JOIN FETCH f.author")
    List<Favorite> findAllWithRefs();
}
