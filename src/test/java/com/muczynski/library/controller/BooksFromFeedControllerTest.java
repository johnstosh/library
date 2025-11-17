/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.BooksFromFeedService;
import com.muczynski.library.service.GooglePhotosService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for BooksFromFeedController
 *
 * Note: Tests for processPhotos endpoint have been removed as the controller
 * was refactored to use Google Photos Picker API. The new endpoints are:
 * - POST /api/books-from-feed/process-saved-photos
 * - POST /api/books-from-feed/save-photos
 * - POST /api/books-from-feed/picker-session
 * - GET /api/books-from-feed/picker-session/{sessionId}
 * - GET /api/books-from-feed/picker-session/{sessionId}/media-items
 */
@ExtendWith(MockitoExtension.class)
class BooksFromFeedControllerTest {

    @Mock
    private BooksFromFeedService booksFromFeedService;

    @Mock
    private GooglePhotosService googlePhotosService;

    @InjectMocks
    private BooksFromFeedController booksFromFeedController;

    // TODO: Add tests for the new Google Photos Picker API endpoints:
    // - processSavedPhotos()
    // - savePhotosFromPicker()
    // - createPickerSession()
    // - getPickerSessionStatus()
    // - getPickerMediaItems()
}
