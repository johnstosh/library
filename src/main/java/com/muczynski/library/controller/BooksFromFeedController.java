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
     * Phase 1: Fetch photos from Google Photos and save to database
     * This completes quickly before Google Photos connection timeout
     * @return Fetch results
     * @deprecated Use POST /process-from-picker instead. The mediaItems:search API is deprecated.
     */
    @Deprecated
    @PostMapping("/fetch-photos")
    public ResponseEntity<Map<String, Object>> fetchPhotos() {
        try {
            Map<String, Object> result = booksFromFeedService.fetchAndSavePhotos();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "savedCount", 0,
                    "skippedCount", 0,
                    "totalPhotos", 0
            ));
        }
    }

    /**
     * Phase 2: Process saved photos using AI book-by-photo workflow
     * This processes photos that were previously saved to the database
     * @return Processing results
     * @deprecated Use POST /process-from-picker instead. The mediaItems:search API is deprecated.
     */
    @Deprecated
    @PostMapping("/process-saved")
    public ResponseEntity<Map<String, Object>> processSavedPhotos() {
        try {
            Map<String, Object> result = booksFromFeedService.processSavedPhotos();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "processedCount", 0,
                    "failedCount", 0,
                    "totalBooks", 0
            ));
        }
    }

    /**
     * Legacy: Process photos from Google Photos Library API in one step
     * WARNING: May timeout due to long-running Google Photos connection
     * @deprecated Use POST /fetch-photos then POST /process-saved instead
     * @return Processing results
     */
    @Deprecated
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
     * Create a new Google Photos Picker session (server-side to handle token refresh)
     * @return Session info with id and pickerUri
     */
    @PostMapping("/picker-session")
    public ResponseEntity<Map<String, Object>> createPickerSession() {
        try {
            Map<String, Object> session = googlePhotosService.createPickerSession();
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Poll a Google Photos Picker session for completion status
     * @param sessionId The session ID from the Picker API
     * @return Session status with mediaItemsSet field
     */
    @GetMapping("/picker-session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getPickerSessionStatus(@PathVariable String sessionId) {
        try {
            Map<String, Object> status = googlePhotosService.getPickerSessionStatus(sessionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
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
