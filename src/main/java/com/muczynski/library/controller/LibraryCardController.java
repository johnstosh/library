/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.User;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.LibraryCardPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller for library card operations.
 * Allows users to print their library card as a wallet-sized PDF.
 */
@RestController
@RequestMapping("/api/library-card")
public class LibraryCardController {

    @Autowired
    private LibraryCardPdfService libraryCardPdfService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Generate and download a wallet-sized library card PDF for the current user.
     *
     * @return PDF file as byte array with appropriate headers
     */
    @GetMapping("/print")
    public ResponseEntity<byte[]> printLibraryCard() {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new LibraryException("User not authenticated");
            }

            String username = authentication.getName();
            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new LibraryException("User not found"));

            // Generate PDF
            byte[] pdfBytes = libraryCardPdfService.generateLibraryCardPdf(user);

            // Set appropriate headers for PDF download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "library-card.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            throw new LibraryException("Failed to generate library card PDF: " + e.getMessage());
        }
    }
}
