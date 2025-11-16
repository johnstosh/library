/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.PhotoExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photo-export")
public class PhotoExportController {

    private static final Logger logger = LoggerFactory.getLogger(PhotoExportController.class);

    @Autowired
    private PhotoExportService photoExportService;

    /**
     * Get export statistics
     */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getExportStats() {
        try {
            logger.info("Getting export statistics");
            Map<String, Object> stats = photoExportService.getExportStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get export statistics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get export statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all photos with export status
     */
    @GetMapping("/photos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllPhotosWithExportStatus() {
        try {
            logger.info("Getting all photos with export status");
            List<Map<String, Object>> photos = photoExportService.getAllPhotosWithExportStatus();
            logger.info("Successfully retrieved {} photos with export status", photos.size());
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.error("Failed to get photos with export status - Error: {} - Message: {}",
                e.getClass().getSimpleName(), e.getMessage(), e);
            // Return empty list instead of null to avoid frontend issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new java.util.ArrayList<>());
        }
    }

    /**
     * Manually trigger export for all pending photos
     */
    @PostMapping("/export-all")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> exportAllPhotos() {
        try {
            logger.info("Manually triggering export for all photos");
            photoExportService.exportPhotos();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Export process started");
            response.put("stats", photoExportService.getExportStats());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to export all photos", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to export photos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Manually trigger export for a specific photo
     */
    @PostMapping("/export/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> exportPhoto(@PathVariable Long photoId) {
        try {
            logger.info("Manually triggering export for photo ID: {}", photoId);
            photoExportService.exportPhotoById(photoId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Photo export completed successfully");
            response.put("photoId", photoId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to export photo ID: {}", photoId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to export photo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
