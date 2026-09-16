/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.AclaLookupResultDto;
import com.muczynski.library.service.AclaLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for ACLA (Allegheny County Library Association) availability lookup operations
 */
@RestController
@RequestMapping("/api/acla-lookup")
@RequiredArgsConstructor
@Slf4j
public class AclaLookupController {

    private final AclaLookupService aclaLookupService;

    /**
     * Lookup ACLA availability for a single book.
     * Public so patrons can refresh holdings from the book page.
     */
    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AclaLookupResultDto> lookupSingleBook(@PathVariable Long bookId) {
        log.info("Looking up ACLA availability for book ID: {}", bookId);
        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(bookId);
        return ResponseEntity.ok(result);
    }
}
