/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.DuplicateTitleActionResultDto;
import com.muczynski.library.dto.DuplicateTitleDismissalRequestDto;
import com.muczynski.library.dto.DuplicateTitlePairActionRequestDto;
import com.muczynski.library.dto.DuplicateTitleUndoRequestDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.dto.IllegalGenresMaintenanceDto;
import com.muczynski.library.service.MaintenanceService;
import com.muczynski.library.service.duplicate.DuplicateTitleActionService;
import com.muczynski.library.service.duplicate.DuplicateTitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Maintenance API for librarian Data Management page (Issues #347, #351).
 * Provides illegal-genre cleanup, duplicate-title find, not-duplicate marks,
 * and the Duplicate Titles merge and fix actions. Those actions are not a
 * general author or book merge.
 */
@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final DuplicateTitleService duplicateTitleService;
    private final DuplicateTitleActionService duplicateTitleActionService;

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
     * WITHDRAWN books are excluded from the scan.
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

    /**
     * Remember that the two works on a result row are not duplicates.
     * Stored by folded title and author, so every extra copy stays hidden.
     * The current result table is left unchanged until the next find.
     */
    @PostMapping("/duplicate-titles/dismissals")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> dismissDuplicateTitle(@RequestBody DuplicateTitleDismissalRequestDto request) {
        duplicateTitleService.dismiss(
                request.getTitleA(), request.getAuthorA(), request.getTitleB(), request.getAuthorB());
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a not-duplicate mark. Used by Undo while the row is still on screen.
     */
    @PostMapping("/duplicate-titles/dismissals/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> undoDuplicateTitleDismissal(
            @RequestBody DuplicateTitleDismissalRequestDto request) {
        duplicateTitleService.restore(
                request.getTitleA(), request.getAuthorA(), request.getTitleB(), request.getAuthorB());
        return ResponseEntity.noContent().build();
    }

    /**
     * Merge the two authors on one Duplicate Titles row.
     * Not a general author merge. The change is recorded in duplicate_title_action.
     */
    @PostMapping("/duplicate-titles/author-merges")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitleActionResultDto> mergeDuplicateTitleAuthors(
            @RequestBody DuplicateTitlePairActionRequestDto request) {
        return ResponseEntity.ok(duplicateTitleActionService.mergeAuthors(
                request.getBookAId(), request.getBookBId()));
    }

    /**
     * Reverse a Duplicate Titles author merge. Inserts an undo row and leaves the
     * original row in place.
     */
    @PostMapping("/duplicate-titles/author-merges/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitleActionResultDto> undoDuplicateTitleAuthorMerge(
            @RequestBody DuplicateTitleUndoRequestDto request) {
        return ResponseEntity.ok(duplicateTitleActionService.undoAuthorMerge(request.getActionId()));
    }

    /**
     * Merge the two books on one Duplicate Titles row by combining free-text URLs.
     * Refuses when both books have call numbers. Not a general book merge.
     */
    @PostMapping("/duplicate-titles/title-merges")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitleActionResultDto> mergeDuplicateTitles(
            @RequestBody DuplicateTitlePairActionRequestDto request) {
        return ResponseEntity.ok(duplicateTitleActionService.mergeTitles(
                request.getBookAId(), request.getBookBId()));
    }

    /**
     * Reverse a Duplicate Titles title merge. Inserts an undo row and leaves the
     * original row in place.
     */
    @PostMapping("/duplicate-titles/title-merges/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitleActionResultDto> undoDuplicateTitleMerge(
            @RequestBody DuplicateTitleUndoRequestDto request) {
        return ResponseEntity.ok(duplicateTitleActionService.undoTitleMerge(request.getActionId()));
    }

    /**
     * Add the next ", c. N" copy suffix on a Duplicate Titles row whose books both
     * have call numbers. Uses the save-title copy-number rule, so existing
     * ", c. 2", ", c. 3", and ", c. 4" copies make the next title ", c. 5".
     * Does not delete either book.
     */
    @PostMapping("/duplicate-titles/title-fixes")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitleActionResultDto> fixDuplicateTitles(
            @RequestBody DuplicateTitlePairActionRequestDto request) {
        return ResponseEntity.ok(duplicateTitleActionService.fixTitles(
                request.getBookAId(), request.getBookBId()));
    }

    /**
     * Reverse a Duplicate Titles fix. Inserts an undo row and leaves the original
     * row in place.
     */
    @PostMapping("/duplicate-titles/title-fixes/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DuplicateTitleActionResultDto> undoDuplicateTitleFix(
            @RequestBody DuplicateTitleUndoRequestDto request) {
        return ResponseEntity.ok(duplicateTitleActionService.undoFixTitles(request.getActionId()));
    }
}
