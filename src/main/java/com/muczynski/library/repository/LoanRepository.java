// (c) Copyright 2025 by Muczynski
package com.muczynski.library.repository;

import com.muczynski.library.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Modifying
    @Query("DELETE FROM Loan l WHERE l.loanDate = ?1")
    void deleteByLoanDate(LocalDate loanDate);
    List<Loan> findAllByReturnDateIsNullOrderByDueDateAsc();
    List<Loan> findAllByOrderByDueDateAsc();
    long countByBookId(Long bookId);
    long countByBookIdAndReturnDateIsNull(Long bookId);
    long countByUserIdAndReturnDateIsNull(Long userId);
    void deleteByUserId(Long userId);
}
