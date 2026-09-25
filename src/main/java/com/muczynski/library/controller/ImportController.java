/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.BookAvailabilityStatsDto;
import com.muczynski.library.dto.DatabaseStatsDto;
import com.muczynski.library.dto.FavoriteListCountDto;
import com.muczynski.library.dto.LabelCountDto;
import com.muczynski.library.dto.importdtos.ImportResponseDto;
import com.muczynski.library.service.FavoriteService;
import com.muczynski.library.service.ImportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;
    private final FavoriteService favoriteService;

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<ImportResponseDto> importJson(HttpServletRequest request) {
        logger.info("Streaming JSON import request received");
        try (InputStream inputStream = request.getInputStream()) {
            ImportResponseDto.ImportResult result = importService.streamImportJson(inputStream);
            String message = result.hasErrors()
                    ? "Import completed with " + result.getErrors().size() + " error(s)"
                    : "Import completed successfully";
            logger.info("Import completed. Errors: {}", result.hasErrors() ? result.getErrors().size() : 0);
            return ResponseEntity.ok(ImportResponseDto.success(message, result.getCounts(), result.getErrors()));
        } catch (Exception e) {
            logger.error("Import failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ImportResponseDto.error("Import failed: " + e.getMessage()));
        }
    }

    @GetMapping("/json")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public void exportJson(HttpServletResponse response) throws IOException {
        try {
            logger.info("Starting streaming JSON export");
            // APPLICATION_JSON_VALUE has no charset so MockMvc matches application/json exactly
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            importService.streamExportJson(response.getOutputStream());
            response.flushBuffer();
            logger.info("Streaming JSON export completed");
        } catch (Exception e) {
            logger.error("Streaming JSON export failed", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    /**
     * Database statistics for the Data Management page.
     * {@code favoriteCount} is the raw favorites row count.
     * {@code priceCount} is unique books with a usable AbeBooks listing.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<DatabaseStatsDto> getDatabaseStats() {
        DatabaseStatsDto stats = importService.getDatabaseStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Returns book counts per label, sorted by count descending then alphabetically.
     */
    @GetMapping("/label-counts")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<LabelCountDto>> getLabelCounts() {
        List<LabelCountDto> labelCounts = importService.getLabelCounts();
        return ResponseEntity.ok(labelCounts);
    }

    /**
     * Returns named book-count statistics for electronic, call number, withdrawn, YDL, EMU, and ACLA.
     * hasCallNumber excludes WITHDRAWN and REQUESTED even when a call number is present.
     */
    @GetMapping("/availability-stats")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookAvailabilityStatsDto> getAvailabilityStats() {
        BookAvailabilityStatsDto stats = importService.getAvailabilityStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Returns favorite-list membership counts across all users, split by books and authors.
     * Sorted by total count descending, then list name.
     */
    @GetMapping("/favorite-stats")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<FavoriteListCountDto>> getFavoriteStats() {
        return ResponseEntity.ok(favoriteService.getListCounts());
    }
}
