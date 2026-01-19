/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.BookAlreadyLoanedException;
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

        // Check if book is already on loan (has an active loan with no return date)
        long activeLoans = loanRepository.countByBookIdAndReturnDateIsNull(book.getId());
        if (activeLoans > 0) {
            throw new BookAlreadyLoanedException(book.getId());
        }

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
        logger.info("LoanService.getAllLoans called with showAll={}", showAll);
        List<Loan> loans;
        if (showAll) {
            loans = loanRepository.findAllByOrderByDueDateAsc();
            logger.info("LoanService.getAllLoans: Found {} total loans (showAll=true)", loans.size());
        } else {
            loans = loanRepository.findAllByReturnDateIsNullOrderByDueDateAsc();
            logger.info("LoanService.getAllLoans: Found {} active loans (showAll=false)", loans.size());
        }
        if (!loans.isEmpty()) {
            logger.debug("LoanService.getAllLoans: First loan - id={}, bookId={}, userId={}, returnDate={}",
                loans.get(0).getId(),
                loans.get(0).getBook() != null ? loans.get(0).getBook().getId() : null,
                loans.get(0).getUser() != null ? loans.get(0).getUser().getId() : null,
                loans.get(0).getReturnDate());
        }
        List<LoanDto> result = loans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
        logger.info("LoanService.getAllLoans: Returning {} loan DTOs", result.size());
        return result;
    }

    public List<LoanDto> getLoansByUserId(Long userId, boolean showAll) {
        logger.info("LoanService.getLoansByUserId called with userId={}, showAll={}", userId, showAll);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found: " + userId));
        logger.info("LoanService.getLoansByUserId: Found user with id={}, username={}", user.getId(), user.getUsername());

        // Debug: Compare count method (which works) vs fetch method
        long countByUserId = loanRepository.countByUserIdAndReturnDateIsNull(userId);
        logger.info("LoanService.getLoansByUserId: countByUserIdAndReturnDateIsNull({})={}", userId, countByUserId);

        List<Loan> loans;
        if (showAll) {
            // Use the new userId-based query method
            loans = loanRepository.findAllByUserIdOrderByDueDateAsc(userId);
            logger.info("LoanService.getLoansByUserId: findAllByUserIdOrderByDueDateAsc returned {} loans", loans.size());

            // Debug comparison with User entity method
            List<Loan> loansViaUserEntity = loanRepository.findAllByUserOrderByDueDateAsc(user);
            logger.info("LoanService.getLoansByUserId: [DEBUG] findAllByUserOrderByDueDateAsc (User entity) returned {} loans", loansViaUserEntity.size());
        } else {
            // Use the new userId-based query method
            loans = loanRepository.findAllByUserIdAndReturnDateIsNullOrderByDueDateAsc(userId);
            logger.info("LoanService.getLoansByUserId: findAllByUserIdAndReturnDateIsNullOrderByDueDateAsc returned {} loans", loans.size());

            // Debug comparison with User entity method
            List<Loan> loansViaUserEntity = loanRepository.findAllByUserAndReturnDateIsNullOrderByDueDateAsc(user);
            logger.info("LoanService.getLoansByUserId: [DEBUG] findAllByUserAndReturnDateIsNullOrderByDueDateAsc (User entity) returned {} loans", loansViaUserEntity.size());
        }

        if (!loans.isEmpty()) {
            Loan firstLoan = loans.get(0);
            logger.info("LoanService.getLoansByUserId: First loan - id={}, bookId={}, userId={}, loanDate={}, returnDate={}",
                firstLoan.getId(),
                firstLoan.getBook() != null ? firstLoan.getBook().getId() : null,
                firstLoan.getUser() != null ? firstLoan.getUser().getId() : null,
                firstLoan.getLoanDate(),
                firstLoan.getReturnDate());
        }

        List<LoanDto> result = loans.stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
        logger.info("LoanService.getLoansByUserId: Returning {} loan DTOs", result.size());
        return result;
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
