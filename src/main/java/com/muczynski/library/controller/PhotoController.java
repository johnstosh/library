/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.ErrorResponse;
import com.muczynski.library.dto.PhotoZipImportResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.service.PhotoService;
import com.muczynski.library.service.PhotoZipImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private static final Logger logger = LoggerFactory.getLogger(PhotoController.class);

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoZipImportService photoZipImportService;

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}/image")
    public ResponseEntity<?> getImage(@PathVariable Long id) {
        try {
            logger.debug("Image request for photo ID {}", id);
            byte[] image = photoService.getImage(id);
            if (image == null) {
                logger.warn("Image not found for photo ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            Photo photo = photoService.getPhotoById(id);
            String contentType = photo != null ? photo.getContentType() : MediaType.IMAGE_JPEG_VALUE;
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(image);
        } catch (LibraryException e) {
            if (e.getMessage() != null && e.getMessage().contains("Photo not found")) {
                logger.warn("Photo not found for image request: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            logger.error("Failed to retrieve image for photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to retrieve image for photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<?> getThumbnail(@PathVariable Long id, @RequestParam Integer width) {
        try {
            logger.debug("Thumbnail request for photo ID {} with width {}", id, width);
            Pair<byte[], String> thumbnailData = photoService.getThumbnail(id, width);
            if (thumbnailData == null) {
                logger.warn("Thumbnail not found for photo ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            logger.debug("Successfully generated thumbnail for photo ID {}", id);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(thumbnailData.getSecond()))
                    .body(thumbnailData.getFirst());
        } catch (LibraryException e) {
            if (e.getMessage() != null && e.getMessage().contains("Photo not found")) {
                logger.warn("Photo not found for thumbnail request: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            logger.error("Failed to generate thumbnail for photo ID {} with width {}: {}", id, width, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to generate thumbnail for photo ID {} with width {}: {}", id, width, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id) {
        try {
            logger.debug("Delete request for photo ID {}", id);
            photoService.softDeletePhoto(id);
            return ResponseEntity.ok().build();
        } catch (LibraryException e) {
            if (e.getMessage() != null && e.getMessage().contains("Photo not found")) {
                logger.warn("Photo not found for delete request: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            logger.error("Failed to delete photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restorePhoto(@PathVariable Long id) {
        try {
            logger.debug("Restore request for photo ID {}", id);
            photoService.restorePhoto(id);
            return ResponseEntity.ok().build();
        } catch (LibraryException e) {
            if (e.getMessage() != null && e.getMessage().contains("Photo not found")) {
                logger.warn("Photo not found for restore request: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            logger.error("Failed to restore photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to restore photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PutMapping("/{id}/crop")
    public ResponseEntity<?> cropPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            logger.debug("Crop request for photo ID {}", id);
            photoService.cropPhoto(id, file);
            return ResponseEntity.ok().build();
        } catch (LibraryException e) {
            if (e.getMessage() != null && e.getMessage().contains("Photo not found")) {
                logger.warn("Photo not found for crop request: ID {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Photo not found"));
            }
            logger.error("Failed to crop photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to crop photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }
    }

    /**
     * Import photos from a ZIP file (small files, < 500MB).
     * Filename format determines which entity the photo is associated with:
     * - book-{title}[-{n}].{ext} - Associates photo with a book by title
     * - author-{name}[-{n}].{ext} - Associates photo with an author by name
     * - loan-{title}-{username}[-{n}].{ext} - Associates photo with a loan
     */
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PostMapping("/import-zip")
    public ResponseEntity<?> importFromZip(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("ZIP import request received: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Bad Request", "File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Bad Request", "File must be a ZIP archive"));
            }

            PhotoZipImportResultDto result = photoZipImportService.importFromZip(file);

            logger.info("ZIP import completed: {} total, {} success, {} failed, {} skipped",
                    result.getTotalFiles(), result.getSuccessCount(),
                    result.getFailureCount(), result.getSkippedCount());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to import photos from ZIP: {}", e.getMessage(), e);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Failed to import photos from ZIP: " + detail));
        }
    }

    /**
     * Import photos from a ZIP file using streaming (for large files).
     * This endpoint processes the ZIP as it streams in, without buffering the entire file.
     * Supports files of any size (tested with 6GB+).
     *
     * Usage: POST /api/photos/import-zip-stream with raw ZIP bytes in request body
     * Content-Type: application/zip or application/octet-stream
     */
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PostMapping(value = "/import-zip-stream", consumes = {"application/zip", "application/octet-stream"})
    public ResponseEntity<?> importFromZipStream(HttpServletRequest request) {
        try {
            logger.info("Streaming ZIP import request received");

            PhotoZipImportResultDto result = photoZipImportService.importFromZipStream(request.getInputStream());

            logger.info("Streaming ZIP import completed: {} total, {} success, {} failed, {} skipped",
                    result.getTotalFiles(), result.getSuccessCount(),
                    result.getFailureCount(), result.getSkippedCount());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to import photos from ZIP stream: {}", e.getMessage(), e);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Failed to import photos from ZIP: " + detail));
        }
    }
}
