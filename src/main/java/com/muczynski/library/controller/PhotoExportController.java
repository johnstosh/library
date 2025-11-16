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
     * Get backup statistics
     */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getBackupStats() {
        try {
            logger.info("Getting backup statistics");
            Map<String, Object> stats = photoExportService.getExportStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get backup statistics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get backup statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all photos with backup status
     */
    @GetMapping("/photos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllPhotosWithBackupStatus() {
        try {
            logger.info("Getting all photos with backup status");
            List<Map<String, Object>> photos = photoExportService.getAllPhotosWithExportStatus();
            logger.info("Successfully retrieved {} photos with backup status", photos.size());
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.error("Failed to get photos with backup status - Error: {} - Message: {}",
                e.getClass().getSimpleName(), e.getMessage(), e);
            // Return empty list instead of null to avoid frontend issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new java.util.ArrayList<>());
        }
    }

    /**
     * Manually trigger backup for all pending photos
     */
    @PostMapping("/backup-all")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> backupAllPhotos() {
        try {
            logger.info("Manually triggering backup for all photos");
            photoExportService.exportPhotos();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Backup process completed");
            response.put("stats", photoExportService.getExportStats());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to backup all photos", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to backup photos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Manually trigger backup for a specific photo
     */
    @PostMapping("/backup/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> backupPhoto(@PathVariable Long photoId) {
        try {
            logger.info("Manually triggering backup for photo ID: {}", photoId);
            photoExportService.exportPhotoById(photoId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Photo backup completed successfully");
            response.put("photoId", photoId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to backup photo ID: {}", photoId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to backup photo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
