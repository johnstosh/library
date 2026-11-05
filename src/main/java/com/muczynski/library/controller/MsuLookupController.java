/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.MsuLookupResultDto;
import com.muczynski.library.service.MsuLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Michigan State University availability lookup operations.
 */
@RestController
@RequestMapping("/api/msu-lookup")
@RequiredArgsConstructor
@Slf4j
public class MsuLookupController {

    private final MsuLookupService msuLookupService;

    /**
     * Lookup Michigan State University availability for a single book.
     * Public so patrons can refresh holdings from the book page.
     */
    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MsuLookupResultDto> lookupSingleBook(@PathVariable Long bookId) {
        log.info("Looking up MSU availability for book ID: {}", bookId);
        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(bookId);
        return ResponseEntity.ok(result);
    }
}
