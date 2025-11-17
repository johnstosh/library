/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for BooksFromFeedService
 *
 * Note: Tests for processPhotosFromFeed method have been removed as the service
 * was refactored to use Google Photos Picker API. The new workflow is:
 * 1. savePhotosFromPicker - saves selected photos from picker to database
 * 2. processSavedPhotos - processes the saved photos with AI
 */
@ExtendWith(MockitoExtension.class)
class BooksFromFeedServiceTest {

    @Mock
    private GooglePhotosService googlePhotosService;

    @Mock
    private AskGrok askGrok;

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private BookService bookService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private BooksFromFeedService booksFromFeedService;

    private UserDto testUser;

    @BeforeEach
    void setUp() {
        // Set up security context
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        // Set up test user
        testUser = new UserDto();
        testUser.setUsername("testuser");
        testUser.setGooglePhotosApiKey("test-api-key");
        testUser.setLastPhotoTimestamp("2025-01-01T00:00:00Z");

        lenient().when(userSettingsService.getUserSettings("testuser")).thenReturn(testUser);
    }

    // TODO: Add tests for savePhotosFromPicker and processSavedPhotos methods
}
