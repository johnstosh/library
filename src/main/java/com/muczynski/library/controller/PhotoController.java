/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.ErrorResponse;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.service.PhotoService;
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
}
