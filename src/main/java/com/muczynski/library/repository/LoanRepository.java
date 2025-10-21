package com.muczynski.library.repository;

import com.muczynski.library.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    void deleteByLoanDate(LocalDate loanDate);
    List<Loan> findAllByOrderByDueDateAsc();
    long countByBookId(Long bookId);
}