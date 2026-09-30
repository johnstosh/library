/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.User;

import java.time.LocalDate;

/**
 * JSON captured before and after a loan change. Enough to describe the loan
 * and to notice when a later edit puts it back the way it was.
 */
public record LoanMailSnapshot(
        Long id,
        String bookTitle,
        String borrowerName,
        String borrowerEmail,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate) {

    public static LoanMailSnapshot from(Loan loan) {
        if (loan == null) {
            return null;
        }
        Book book = loan.getBook();
        User user = loan.getUser();
        return new LoanMailSnapshot(
                loan.getId(),
                book != null ? book.getTitle() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getEmail() : null,
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate());
    }
}
