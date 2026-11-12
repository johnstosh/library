/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.CatalogToolApplyRequestDto;
import com.muczynski.library.dto.CatalogToolApplyResultDto;
import com.muczynski.library.dto.CatalogToolCandidatesDto;
import com.muczynski.library.dto.CatalogToolChangeDto;
import com.muczynski.library.dto.CatalogToolPreviewDto;
import com.muczynski.library.dto.CatalogToolUndoRequestDto;
import com.muczynski.library.dto.CatalogToolUndoableDto;
import com.muczynski.library.service.CatalogToolMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Data Management catalog tools (Issue #386). Chicago and Grokipedia: Recalc
 * previews, Do It writes, Undo restores. Free text searches a capped oldest-first
 * prefix and stores each finished book before the next batch.
 */
@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class CatalogToolMaintenanceController {

    private final CatalogToolMaintenanceService catalogToolMaintenanceService;

    @PostMapping("/chicago-titles/preview")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolPreviewDto> previewChicago() {
        return preview("Chicago title case", catalogToolMaintenanceService::previewChicago);
    }

    @PostMapping("/chicago-titles/apply")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolApplyResultDto> applyChicago(@RequestBody(required = false) CatalogToolApplyRequestDto request) {
        return apply("Chicago title case", () -> catalogToolMaintenanceService.applyChicago(changes(request)));
    }

    @PostMapping("/chicago-titles/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolApplyResultDto> undoChicago(@RequestBody CatalogToolUndoRequestDto request) {
        return apply("Chicago title case undo", () -> catalogToolMaintenanceService.undoChicago(request.getActionId()));
    }

    @GetMapping("/chicago-titles/undoable")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolUndoableDto> undoableChicago() {
        return ResponseEntity.ok(catalogToolMaintenanceService.undoableChicago());
    }

    @GetMapping("/grokipedia/candidates")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolCandidatesDto> grokipediaCandidates() {
        return ResponseEntity.ok(catalogToolMaintenanceService.grokipediaCandidateIds());
    }

    @PostMapping("/grokipedia/preview")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolPreviewDto> previewGrokipedia(@RequestBody(required = false) CatalogToolCandidatesDto request) {
        return preview("Grokipedia", () -> catalogToolMaintenanceService.previewGrokipedia(
                request == null ? List.of() : request.getBookIds()));
    }

    @PostMapping("/grokipedia/apply")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolApplyResultDto> applyGrokipedia(@RequestBody(required = false) CatalogToolApplyRequestDto request) {
        return apply("Grokipedia", () -> catalogToolMaintenanceService.applyGrokipedia(changes(request)));
    }

    @PostMapping("/grokipedia/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolApplyResultDto> undoGrokipedia(@RequestBody CatalogToolUndoRequestDto request) {
        return apply("Grokipedia undo", () -> catalogToolMaintenanceService.undoGrokipedia(request.getActionId()));
    }

    @GetMapping("/grokipedia/undoable")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolUndoableDto> undoableGrokipedia() {
        return ResponseEntity.ok(catalogToolMaintenanceService.undoableGrokipedia());
    }

    @GetMapping("/grokipedia-authors/candidates")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolCandidatesDto> grokipediaAuthorCandidates() {
        return ResponseEntity.ok(catalogToolMaintenanceService.grokipediaAuthorCandidateIds());
    }

    @PostMapping("/grokipedia-authors/preview")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolPreviewDto> previewGrokipediaAuthors(
            @RequestBody(required = false) CatalogToolCandidatesDto request) {
        return preview("Author Grokipedia", () -> catalogToolMaintenanceService.previewGrokipediaAuthors(
                request == null || request.getAuthorIds() == null ? List.of() : request.getAuthorIds()));
    }

    @PostMapping("/grokipedia-authors/apply")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolApplyResultDto> applyGrokipediaAuthors(
            @RequestBody(required = false) CatalogToolApplyRequestDto request) {
        return apply("Author Grokipedia", () -> catalogToolMaintenanceService.applyGrokipediaAuthors(changes(request)));
    }

    @PostMapping("/grokipedia-authors/undo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolApplyResultDto> undoGrokipediaAuthors(
            @RequestBody CatalogToolUndoRequestDto request) {
        return apply("Author Grokipedia undo",
                () -> catalogToolMaintenanceService.undoGrokipediaAuthors(request.getActionId()));
    }

    @GetMapping("/grokipedia-authors/undoable")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolUndoableDto> undoableGrokipediaAuthors() {
        return ResponseEntity.ok(catalogToolMaintenanceService.undoableGrokipediaAuthors());
    }

    @GetMapping("/free-text/candidates")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolCandidatesDto> freeTextCandidates(@RequestParam int limit) {
        return ResponseEntity.ok(catalogToolMaintenanceService.freeTextCandidateIds(limit));
    }

    @PostMapping("/free-text/search")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<CatalogToolPreviewDto> searchFreeText(
            @RequestBody(required = false) CatalogToolCandidatesDto request) {
        try {
            return ResponseEntity.ok(catalogToolMaintenanceService.searchFreeText(
                    request == null ? List.of() : request.getBookIds()));
        } catch (Exception e) {
            log.error("Free text search failed", e);
            return ResponseEntity.ok(CatalogToolPreviewDto.builder()
                    .error("Search failed: " + e.getMessage())
                    .message("Internal error during search. Check logs for details.")
                    .changes(List.of())
                    .skipped(List.of())
                    .build());
        }
    }

    private ResponseEntity<CatalogToolPreviewDto> preview(String tool, PreviewCall call) {
        try {
            return ResponseEntity.ok(call.run());
        } catch (Exception e) {
            log.error("{} recalc failed", tool, e);
            return ResponseEntity.ok(CatalogToolPreviewDto.builder()
                    .error("Recalc failed: " + e.getMessage())
                    .message("Internal error during recalc. Check logs for details.")
                    .changes(List.of())
                    .skipped(List.of())
                    .build());
        }
    }

    private ResponseEntity<CatalogToolApplyResultDto> apply(String tool, ApplyCall call) {
        try {
            return ResponseEntity.ok(call.run());
        } catch (Exception e) {
            log.error("{} failed", tool, e);
            return ResponseEntity.ok(CatalogToolApplyResultDto.builder()
                    .error("Failed: " + e.getMessage())
                    .message("Internal error. Check logs for details.")
                    .changes(List.of())
                    .skippedChanges(List.of())
                    .build());
        }
    }

    private static List<CatalogToolChangeDto> changes(CatalogToolApplyRequestDto request) {
        if (request == null || request.getChanges() == null) {
            return List.of();
        }
        return request.getChanges();
    }

    @FunctionalInterface
    private interface PreviewCall {
        CatalogToolPreviewDto run();
    }

    @FunctionalInterface
    private interface ApplyCall {
        CatalogToolApplyResultDto run();
    }
}
