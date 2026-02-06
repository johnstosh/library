/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.SavedBookDto;
import com.muczynski.library.service.BookService;
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
    private BookService bookService;

    @Autowired
    private com.muczynski.library.service.GooglePhotosService googlePhotosService;

    /**
     * Get books from most recent day OR with temporary titles (date-pattern titles).
     * Uses efficient projection query - no N+1 queries.
     * Delegates to BookService.getBooksFromMostRecentDay().
     *
     * @return List of SavedBookDto with id, title, author, library, photoCount, needsProcessing
     */
    @GetMapping("/saved-books")
    public ResponseEntity<List<SavedBookDto>> getSavedBooks() {
        try {
            List<SavedBookDto> books = bookService.getBooksFromMostRecentDay();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }

    /**
     * Process a single saved book using AI book-by-photo workflow
     * @param bookId The ID of the book to process
     * @return Processing result for this book
     */
    @PostMapping("/process-single/{bookId}")
    public ResponseEntity<Map<String, Object>> processSingleBook(@PathVariable Long bookId) {
        try {
            Map<String, Object> result = booksFromFeedService.processSingleBook(bookId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "bookId", bookId,
                    "error", message
            ));
        }
    }

    /**
     * Phase 2: Process saved photos using AI book-by-photo workflow
     * This processes photos that were previously saved to the database (from save-from-picker)
     * @return Processing results
     */
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
     * Save photos selected via Google Photos Picker API to database (Phase 1 - No AI)
     * @param request Contains 'photos' array with photo metadata from Picker
     * @return Save results
     */
    @PostMapping("/save-from-picker")
    public ResponseEntity<Map<String, Object>> savePhotosFromPicker(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) request.get("photos");

            if (photos == null || photos.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No photos provided",
                        "savedCount", 0,
                        "skippedCount", 0,
                        "totalPhotos", 0
                ));
            }

            Map<String, Object> result = booksFromFeedService.savePhotosFromPicker(photos);
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
