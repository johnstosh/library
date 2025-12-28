/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.PhotoExportInfoDto;
import com.muczynski.library.dto.PhotoExportResponseDto;
import com.muczynski.library.dto.PhotoExportStatsDto;
import com.muczynski.library.dto.PhotoImportResultDto;
import com.muczynski.library.dto.PhotoVerifyResultDto;
import com.muczynski.library.service.PhotoExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<PhotoExportStatsDto> getExportStats() {
        try {
            logger.info("Getting export statistics");
            PhotoExportStatsDto stats = photoExportService.getExportStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get export statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all photos with export status
     */
    @GetMapping("/photos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PhotoExportInfoDto>> getAllPhotosWithExportStatus() {
        try {
            logger.info("Getting all photos with export status");
            List<PhotoExportInfoDto> photos = photoExportService.getAllPhotosWithExportStatus();
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
    public ResponseEntity<PhotoExportResponseDto> exportAllPhotos() {
        try {
            logger.info("Manually triggering export for all photos");
            photoExportService.exportPhotos();

            PhotoExportResponseDto response = new PhotoExportResponseDto();
            response.setMessage("Export process started");
            response.setStats(photoExportService.getExportStats());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to export all photos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Manually trigger export for a specific photo
     */
    @PostMapping("/export/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<PhotoExportResponseDto> exportPhoto(@PathVariable Long photoId) {
        try {
            logger.info("Manually triggering export for photo ID: {}", photoId);
            photoExportService.exportPhotoById(photoId);

            PhotoExportResponseDto response = new PhotoExportResponseDto();
            response.setMessage("Photo export completed successfully");
            response.setPhotoId(photoId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to export photo ID: {}", photoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import a photo from Google Photos using its permanent ID
     */
    @PostMapping("/import/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoExportResponseDto> importPhoto(@PathVariable Long photoId) {
        try {
            logger.info("Importing photo ID: {} from Google Photos", photoId);
            String errorMessage = photoExportService.importPhotoById(photoId);

            PhotoExportResponseDto response = new PhotoExportResponseDto();
            response.setPhotoId(photoId);

            if (errorMessage == null) {
                response.setMessage("Photo imported successfully");
                return ResponseEntity.ok(response);
            } else {
                logger.error("Failed to import photo ID: {}: {}", photoId, errorMessage);
                response.setMessage(errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            logger.error("Failed to import photo ID: {}", photoId, e);
            PhotoExportResponseDto response = new PhotoExportResponseDto();
            response.setPhotoId(photoId);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Import all photos that have permanent IDs but no image data
     */
    @PostMapping("/import-all")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<PhotoImportResultDto> importAllPhotos() {
        try {
            logger.info("Importing all pending photos from Google Photos");
            PhotoImportResultDto result = photoExportService.importAllPhotos();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to import all photos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Verify that a photo's permanent ID still works in Google Photos
     */
    @PostMapping("/verify/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoVerifyResultDto> verifyPhoto(@PathVariable Long photoId) {
        try {
            logger.info("Verifying photo ID: {}", photoId);
            PhotoVerifyResultDto result = photoExportService.verifyPhotoById(photoId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to verify photo ID: {}", photoId, e);
            PhotoVerifyResultDto result = new PhotoVerifyResultDto();
            result.setPhotoId(photoId);
            result.setValid(false);
            result.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Unlink a photo by removing its permanent ID
     */
    @PostMapping("/unlink/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<PhotoExportResponseDto> unlinkPhoto(@PathVariable Long photoId) {
        try {
            logger.info("Unlinking photo ID: {}", photoId);
            photoExportService.unlinkPhotoById(photoId);

            PhotoExportResponseDto response = new PhotoExportResponseDto();
            response.setMessage("Photo unlinked successfully");
            response.setPhotoId(photoId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to unlink photo ID: {}", photoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
