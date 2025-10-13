package com.muczynski.library.service;

import com.muczynski.library.domain.Loan;
import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.mapper.LoanMapper;
import com.muczynski.library.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
    }

    public LoanDto checkoutBook(LoanDto loanDto) {
        Loan loan = loanMapper.toEntity(loanDto);
        loan.setLoanDate(LocalDate.now());
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
        return loanRepository.findAll().stream()
                .map(loanMapper::toDto)
                .collect(Collectors.toList());
    }

    public LoanDto getLoanById(Long id) {
        return loanRepository.findById(id)
                .map(loanMapper::toDto)
                .orElse(null);
    }
}