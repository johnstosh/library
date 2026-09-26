/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.dto.IllegalGenresMaintenanceDto;
import com.muczynski.library.service.MaintenanceService;
import com.muczynski.library.service.duplicate.DuplicateTitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Maintenance API for librarian Data Management page (Issues #347, #351).
 * Provides illegal-genre cleanup and duplicate-title find (read-only).
 */
@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final DuplicateTitleService duplicateTitleService;

    /**
     * Recalculates count of books affected by illegal or mismatched genre tags.
     * Used to populate the Count column (shows "-" until clicked).
     */
    @GetMapping("/illegal-genres/count")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<IllegalGenresMaintenanceDto> countIllegalGenres() {
        log.info("Recalculating illegal genres count");
        IllegalGenresMaintenanceDto result = maintenanceService.countIllegalGenres();
        return ResponseEntity.ok(result);
    }

    /**
     * Performs cleanup of illegal genres: normalizes plurals/variants and removes
     * any remaining non-canonical tags. Updates books in place.
     * Returns detailed summary for the Results column.
     */
    @PostMapping("/illegal-genres/cleanup")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<IllegalGenresMaintenanceDto> cleanupIllegalGenres() {
        log.info("Starting illegal genres cleanup");
        try {
            IllegalGenresMaintenanceDto result = maintenanceService.cleanupIllegalGenres();
            log.info("Cleanup completed successfully: {}", result.getMessage());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Illegal genres cleanup failed", e);
            return ResponseEntity.ok(IllegalGenresMaintenanceDto.builder()
                    .error("Cleanup failed: " + e.getMessage())
                    .message("Internal error during cleanup. Check logs for details.")
                    .build());
        }
    }

    /**
     * Find near-duplicate catalog titles (read-only). Uses titles + alternate
     * titles, keyword indexing, and Jaro–Winkler; returns top 100 pairs.
     */
    @PostMapping("/duplicate-titles/find")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitlesResultDto> findDuplicateTitles() {
        log.info("Starting duplicate title scan");
        try {
            DuplicateTitlesResultDto result = duplicateTitleService.findDuplicates();
            log.info("Duplicate title scan finished: {}", result.getMessage());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Duplicate title scan failed", e);
            return ResponseEntity.ok(DuplicateTitlesResultDto.builder()
                    .error("Find failed: " + e.getMessage())
                    .message("Internal error during duplicate title scan. Check logs for details.")
                    .build());
        }
    }
}
