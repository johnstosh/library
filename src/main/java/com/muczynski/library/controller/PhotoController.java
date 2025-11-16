/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Photo;
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

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private static final Logger logger = LoggerFactory.getLogger(PhotoController.class);

    @Autowired
    private PhotoService photoService;

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        try {
            byte[] image = photoService.getImage(id);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            Photo photo = photoService.getPhotoById(id);
            String contentType = photo != null ? photo.getContentType() : MediaType.IMAGE_JPEG_VALUE;
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(image);
        } catch (Exception e) {
            logger.warn("Failed to retrieve image for photo ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id, @RequestParam Integer width) {
        try {
            Pair<byte[], String> thumbnailData = photoService.getThumbnail(id, width);
            if (thumbnailData == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(thumbnailData.getSecond()))
                    .body(thumbnailData.getFirst());
        } catch (Exception e) {
            logger.warn("Failed to generate thumbnail for photo ID {} with width {}: {}", id, width, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
