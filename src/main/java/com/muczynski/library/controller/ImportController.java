/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.DatabaseStatsDto;
import com.muczynski.library.dto.importdtos.ImportRequestDto;
import com.muczynski.library.service.ImportService;
import lombok.RequiredArgsConstructor;
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

    private final ImportService importService;

    @PostMapping("/json")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<String> importJson(@RequestBody ImportRequestDto dto) {
        importService.importData(dto);
        return ResponseEntity.ok("Import completed successfully");
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
