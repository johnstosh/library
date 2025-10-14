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
        loanDto.setReturnDate(loan.getReturnDate());
        if (loan.getBook() != null) {
            loanDto.setBookId(loan.getBook().getId());
        }
        if (loan.getUser() != null) {
            loanDto.setUserId(loan.getUser().getId());
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
