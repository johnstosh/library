/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.LoanDto;
import org.springframework.stereotype.Service;

@Service
public class LoanMapper {

    public LoanDto toDto(Loan loan) {
        if (loan == null) {
            return null;
        }

        LoanDto loanDto = new LoanDto();
        loanDto.setId(loan.getId());
        loanDto.setLoanDate(loan.getLoanDate());
        loanDto.setDueDate(loan.getDueDate());
        loanDto.setReturnDate(loan.getReturnDate());
        loanDto.setLastModified(loan.getLastModified());
        if (loan.getBook() != null) {
            loanDto.setBookId(loan.getBook().getId());
            loanDto.setBookTitle(loan.getBook().getTitle());
        }
        if (loan.getUser() != null) {
            loanDto.setUserId(loan.getUser().getId());
            loanDto.setUserName(loan.getUser().getUsername());
        }

        return loanDto;
    }

    public Loan toEntity(LoanDto loanDto) {
        if (loanDto == null) {
            return null;
        }

        Loan loan = new Loan();
        loan.setId(loanDto.getId());
        loan.setLoanDate(loanDto.getLoanDate());
        loan.setDueDate(loanDto.getDueDate());
        loan.setReturnDate(loanDto.getReturnDate());

        if (loanDto.getBookId() != null) {
            Book book = new Book();
            book.setId(loanDto.getBookId());
            loan.setBook(book);
        }

        if (loanDto.getUserId() != null) {
            User user = new User();
            user.setId(loanDto.getUserId());
            loan.setUser(user);
        }

        return loan;
    }
}
