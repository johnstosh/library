/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.EmuLookupResultDto;
import com.muczynski.library.service.EmuLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for EMU Halle Library availability lookup operations
 */
@RestController
@RequestMapping("/api/emu-lookup")
@RequiredArgsConstructor
@Slf4j
public class EmuLookupController {

    private final EmuLookupService emuLookupService;

    /**
     * Lookup EMU Halle Library availability for a single book
     */
    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<EmuLookupResultDto> lookupSingleBook(@PathVariable Long bookId) {
        log.info("Looking up EMU availability for book ID: {}", bookId);
        EmuLookupResultDto result = emuLookupService.lookupAndUpdateBook(bookId);
        return ResponseEntity.ok(result);
    }
}
