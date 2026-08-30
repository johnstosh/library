/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.YdlLookupResultDto;
import com.muczynski.library.service.YdlLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for YDL (Ypsilanti District Library) availability lookup operations
 */
@RestController
@RequestMapping("/api/ydl-lookup")
@RequiredArgsConstructor
@Slf4j
public class YdlLookupController {

    private final YdlLookupService ydlLookupService;

    /**
     * Lookup YDL availability for a single book.
     * Public so patrons can refresh holdings from the book page.
     */
    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<YdlLookupResultDto> lookupSingleBook(@PathVariable Long bookId) {
        log.info("Looking up YDL availability for book ID: {}", bookId);
        YdlLookupResultDto result = ydlLookupService.lookupAndUpdateBook(bookId);
        return ResponseEntity.ok(result);
    }
}
