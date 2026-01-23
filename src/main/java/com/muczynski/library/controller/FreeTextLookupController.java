/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.FreeTextBulkLookupResultDto;
import com.muczynski.library.freetext.FreeTextLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for free text lookup operations.
 * Searches for free online text versions of books across multiple providers.
 */
@RestController
@RequestMapping("/api/free-text")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('LIBRARIAN')")
public class FreeTextLookupController {

    private final FreeTextLookupService freeTextLookupService;

    /**
     * Look up free online text for a single book.
     *
     * @param bookId the book ID to look up
     * @return lookup result with URL if found
     */
    @PostMapping("/lookup/{bookId}")
    public ResponseEntity<FreeTextBulkLookupResultDto> lookupBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(freeTextLookupService.lookupBook(bookId));
    }

    /**
     * Look up free online text for multiple books (bulk operation).
     *
     * @param bookIds list of book IDs to look up
     * @return list of lookup results
     */
    @PostMapping("/lookup-bulk")
    public ResponseEntity<List<FreeTextBulkLookupResultDto>> lookupBooks(@RequestBody List<Long> bookIds) {
        return ResponseEntity.ok(freeTextLookupService.lookupBooks(bookIds));
    }

    /**
     * Get the list of available free text providers.
     *
     * @return list of provider names in priority order
     */
    @GetMapping("/providers")
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok(freeTextLookupService.getProviderNames());
    }
}
