/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.BookLocStatusDto;
import com.muczynski.library.dto.LocLookupResultDto;
import com.muczynski.library.service.LocBulkLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for LOC bulk lookup maintenance operations
 */
@RestController
@RequestMapping("/api/loc-bulk-lookup")
@RequiredArgsConstructor
@Slf4j
public class LocBulkLookupController {

    private final LocBulkLookupService locBulkLookupService;

    /**
     * Get all books with their LOC status
     */
    @GetMapping("/books")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<BookLocStatusDto>> getAllBooksWithLocStatus() {
        log.info("Getting all books with LOC status");
        List<BookLocStatusDto> books = locBulkLookupService.getAllBooksWithLocStatus();
        return ResponseEntity.ok(books);
    }

    /**
     * Lookup LOC number for a single book
     */
    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LocLookupResultDto> lookupSingleBook(@PathVariable Long bookId) {
        log.info("Looking up LOC number for book ID: {}", bookId);
        LocLookupResultDto result = locBulkLookupService.lookupAndUpdateBook(bookId);
        return ResponseEntity.ok(result);
    }

    /**
     * Lookup LOC numbers for multiple books
     */
    @PostMapping("/lookup-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<LocLookupResultDto>> lookupBulkBooks(@RequestBody List<Long> bookIds) {
        log.info("Looking up LOC numbers for {} books", bookIds.size());
        List<LocLookupResultDto> results = locBulkLookupService.lookupAndUpdateBooks(bookIds);
        return ResponseEntity.ok(results);
    }

    /**
     * Lookup LOC numbers for all books missing LOC numbers
     */
    @PostMapping("/lookup-all-missing")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<LocLookupResultDto>> lookupAllMissing() {
        log.info("Looking up LOC numbers for all books with missing LOC numbers");
        List<LocLookupResultDto> results = locBulkLookupService.lookupAllMissingLoc();
        return ResponseEntity.ok(results);
    }
}
