package com.muczynski.library.controller;

import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LoanDto> checkoutBook(@RequestBody LoanDto loanDto) {
        LoanDto createdLoan = loanService.checkoutBook(loanDto);
        return new ResponseEntity<>(createdLoan, HttpStatus.CREATED);
    }

    @PutMapping("/return/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LoanDto> returnBook(@PathVariable Long id) {
        LoanDto returnedLoan = loanService.returnBook(id);
        return returnedLoan != null ? new ResponseEntity<>(returnedLoan, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<LoanDto>> getAllLoans() {
        List<LoanDto> loans = loanService.getAllLoans();
        return new ResponseEntity<>(loans, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long id) {
        LoanDto loan = loanService.getLoanById(id);
        return loan != null ? new ResponseEntity<>(loan, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}