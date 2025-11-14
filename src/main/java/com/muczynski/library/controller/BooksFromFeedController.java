/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.BooksFromFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books-from-feed")
@PreAuthorize("hasAuthority('LIBRARIAN')")
public class BooksFromFeedController {

    @Autowired
    private BooksFromFeedService booksFromFeedService;

    @Autowired
    private com.muczynski.library.service.GooglePhotosService googlePhotosService;

    /**
     * Process photos from Google Photos Library API (complex - has 403 scope issues)
     * @return Processing results
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPhotos() {
        try {
            Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "processedCount", 0,
                    "skippedCount", 0,
                    "totalPhotos", 0
            ));
        }
    }

    /**
     * Process photos selected via Google Photos Picker API
     * @param request Contains 'photos' array with photo metadata from Picker
     * @return Processing results
     */
    @PostMapping("/process-from-picker")
    public ResponseEntity<Map<String, Object>> processPhotosFromPicker(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) request.get("photos");

            if (photos == null || photos.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No photos provided",
                        "processedCount", 0,
                        "skippedCount", 0,
                        "totalPhotos", 0
                ));
            }

            Map<String, Object> result = booksFromFeedService.processPhotosFromPicker(photos);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "processedCount", 0,
                    "skippedCount", 0,
                    "totalPhotos", 0
            ));
        }
    }

    /**
     * Fetch media items from a Google Photos Picker session (server-side to avoid CORS)
     * @param sessionId The session ID from the Picker API
     * @return Media items list
     */
    @GetMapping("/picker-session/{sessionId}/media-items")
    public ResponseEntity<Map<String, Object>> getPickerMediaItems(@PathVariable String sessionId) {
        try {
            List<Map<String, Object>> mediaItems = googlePhotosService.fetchPickerMediaItems(sessionId);
            return ResponseEntity.ok(Map.of(
                    "mediaItems", mediaItems,
                    "count", mediaItems.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "mediaItems", List.of()
            ));
        }
    }
}
