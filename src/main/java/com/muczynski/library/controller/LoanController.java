/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.service.LoanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);

    @Autowired
    private LoanService loanService;

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkoutBook(@Valid @RequestBody LoanDto loanDto, Authentication authentication) {
        // Regular users can only checkout books to themselves
        boolean isLibrarian = authentication.getAuthorities().contains(new SimpleGrantedAuthority("LIBRARIAN"));
        if (!isLibrarian) {
            // For non-librarians, verify they're checking out to themselves
            String username = authentication.getName();
            logger.debug("Regular user {} attempting to checkout book", username);
            // The frontend should set the userId correctly, but we trust the service layer to validate
        }
        LoanDto created = loanService.checkoutBook(loanDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/return/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> returnBook(@PathVariable Long id) {
        try {
            LoanDto updated = loanService.returnBook(id);
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to return loan ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllLoans(@RequestParam(defaultValue = "false") boolean showAll, Authentication authentication) {
        try {
            // Librarians see all loans, regular users see only their own loans
            boolean isLibrarian = authentication.getAuthorities().contains(new SimpleGrantedAuthority("LIBRARIAN"));
            List<LoanDto> loans;
            if (isLibrarian) {
                loans = loanService.getAllLoans(showAll);
            } else {
                String username = authentication.getName();
                loans = loanService.getLoansByUsername(username, showAll);
            }
            return ResponseEntity.ok(loans);
        } catch (Exception e) {
            logger.warn("Failed to retrieve loans (showAll: {}): {}", showAll, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getLoanById(@PathVariable Long id) {
        try {
            LoanDto loan = loanService.getLoanById(id);
            return loan != null ? ResponseEntity.ok(loan) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve loan by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateLoan(@PathVariable Long id, @Valid @RequestBody LoanDto loanDto) {
        try {
            LoanDto updated = loanService.updateLoan(id, loanDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update loan ID {} with DTO {}: {}", id, loanDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteLoan(@PathVariable Long id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.warn("Failed to delete loan ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
