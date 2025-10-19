package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.mapper.LoanMapper;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    public LoanDto checkoutBook(LoanDto loanDto) {
        Book book = bookRepository.findById(loanDto.getBookId()).orElseThrow(() -> new RuntimeException("Book not found: " + loanDto.getBookId()));
        User user = userRepository.findById(loanDto.getUserId()).orElseThrow(() -> new RuntimeException("User not found: " + loanDto.getUserId()));
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setLoanDate(loanDto.getLoanDate() != null ? loanDto.getLoanDate() : LocalDate.now());
        loan.setReturnDate(loanDto.getReturnDate());
        Loan savedLoan = loanRepository.save(loan);
        return loanMapper.toDto(savedLoan);
    }

    public LoanDto returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan != null) {
            loan.setReturnDate(LocalDate.now());
            Loan savedLoan = loanRepository.save(loan);
            return loanMapper.toDto(savedLoan);
        }
        return null;
    }

    public List<LoanDto> getAllLoans() {
        return loanRepository.findAllByOrderByReturnDateAsc().stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    public LoanDto getLoanById(Long id) {
        return loanRepository.findById(id)
                .map(loanMapper::toDto)
                .orElse(null);
    }

    public LoanDto updateLoan(Long id, LoanDto loanDto) {
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new RuntimeException("Loan not found: " + id));
        if (loanDto.getLoanDate() != null) {
            loan.setLoanDate(loanDto.getLoanDate());
        }
        if (loanDto.getReturnDate() != null) {
            loan.setReturnDate(loanDto.getReturnDate());
        }
        if (loanDto.getBookId() != null) {
            loan.setBook(bookRepository.findById(loanDto.getBookId()).orElseThrow(() -> new RuntimeException("Book not found: " + loanDto.getBookId())));
        }
        if (loanDto.getUserId() != null) {
            loan.setUser(userRepository.findById(loanDto.getUserId()).orElseThrow(() -> new RuntimeException("User not found: " + loanDto.getUserId())));
        }
        Loan savedLoan = loanRepository.save(loan);
        return loanMapper.toDto(savedLoan);
    }

    public void deleteLoan(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new RuntimeException("Loan not found: " + id);
        }
        loanRepository.deleteById(id);
    }
}
