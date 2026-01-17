/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.exception.InsufficientPermissionsException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.service.LoanService;
import com.muczynski.library.service.UserService;
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

    @Autowired
    private UserService userService;

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkoutBook(@Valid @RequestBody LoanDto loanDto, Authentication authentication) {
        // Regular users can only checkout books to themselves
        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(auth -> "LIBRARIAN".equals(auth.getAuthority()));
        if (!isLibrarian) {
            // For non-librarians, verify they're checking out to themselves
            // The principal name is the database user ID (not username)
            Long authenticatedUserId = Long.parseLong(authentication.getName());
            logger.debug("Regular user ID {} attempting to checkout book", authenticatedUserId);
            if (!authenticatedUserId.equals(loanDto.getUserId())) {
                logger.warn("User ID {} attempted to checkout book to different user ID {}", authenticatedUserId, loanDto.getUserId());
                throw new InsufficientPermissionsException("You can only checkout books to yourself");
            }
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
        logger.info("LoanController.getAllLoans: showAll={}, principal={}, principalClass={}",
            showAll, authentication.getName(), authentication.getPrincipal().getClass().getSimpleName());
        logger.info("LoanController.getAllLoans: authorities={}", authentication.getAuthorities());
        try {
            // Librarians see all loans, regular users see only their own loans
            boolean isLibrarian = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "LIBRARIAN".equals(auth.getAuthority()));
            logger.info("LoanController.getAllLoans: isLibrarian={}", isLibrarian);
            List<LoanDto> loans;
            if (isLibrarian) {
                loans = loanService.getAllLoans(showAll);
                logger.info("LoanController.getAllLoans: Librarian retrieved {} loans", loans.size());
            } else {
                // The principal name is the database user ID (not username) for OAuth users
                // For form login users, we need to look up the user by username
                String principalName = authentication.getName();
                Long userId;
                try {
                    userId = Long.parseLong(principalName);
                    logger.info("LoanController.getAllLoans: Parsed principal as user ID: {}", userId);
                } catch (NumberFormatException e) {
                    // Principal is a username (form login), look up user ID
                    logger.info("LoanController.getAllLoans: Principal '{}' is not a number, treating as username", principalName);
                    userId = userService.getUserIdByUsernameOrSsoSubject(principalName);
                    logger.info("LoanController.getAllLoans: Looked up user ID {} for username '{}'", userId, principalName);
                    if (userId == null) {
                        throw new LibraryException("User not found: " + principalName);
                    }
                }
                loans = loanService.getLoansByUserId(userId, showAll);
                logger.info("LoanController.getAllLoans: User {} retrieved {} loans", userId, loans.size());
            }
            logger.info("LoanController.getAllLoans: Returning {} loans to client", loans.size());
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
