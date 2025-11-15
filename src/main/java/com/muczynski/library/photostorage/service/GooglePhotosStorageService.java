/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.service;

import com.muczynski.library.photostorage.client.GooglePhotosLibraryClient;
import com.muczynski.library.photostorage.config.PhotoStorageConfig;
import com.muczynski.library.photostorage.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level service for managing photo storage in Google Photos Library API
 * Implements the complete workflows from the 2025 API design:
 * 1. Creating app-created albums
 * 2. Uploading photos to albums
 * 3. Managing permanent media item IDs
 * 4. Fetching photos by ID
 * 5. Updating photo metadata
 * 6. Listing all photos in albums
 *
 * This service provides business logic on top of GooglePhotosLibraryClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePhotosStorageService {

    private final GooglePhotosLibraryClient client;
    private final PhotoStorageConfig config;

    /**
     * Example 1: Create an app-created album (folder)
     * Creates a new album in the user's Google Photos library that can only be managed by this app
     *
     * @param accessToken OAuth access token
     * @param title Album title
     * @param description Album description
     * @return Permanent album ID - store this for future use
     */
    public String createAlbum(String accessToken, String title, String description) {
        log.info("Creating app-created album: {}", title);

        AlbumResponse response = client.createAlbum(accessToken, title, description);
        String albumId = response.getAlbum().getId();

        log.info("Successfully created album '{}' with ID: {}", title, albumId);
        log.info("Album URL: {}", response.getAlbum().getProductUrl());

        return albumId;
    }

    /**
     * Example 2: Upload a photo to an album and get permanent ID
     * Two-step process: upload bytes, then create media item
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID to add photo to
     * @param photoBytes Photo bytes
     * @param mimeType MIME type (e.g., "image/jpeg")
     * @param description Photo description
     * @return Permanent media item ID - store this to reference the photo later
     */
    public String uploadPhotoToAlbum(String accessToken, String albumId, byte[] photoBytes,
                                     String mimeType, String description) {
        log.info("Uploading photo to album: {} ({} bytes)", albumId, photoBytes.length);

        // Step 1: Upload bytes to get token
        String uploadToken = client.uploadBytes(accessToken, photoBytes, mimeType);

        // Step 2: Create media item in album
        BatchCreateRequest.NewMediaItem item = new BatchCreateRequest.NewMediaItem();
        item.setDescription(description);

        BatchCreateRequest.SimpleMediaItem simpleItem = new BatchCreateRequest.SimpleMediaItem();
        simpleItem.setUploadToken(uploadToken);
        item.setSimpleMediaItem(simpleItem);

        BatchCreateResponse response = client.batchCreate(accessToken, albumId, List.of(item));

        String mediaItemId = response.getNewMediaItemResults().get(0).getMediaItem().getId();

        log.info("Successfully uploaded photo with ID: {}", mediaItemId);
        return mediaItemId;
    }

    /**
     * Example 2 (variant): Upload a photo from file path
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID
     * @param photoPath Path to photo file
     * @param description Photo description
     * @return Permanent media item ID
     * @throws IOException if file cannot be read
     */
    public String uploadPhotoFromFile(String accessToken, String albumId, Path photoPath, String description)
            throws IOException {
        byte[] photoBytes = Files.readAllBytes(photoPath);
        String mimeType = Files.probeContentType(photoPath);
        if (mimeType == null) {
            mimeType = "image/jpeg"; // default
        }

        return uploadPhotoToAlbum(accessToken, albumId, photoBytes, mimeType, description);
    }

    /**
     * Example 3: Extract permanent reference ID
     * Note: The permanent ID is already returned from uploadPhotoToAlbum
     * This example shows how to extract it from a batch create response
     *
     * @param response BatchCreateResponse from upload
     * @return List of permanent media item IDs
     */
    public List<String> extractPermanentIds(BatchCreateResponse response) {
        List<String> permanentIds = new ArrayList<>();

        for (BatchCreateResponse.NewMediaItemResult result : response.getNewMediaItemResults()) {
            if (result.getStatus().getMessage().equals("Success") && result.getMediaItem() != null) {
                permanentIds.add(result.getMediaItem().getId());
            } else {
                log.warn("Failed to create media item with upload token: {}. Status: {}",
                        result.getUploadToken(), result.getStatus().getMessage());
            }
        }

        log.info("Extracted {} permanent media item IDs", permanentIds.size());
        return permanentIds;
    }

    /**
     * Example 4: Fetch a photo by its permanent ID
     * Gets the media item metadata including a fresh baseUrl for downloading
     *
     * @param accessToken OAuth access token
     * @param mediaItemId Permanent media item ID
     * @return MediaItemResponse with all metadata and baseUrl
     */
    public MediaItemResponse getPhotoById(String accessToken, String mediaItemId) {
        log.info("Fetching photo by ID: {}", mediaItemId);

        MediaItemResponse response = client.getMediaItem(accessToken, mediaItemId);

        log.info("Retrieved photo '{}' ({})", response.getFilename(), response.getMimeType());
        log.debug("Base URL: {}", response.getBaseUrl());

        return response;
    }

    /**
     * Example 4 (variant): Download photo bytes by permanent ID
     *
     * @param accessToken OAuth access token
     * @param mediaItemId Permanent media item ID
     * @return Photo bytes
     */
    public byte[] downloadPhotoById(String accessToken, String mediaItemId) {
        MediaItemResponse item = getPhotoById(accessToken, mediaItemId);
        return client.downloadPhoto(accessToken, item.getBaseUrl());
    }

    /**
     * Example 4 (variant): Get a sized photo URL
     *
     * @param accessToken OAuth access token
     * @param mediaItemId Permanent media item ID
     * @param width Desired width
     * @param height Desired height (can be null)
     * @return URL to sized photo
     */
    public String getSizedPhotoUrl(String accessToken, String mediaItemId, Integer width, Integer height) {
        MediaItemResponse item = getPhotoById(accessToken, mediaItemId);
        return client.getSizedPhotoUrl(item.getBaseUrl(), width, height);
    }

    /**
     * Example 5: Update photo metadata (description only)
     * Note: This updates metadata only. To replace the actual image, you must upload a new photo
     * which will get a NEW media item ID
     *
     * @param accessToken OAuth access token
     * @param mediaItemId Media item ID to update
     * @param newDescription New description
     */
    public void updatePhotoDescription(String accessToken, String mediaItemId, String newDescription) {
        log.info("Updating description for media item: {}", mediaItemId);

        client.batchUpdateMetadata(accessToken, mediaItemId, newDescription, null);

        log.info("Successfully updated description for: {}", mediaItemId);
    }

    /**
     * Example 5 (variant): Replace a photo (creates new media item with NEW ID)
     * To "update" the actual image content, you must:
     * 1. Upload the new photo (gets a NEW media item ID)
     * 2. Optionally delete or keep the old photo
     * 3. Update your database to reference the NEW ID
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID
     * @param newPhotoBytes New photo bytes
     * @param mimeType MIME type
     * @param description Description
     * @return NEW permanent media item ID for the replacement photo
     */
    public String replacePhoto(String accessToken, String albumId, byte[] newPhotoBytes,
                              String mimeType, String description) {
        log.info("Replacing photo (uploading new version)");

        // Upload new photo - this creates a NEW media item with a NEW ID
        String newMediaItemId = uploadPhotoToAlbum(accessToken, albumId, newPhotoBytes, mimeType, description);

        log.info("Replacement photo uploaded with NEW ID: {}", newMediaItemId);
        log.warn("Remember: The old media item still exists with its original ID");

        return newMediaItemId;
    }

    /**
     * Example 6: List all photos in an album with automatic pagination
     * Retrieves all media items from an app-created album
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID to list
     * @return Complete list of all media items in the album
     */
    public List<MediaItemResponse> listAllPhotosInAlbum(String accessToken, String albumId) {
        log.info("Listing all photos in album: {}", albumId);

        List<MediaItemResponse> allPhotos = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;

        do {
            pageCount++;
            log.debug("Fetching page {} (pageToken: {})", pageCount, pageToken != null ? "present" : "null");

            SearchResponse response = client.searchAlbum(accessToken, albumId, null, pageToken);

            if (response.getMediaItems() != null && !response.getMediaItems().isEmpty()) {
                allPhotos.addAll(response.getMediaItems());
                log.debug("Page {} returned {} items", pageCount, response.getMediaItems().size());
            }

            pageToken = response.getNextPageToken();

        } while (pageToken != null);

        log.info("Retrieved {} total photos from album {} ({} pages)", allPhotos.size(), albumId, pageCount);
        return allPhotos;
    }

    /**
     * Example 6 (variant): List photos with callback for streaming processing
     * Useful for large albums where you want to process items as they're fetched
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID
     * @param callback Callback to process each page of results
     */
    public void listPhotosWithCallback(String accessToken, String albumId,
                                       PhotoPageCallback callback) {
        log.info("Listing photos in album with callback: {}", albumId);

        String pageToken = null;
        int pageCount = 0;
        int totalProcessed = 0;

        do {
            pageCount++;
            SearchResponse response = client.searchAlbum(accessToken, albumId, null, pageToken);

            if (response.getMediaItems() != null && !response.getMediaItems().isEmpty()) {
                callback.onPage(response.getMediaItems(), pageCount);
                totalProcessed += response.getMediaItems().size();
            }

            pageToken = response.getNextPageToken();

        } while (pageToken != null);

        log.info("Processed {} total photos from {} pages", totalProcessed, pageCount);
    }

    /**
     * Callback interface for streaming photo list processing
     */
    @FunctionalInterface
    public interface PhotoPageCallback {
        void onPage(List<MediaItemResponse> photos, int pageNumber);
    }

    /**
     * Helper: Initialize book covers album (run once during setup)
     * Creates the album and returns its ID to be configured in application.yml
     *
     * @param accessToken OAuth access token
     * @return Album ID to configure in google.photos.book-covers-album-id
     */
    public String initializeBookCoversAlbum(String accessToken) {
        String albumId = createAlbum(
                accessToken,
                "Library Book Covers",
                "Book cover photos uploaded via Library Management System"
        );

        log.info("============================================");
        log.info("Book Covers Album Created!");
        log.info("Add this to your application.yml:");
        log.info("google:");
        log.info("  photos:");
        log.info("    book-covers-album-id: {}", albumId);
        log.info("============================================");

        return albumId;
    }

    /**
     * Helper: Initialize author photos album (run once during setup)
     * Creates the album and returns its ID to be configured in application.yml
     *
     * @param accessToken OAuth access token
     * @return Album ID to configure in google.photos.author-photos-album-id
     */
    public String initializeAuthorPhotosAlbum(String accessToken) {
        String albumId = createAlbum(
                accessToken,
                "Library Author Photos",
                "Author photos uploaded via Library Management System"
        );

        log.info("============================================");
        log.info("Author Photos Album Created!");
        log.info("Add this to your application.yml:");
        log.info("google:");
        log.info("  photos:");
        log.info("    author-photos-album-id: {}", albumId);
        log.info("============================================");

        return albumId;
    }

    /**
     * Get configured book covers album ID from config
     */
    public String getBookCoversAlbumId() {
        if (config.getBookCoversAlbumId() == null || config.getBookCoversAlbumId().isEmpty()) {
            throw new IllegalStateException(
                    "Book covers album ID not configured. " +
                    "Run initializeBookCoversAlbum() and update application.yml"
            );
        }
        return config.getBookCoversAlbumId();
    }

    /**
     * Get configured author photos album ID from config
     */
    public String getAuthorPhotosAlbumId() {
        if (config.getAuthorPhotosAlbumId() == null || config.getAuthorPhotosAlbumId().isEmpty()) {
            throw new IllegalStateException(
                    "Author photos album ID not configured. " +
                    "Run initializeAuthorPhotosAlbum() and update application.yml"
            );
        }
        return config.getAuthorPhotosAlbumId();
    }
}
