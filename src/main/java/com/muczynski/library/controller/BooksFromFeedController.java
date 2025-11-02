/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.BooksFromFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/books-from-feed")
@PreAuthorize("hasAuthority('LIBRARIAN')")
public class BooksFromFeedController {

    @Autowired
    private BooksFromFeedService booksFromFeedService;

    /**
     * Process photos from Google Photos to create books
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
}
