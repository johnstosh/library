/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.client;

import com.muczynski.library.photostorage.config.PhotoStorageConfig;
import com.muczynski.library.photostorage.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for Google Photos Library API (2025 app-created content model)
 * Handles low-level REST API communication with Google Photos
 *
 * Required OAuth scopes:
 * - https://www.googleapis.com/auth/photoslibrary.appendonly
 * - https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata
 * - https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePhotosLibraryClient {

    private final RestTemplate restTemplate;
    private final PhotoStorageConfig config;

    /**
     * Create HTTP headers with Bearer token authentication
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Create a new album in Google Photos
     * Required scope: photoslibrary.appendonly or photoslibrary.edit.appcreateddata
     *
     * @param accessToken OAuth access token
     * @param title Album title
     * @param description Album description
     * @return AlbumResponse with album ID and details
     */
    public AlbumResponse createAlbum(String accessToken, String title, String description) {
        log.info("Creating Google Photos album: {}", title);

        AlbumCreateRequest request = new AlbumCreateRequest();
        AlbumCreateRequest.Album album = new AlbumCreateRequest.Album();
        album.setTitle(title);
        album.setDescription(description);
        request.setAlbum(album);

        HttpEntity<AlbumCreateRequest> entity = new HttpEntity<>(request, createAuthHeaders(accessToken));

        ResponseEntity<AlbumResponse> response = restTemplate.postForEntity(
                config.getBaseUrl() + "/albums",
                entity,
                AlbumResponse.class
        );

        log.info("Created album with ID: {}", response.getBody().getAlbum().getId());
        return response.getBody();
    }

    /**
     * Upload photo bytes to Google Photos and get upload token
     * This is step 1 of the 2-step upload process
     * Required scope: photoslibrary.appendonly
     *
     * @param accessToken OAuth access token
     * @param photoBytes Raw photo bytes
     * @param mimeType MIME type (e.g., "image/jpeg", "image/png")
     * @return Upload token to be used in batchCreate
     */
    public String uploadBytes(String accessToken, byte[] photoBytes, String mimeType) {
        log.debug("Uploading {} bytes with MIME type: {}", photoBytes.length, mimeType);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("X-Goog-Upload-Content-Type", mimeType);

        HttpEntity<byte[]> entity = new HttpEntity<>(photoBytes, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                config.getUploadUrl(),
                entity,
                String.class
        );

        String uploadToken = response.getBody();
        log.debug("Received upload token (length: {})", uploadToken != null ? uploadToken.length() : 0);
        return uploadToken;
    }

    /**
     * Batch create media items from upload tokens
     * This is step 2 of the 2-step upload process
     * Required scope: photoslibrary.appendonly
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID to add items to (can be null for library root)
     * @param items List of new media items with upload tokens
     * @return BatchCreateResponse with created media item IDs
     */
    public BatchCreateResponse batchCreate(String accessToken, String albumId, List<BatchCreateRequest.NewMediaItem> items) {
        log.info("Batch creating {} media items", items.size());

        BatchCreateRequest request = new BatchCreateRequest();
        request.setAlbumId(albumId);
        request.setNewMediaItems(items);

        HttpEntity<BatchCreateRequest> entity = new HttpEntity<>(request, createAuthHeaders(accessToken));

        ResponseEntity<BatchCreateResponse> response = restTemplate.postForEntity(
                config.getBaseUrl() + "/mediaItems:batchCreate",
                entity,
                BatchCreateResponse.class
        );

        log.info("Batch create completed: {} items", response.getBody().getNewMediaItemResults().size());
        return response.getBody();
    }

    /**
     * Get a media item by its permanent ID
     * Required scope: photoslibrary.readonly.appcreateddata
     *
     * @param accessToken OAuth access token
     * @param mediaItemId Permanent media item ID
     * @return MediaItemResponse with item details including baseUrl
     */
    public MediaItemResponse getMediaItem(String accessToken, String mediaItemId) {
        log.debug("Getting media item: {}", mediaItemId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<MediaItemResponse> response = restTemplate.exchange(
                config.getBaseUrl() + "/mediaItems/" + mediaItemId,
                HttpMethod.GET,
                entity,
                MediaItemResponse.class
        );

        return response.getBody();
    }

    /**
     * Batch update media item metadata (description, filename)
     * Required scope: photoslibrary.edit.appcreateddata
     *
     * @param accessToken OAuth access token
     * @param mediaItemId Media item ID to update
     * @param newDescription New description (use null to skip)
     * @param newFilename New filename (use null to skip)
     * @return BatchUpdateResponse with update status
     */
    public BatchUpdateResponse batchUpdateMetadata(String accessToken, String mediaItemId,
                                                   String newDescription, String newFilename) {
        log.info("Updating metadata for media item: {}", mediaItemId);

        BatchUpdateRequest.MediaItemUpdate update = new BatchUpdateRequest.MediaItemUpdate();
        update.setMediaItemId(mediaItemId);

        // Build update mask based on what's being updated
        StringBuilder updateMask = new StringBuilder();
        BatchUpdateRequest.MediaItemUpdate.MediaItem mediaItem = new BatchUpdateRequest.MediaItemUpdate.MediaItem();

        if (newDescription != null) {
            mediaItem.setDescription(newDescription);
            updateMask.append("description");
        }
        if (newFilename != null) {
            mediaItem.setFilename(newFilename);
            if (updateMask.length() > 0) {
                updateMask.append(",");
            }
            updateMask.append("filename");
        }

        update.setUpdateMask(updateMask.toString());
        update.setMediaItem(mediaItem);

        BatchUpdateRequest request = new BatchUpdateRequest();
        request.setMediaItemUpdates(List.of(update));

        HttpEntity<BatchUpdateRequest> entity = new HttpEntity<>(request, createAuthHeaders(accessToken));

        ResponseEntity<BatchUpdateResponse> response = restTemplate.postForEntity(
                config.getBaseUrl() + "/mediaItems:batchUpdate",
                entity,
                BatchUpdateResponse.class
        );

        log.info("Metadata update completed for: {}", mediaItemId);
        return response.getBody();
    }

    /**
     * Search for media items in an album with pagination
     * Required scope: photoslibrary.readonly.appcreateddata
     *
     * @param accessToken OAuth access token
     * @param albumId Album ID to search in
     * @param pageSize Number of results per page (max 100)
     * @param pageToken Page token for pagination (null for first page)
     * @return SearchResponse with media items and next page token
     */
    public SearchResponse searchAlbum(String accessToken, String albumId, Integer pageSize, String pageToken) {
        log.debug("Searching album: {} (pageSize: {}, pageToken: {})", albumId, pageSize, pageToken != null ? "present" : "null");

        SearchRequest request = new SearchRequest();
        request.setAlbumId(albumId);
        request.setPageSize(pageSize != null ? pageSize : config.getDefaultPageSize());
        request.setPageToken(pageToken);

        HttpEntity<SearchRequest> entity = new HttpEntity<>(request, createAuthHeaders(accessToken));

        ResponseEntity<SearchResponse> response = restTemplate.postForEntity(
                config.getBaseUrl() + "/mediaItems:search",
                entity,
                SearchResponse.class
        );

        SearchResponse searchResponse = response.getBody();
        log.debug("Search returned {} items, nextPageToken: {}",
                searchResponse.getMediaItems() != null ? searchResponse.getMediaItems().size() : 0,
                searchResponse.getNextPageToken() != null ? "present" : "null");

        return searchResponse;
    }

    /**
     * Download photo bytes from a baseUrl
     * Note: baseUrl expires after ~1 hour, so this should be called with fresh URLs
     *
     * @param accessToken OAuth access token
     * @param baseUrl Base URL from MediaItemResponse
     * @return Photo bytes
     */
    public byte[] downloadPhoto(String accessToken, String baseUrl) {
        // Add download parameter to get original resolution
        String downloadUrl = baseUrl + "=d";

        log.debug("Downloading photo from: {}", downloadUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        byte[] photoBytes = response.getBody();
        log.info("Downloaded photo: {} bytes", photoBytes != null ? photoBytes.length : 0);
        return photoBytes;
    }

    /**
     * Get a sized version of a photo from baseUrl
     *
     * @param baseUrl Base URL from MediaItemResponse
     * @param width Desired width in pixels
     * @param height Desired height in pixels (optional)
     * @return Full URL with size parameters
     */
    public String getSizedPhotoUrl(String baseUrl, Integer width, Integer height) {
        if (height != null) {
            return baseUrl + "=w" + width + "-h" + height;
        } else {
            return baseUrl + "=w" + width;
        }
    }
}
