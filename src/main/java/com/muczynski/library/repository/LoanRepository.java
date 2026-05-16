/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities")
    List<Loan> findAllWithBookAndUser();

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities " +
           "WHERE l.returnDate IS NULL " +
           "ORDER BY l.dueDate ASC")
    List<Loan> findAllByReturnDateIsNullOrderByDueDateAsc();

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities " +
           "ORDER BY l.dueDate ASC")
    List<Loan> findAllByOrderByDueDateAsc();

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities " +
           "WHERE l.user = :user " +
           "ORDER BY l.dueDate ASC")
    List<Loan> findAllByUserOrderByDueDateAsc(User user);

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities " +
           "WHERE l.user = :user AND l.returnDate IS NULL " +
           "ORDER BY l.dueDate ASC")
    List<Loan> findAllByUserAndReturnDateIsNullOrderByDueDateAsc(User user);

    // Lightweight projection for photo ZIP import — loads only id, book title, and username.
    // INNER JOIN excludes loans without a book or user (same effect as the null guards previously in Java).
    @Query("SELECT l.id AS id, b.title AS bookTitle, u.username AS username " +
           "FROM Loan l JOIN l.book b JOIN l.user u")
    List<LoanZipImportProjection> findAllForZipImport();

    void deleteByLoanDate(LocalDate loanDate);
    long countByBookId(Long bookId);
    long countByBookIdAndReturnDateIsNull(Long bookId);

    // Batch: get open loan counts for multiple books in one query — avoids N+1 in getBooksByIds
    @Query("SELECT l.book.id, COUNT(l) FROM Loan l WHERE l.book.id IN :bookIds AND l.returnDate IS NULL GROUP BY l.book.id")
    List<Object[]> countOpenLoansByBookIds(@Param("bookIds") List<Long> bookIds);
    long countByUserIdAndReturnDateIsNull(Long userId);
    void deleteByUserId(Long userId);
    /** @deprecated Use findAllByBookIdAndUserIdAndLoanDateOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Loan> findByBookIdAndUserIdAndLoanDate(Long bookId, Long userId, LocalDate loanDate);
    List<Loan> findAllByBookIdAndUserIdAndLoanDateOrderByIdAsc(Long bookId, Long userId, LocalDate loanDate);
    long countByReturnDateIsNull();
    long countByBookLibraryIdAndReturnDateIsNull(Long libraryId);

    // Alternative methods using userId directly (for debugging comparison with count methods)
    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities " +
           "WHERE l.user.id = :userId " +
           "ORDER BY l.dueDate ASC")
    List<Loan> findAllByUserIdOrderByDueDateAsc(Long userId);

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.book b " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.library " +
           "LEFT JOIN FETCH l.user u " +
           "LEFT JOIN FETCH u.authorities " +
           "WHERE l.user.id = :userId AND l.returnDate IS NULL " +
           "ORDER BY l.dueDate ASC")
    List<Loan> findAllByUserIdAndReturnDateIsNullOrderByDueDateAsc(Long userId);
}
