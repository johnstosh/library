/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.WccLookupResultDto;
import com.muczynski.library.service.WccLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Washtenaw Community College availability lookup operations.
 */
@RestController
@RequestMapping("/api/wcc-lookup")
@RequiredArgsConstructor
@Slf4j
public class WccLookupController {

    private final WccLookupService wccLookupService;

    /**
     * Lookup Washtenaw Community College availability for a single book.
     * Public so patrons can refresh holdings from the book page.
     */
    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<WccLookupResultDto> lookupSingleBook(@PathVariable Long bookId) {
        log.info("Looking up WCC availability for book ID: {}", bookId);
        WccLookupResultDto result = wccLookupService.lookupAndUpdateBook(bookId);
        return ResponseEntity.ok(result);
    }
}
