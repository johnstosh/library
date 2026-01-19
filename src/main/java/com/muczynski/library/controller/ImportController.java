/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.DatabaseStatsDto;
import com.muczynski.library.dto.importdtos.ImportRequestDto;
import com.muczynski.library.dto.importdtos.ImportResponseDto;
import com.muczynski.library.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;

    @PostMapping("/json")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<ImportResponseDto> importJson(@RequestBody ImportRequestDto dto) {
        logger.info("Import request received");
        try {
            ImportResponseDto.ImportCounts counts = importService.importData(dto);
            logger.info("Import completed successfully");
            return ResponseEntity.ok(ImportResponseDto.success("Import completed successfully", counts));
        } catch (Exception e) {
            logger.error("Import failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ImportResponseDto.error("Import failed: " + e.getMessage()));
        }
    }

    @GetMapping("/json")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<ImportRequestDto> exportJson() {
        ImportRequestDto exportData = importService.exportData();
        return ResponseEntity.ok(exportData);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DatabaseStatsDto> getDatabaseStats() {
        DatabaseStatsDto stats = importService.getDatabaseStats();
        return ResponseEntity.ok(stats);
    }
}
