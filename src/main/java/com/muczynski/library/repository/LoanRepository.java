/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
           "LEFT JOIN FETCH u.roles")
    List<Loan> findAllWithBookAndUser();
    void deleteByLoanDate(LocalDate loanDate);
    List<Loan> findAllByReturnDateIsNullOrderByDueDateAsc();
    List<Loan> findAllByOrderByDueDateAsc();
    List<Loan> findAllByUserOrderByDueDateAsc(User user);
    List<Loan> findAllByUserAndReturnDateIsNullOrderByDueDateAsc(User user);
    long countByBookId(Long bookId);
    long countByBookIdAndReturnDateIsNull(Long bookId);
    long countByUserIdAndReturnDateIsNull(Long userId);
    void deleteByUserId(Long userId);
    Optional<Loan> findByBookIdAndUserIdAndLoanDate(Long bookId, Long userId, LocalDate loanDate);
    long countByReturnDateIsNull();
}
