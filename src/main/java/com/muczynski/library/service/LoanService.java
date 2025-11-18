/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.mapper.LoanMapper;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    public LoanDto checkoutBook(LoanDto loanDto) {
        Book book = bookRepository.findById(loanDto.getBookId()).orElseThrow(() -> new LibraryException("Book not found: " + loanDto.getBookId()));
        User user = userRepository.findById(loanDto.getUserId()).orElseThrow(() -> new LibraryException("User not found: " + loanDto.getUserId()));
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setLoanDate(loanDto.getLoanDate() != null ? loanDto.getLoanDate() : LocalDate.now());
        loan.setDueDate(loanDto.getDueDate() != null ? loanDto.getDueDate() : loan.getLoanDate().plusWeeks(2));
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

    public List<LoanDto> getAllLoans(boolean showAll) {
        List<Loan> loans;
        if (showAll) {
            loans = loanRepository.findAllByOrderByDueDateAsc();
        } else {
            loans = loanRepository.findAllByReturnDateIsNullOrderByDueDateAsc();
        }
        return loans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<LoanDto> getLoansByUsername(String username, boolean showAll) {
        List<User> users = userRepository.findAllByUsernameOrderByIdAsc(username);
        if (users.isEmpty()) {
            throw new LibraryException("User not found: " + username);
        }
        User user = users.get(0);
        if (users.size() > 1) {
            logger.warn("Found {} duplicate users with username '{}'. Using user with lowest ID: {}.",
                       users.size(), username, user.getId());
        }
        List<Loan> loans;
        if (showAll) {
            loans = loanRepository.findAllByUserOrderByDueDateAsc(user);
        } else {
            loans = loanRepository.findAllByUserAndReturnDateIsNullOrderByDueDateAsc(user);
        }
        return loans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    public LoanDto getLoanById(Long id) {
        return loanRepository.findById(id)
                .map(loanMapper::toDto)
                .orElse(null);
    }

    public LoanDto updateLoan(Long id, LoanDto loanDto) {
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new LibraryException("Loan not found: " + id));
        if (loanDto.getLoanDate() != null) {
            loan.setLoanDate(loanDto.getLoanDate());
        }
        if (loanDto.getDueDate() != null) {
            loan.setDueDate(loanDto.getDueDate());
        }
        if (loanDto.getReturnDate() != null) {
            loan.setReturnDate(loanDto.getReturnDate());
        }
        if (loanDto.getBookId() != null) {
            loan.setBook(bookRepository.findById(loanDto.getBookId()).orElseThrow(() -> new LibraryException("Book not found: " + loanDto.getBookId())));
        }
        if (loanDto.getUserId() != null) {
            loan.setUser(userRepository.findById(loanDto.getUserId()).orElseThrow(() -> new LibraryException("User not found: " + loanDto.getUserId())));
        }
        Loan savedLoan = loanRepository.save(loan);
        return loanMapper.toDto(savedLoan);
    }

    public void deleteLoan(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new LibraryException("Loan not found: " + id);
        }
        loanRepository.deleteById(id);
    }
}
