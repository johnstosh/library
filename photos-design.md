# Google Photos Library API - 2025 Design Document

## Overview

This document outlines the integration of Google Photos Library API with our library management system, incorporating the significant changes introduced in 2025 that prioritize user privacy and security.

## Executive Summary

As of **March 31, 2025**, Google Photos API underwent major changes:
- **Broad library access was restricted** - apps can no longer freely read or search entire user photo libraries
- **New focus on app-created content** - apps can only manage photos/albums they create
- **New Picker API** - secure user-selected photo access without granting full library permissions
- **New OAuth scopes** - previous scopes were deprecated/removed

## 2025 API Changes

### What Changed

#### Removed Scopes (No Longer Valid)
These scopes now cause **403 Forbidden** errors:

| Scope | Previous Use | Status |
|-------|--------------|--------|
| `https://www.googleapis.com/auth/photoslibrary.readonly` | Full read access to user's library | ❌ REMOVED |
| `https://www.googleapis.com/auth/photoslibrary.sharing` | Sharing-related access (shared albums) | ❌ REMOVED |
| `https://www.googleapis.com/auth/photoslibrary` | Full read/write access | ❌ REMOVED |

#### New/Updated Scopes (2025)

| Scope | Description | Use Case | Status |
|-------|-------------|----------|--------|
| `https://www.googleapis.com/auth/photospicker.mediaitems.readonly` | Access to Picker sessions and user-selected media | Retrieving specific photos users choose via Picker UI (no full library access) | ✅ NEW (2024/2025) |
| `https://www.googleapis.com/auth/photoslibrary.appendonly` | Write-only: Upload photos, create albums, add enrichments | Storing/uploading new user photos. Cannot read existing items | ✅ ACTIVE |
| `https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata` | Read access limited to app-created media/albums | Retrieving photos your app previously uploaded | ✅ ACTIVE (refocused) |
| `https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata` | Edit access for app-created albums/media items | Editing metadata of your app's uploaded photos | ✅ NEW (2025) |

### Migration Strategy

For our library application, we have **two approaches**:

1. **App-Created Content Management** (Recommended for book photos uploaded by users)
   - Use `photoslibrary.appendonly` to upload book cover photos
   - Use `photoslibrary.readonly.appcreateddata` to retrieve our uploaded photos
   - Use `photoslibrary.edit.appcreateddata` to update photo metadata

2. **Google Photos Picker** (Recommended for importing existing photos)
   - Use `photospicker.mediaitems.readonly` for users to select photos from their library
   - Provides secure, one-time access to specific user-selected photos
   - No broad permissions required

## Architecture Design

### Current State Analysis

Our existing `GooglePhotosService.java` uses:
- ❌ `photoslibrary.readonly` - **DEPRECATED** (will fail with 403)
- Photo search across entire library - **NO LONGER SUPPORTED**
- Direct photo downloads - **STILL WORKS** for app-created content only

### Proposed Architecture

```
┌─────────────────────────────────────────────────────────┐
│                 Library Application                      │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────┐      ┌─────────────────────────┐  │
│  │   Book Photos    │      │  User Photo Selection   │  │
│  │   (Upload/Store) │      │  (Import from Library)  │  │
│  └────────┬─────────┘      └──────────┬──────────────┘  │
│           │                           │                  │
│           │                           │                  │
│  ┌────────▼─────────┐      ┌─────────▼──────────────┐  │
│  │ App-Created API  │      │   Picker API           │  │
│  │ - appendonly     │      │ - picker.readonly      │  │
│  │ - readonly.app   │      │                        │  │
│  │ - edit.app       │      │                        │  │
│  └──────────────────┘      └────────────────────────┘  │
│                                                           │
└───────────────────────────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  Google Photos API     │
              │  - Library API         │
              │  - Picker API          │
              └────────────────────────┘
```

### Data Flow

#### Upload Flow (App-Created Content)
```
1. User takes photo of book cover
2. Upload bytes → Get upload token
3. Create media item in app-created album → Get permanent mediaItem.id
4. Store mediaItem.id in local Photo entity
5. Use mediaItem.id to retrieve photo later
```

#### Import Flow (Picker API)
```
1. User requests to import photo from their library
2. Create Picker session → Get session.id and pickerUri
3. Redirect user to pickerUri (Google's secure UI)
4. User selects photos
5. Poll session status until mediaItemsSet = true
6. Fetch selected media items using session.id
7. Download and store locally
```

## Implementation Examples

### Table of Contents

1. [Making an App-Created Folder (Album)](#1-making-an-app-created-folder-album)
2. [Putting Photos into the Folder](#2-putting-photos-into-the-folder)
3. [Getting a Permanent Reference ID](#3-getting-a-permanent-reference-id)
4. [Using the Permanent ID to Fetch a Photo](#4-using-the-permanent-id-to-fetch-a-photo)
5. [Updating a Stored Photo](#5-updating-a-stored-photo)
6. [Getting a List of All Photos in the Folder](#6-getting-a-list-of-all-photos-in-the-folder)

### 1. Making an App-Created Folder (Album)

Albums are the equivalent of folders in Google Photos. Create using `albums.create` endpoint.

**Required Scope**: `photoslibrary.appendonly` or `photoslibrary.edit.appcreateddata`

**HTTP Request**:
```bash
curl -X POST 'https://photoslibrary.googleapis.com/v1/albums' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "album": {
      "title": "Library Book Covers"
    }
  }'
```

**Response**:
```json
{
  "id": "ALBUM_ID_1234567890abcdef",
  "title": "Library Book Covers",
  "productUrl": "https://photos.google.com/albums/ALBUM_ID_1234567890abcdef",
  "isWriteable": true
}
```

**Key Points**:
- The API returns the Album object **directly** (not wrapped in an "album" field)
- The `description` field is **not supported** by the API (will cause 400 error)
- The `id` is the permanent identifier - store this in application configuration
- Album is visible to the user in their Google Photos but only manageable via API
- One album per library or per user depending on architecture choice

### 2. Putting Photos into the Folder

Two-step process: upload bytes, then create media item in album.

**Required Scope**: `photoslibrary.appendonly`

**Step 1: Upload Photo Bytes**
```bash
POST https://photoslibrary.googleapis.com/v1/uploads
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/octet-stream
X-Goog-Upload-Content-Type: image/jpeg

[RAW PHOTO BYTES]
```

**Response**: Upload token (plain text)
```
UPLOAD_TOKEN_abc123xyz
```

**Step 2: Create Media Item in Album**
```bash
curl -X POST 'https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "albumId": "ALBUM_ID_1234567890abcdef",
    "newMediaItems": [
      {
        "description": "Cover of The Great Gatsby by F. Scott Fitzgerald",
        "simpleMediaItem": {
          "uploadToken": "UPLOAD_TOKEN_abc123xyz"
        }
      }
    ]
  }'
```

**Response**:
```json
{
  "newMediaItemResults": [
    {
      "uploadToken": "UPLOAD_TOKEN_abc123xyz",
      "status": { "message": "Success" },
      "mediaItem": {
        "id": "MEDIA_ITEM_ID_987654321",
        "description": "Cover of The Great Gatsby by F. Scott Fitzgerald",
        "productUrl": "https://photos.google.com/lr/photo/MEDIA_ITEM_ID_987654321",
        "mimeType": "image/jpeg",
        "filename": "book_cover.jpg"
      }
    }
  ]
}
```

### 3. Getting a Permanent Reference ID

The permanent reference ID is `mediaItem.id` from the `batchCreate` response.

**Key Facts**:
- This ID is **globally unique** and **immutable**
- It never changes for the lifetime of the media item
- Store this in your `Photo` entity's `googlePhotosId` field
- Even if photo is edited/moved, the ID remains the same

**Example Storage Pattern**:
```java
Photo photo = new Photo();
photo.setGooglePhotosId("MEDIA_ITEM_ID_987654321");
photo.setGooglePhotosAlbumId("ALBUM_ID_1234567890abcdef");
photo.setBook(book);
photoRepository.save(photo);
```

### 4. Using the Permanent ID to Fetch a Photo

Retrieve photo metadata and access URLs using `mediaItems.get`.

**Required Scope**: `photoslibrary.readonly.appcreateddata`

**HTTP Request**:
```bash
curl -X GET 'https://photoslibrary.googleapis.com/v1/mediaItems/MEDIA_ITEM_ID_987654321' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**Response**:
```json
{
  "id": "MEDIA_ITEM_ID_987654321",
  "description": "Cover of The Great Gatsby by F. Scott Fitzgerald",
  "mimeType": "image/jpeg",
  "mediaMetadata": {
    "creationTime": "2025-11-14T10:30:00Z",
    "width": "1200",
    "height": "1800",
    "photo": {
      "cameraMake": "Apple",
      "cameraModel": "iPhone 15"
    }
  },
  "filename": "book_cover.jpg",
  "productUrl": "https://photos.google.com/lr/photo/MEDIA_ITEM_ID_987654321",
  "baseUrl": "https://lh3.googleusercontent.com/lr/MEDIA_ITEM_ID_987654321=d"
}
```

**Using baseUrl to Display Photos**:

The `baseUrl` requires size parameters:
- `=w800` - 800px width
- `=h600` - 600px height
- `=w2048-h1024` - 2048x1024 dimensions
- `=d` - Download original

**Example**: `https://lh3.googleusercontent.com/lr/MEDIA_ITEM_ID_987654321=w800`

**Important**: `baseUrl` expires after ~1 hour. Re-fetch as needed or cache.

### 5. Updating a Stored Photo

**Important Limitation**: You cannot directly edit pixel content. Photos are immutable.

#### Option A: Update Metadata Only

**Required Scope**: `photoslibrary.edit.appcreateddata`

```bash
curl -X POST 'https://photoslibrary.googleapis.com/v1/mediaItems:batchUpdate' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "mediaItemUpdates": [
      {
        "mediaItemId": "MEDIA_ITEM_ID_987654321",
        "updateMask": "description",
        "mediaItem": {
          "description": "Updated: The Great Gatsby - First Edition Cover"
        }
      }
    ]
  }'
```

**Result**: Same ID, updated metadata.

#### Option B: Replace Photo (New Upload)

To replace the image content:
1. Upload new bytes → get new upload token
2. Create new media item → **get NEW ID**
3. (Optional) Remove old item from album
4. Update your Photo entity with new ID

**Result**: Different ID for new version.

### 6. Getting a List of All Photos in the Folder

Use `mediaItems.search` with album ID.

**Required Scope**: `photoslibrary.readonly.appcreateddata`

**HTTP Request**:
```bash
curl -X POST 'https://photoslibrary.googleapis.com/v1/mediaItems:search' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "albumId": "ALBUM_ID_1234567890abcdef",
    "pageSize": 100
  }'
```

**Response**:
```json
{
  "mediaItems": [
    {
      "id": "MEDIA_ITEM_ID_987654321",
      "description": "Cover of The Great Gatsby",
      "filename": "book_cover.jpg",
      "baseUrl": "https://lh3.googleusercontent.com/lr/987654321=d"
    },
    {
      "id": "MEDIA_ITEM_ID_111222333",
      "description": "Cover of 1984",
      "filename": "1984_cover.jpg",
      "baseUrl": "https://lh3.googleusercontent.com/lr/111222333=d"
    }
  ],
  "nextPageToken": "CjMKG..."
}
```

**Pagination**: Use `pageToken` in request for next page.

## Spring Boot Implementation

### Required Dependencies (build.gradle)

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### Configuration (application.yml)

```yaml
google:
  photos:
    base-url: https://photoslibrary.googleapis.com/v1
    upload-url: https://photoslibrary.googleapis.com/v1/uploads
    picker-base-url: https://photospicker.googleapis.com/v1
    # Album ID for storing book covers (create once, configure here)
    book-covers-album-id: ${GOOGLE_PHOTOS_ALBUM_ID:}
```

### DTO Classes

```java
package com.muczynski.library.dto.googlephotos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// Album Creation
@Data @NoArgsConstructor
public class AlbumCreateRequest {
    private Album album;

    @Data @NoArgsConstructor
    public static class Album {
        private String title;
        // Note: description field is NOT supported by the API
    }
}

@Data @NoArgsConstructor
public class AlbumResponse {
    private Album album;

    @Data @NoArgsConstructor
    public static class Album {
        private String id;
        private String title;
        private String productUrl;
    }
}

// Media Item Creation
@Data @NoArgsConstructor
public class BatchCreateRequest {
    private String albumId;
    private List<NewMediaItem> newMediaItems;

    @Data @NoArgsConstructor
    public static class NewMediaItem {
        private String description;
        private SimpleMediaItem simpleMediaItem;
    }

    @Data @NoArgsConstructor
    public static class SimpleMediaItem {
        private String uploadToken;
    }
}

@Data @NoArgsConstructor
public class BatchCreateResponse {
    private List<NewMediaItemResult> newMediaItemResults;

    @Data @NoArgsConstructor
    public static class NewMediaItemResult {
        private String uploadToken;
        private Status status;
        private MediaItem mediaItem;
    }

    @Data @NoArgsConstructor
    public static class Status {
        private String message;
    }

    @Data @NoArgsConstructor
    public static class MediaItem {
        private String id;
        private String description;
        private String filename;
        private String baseUrl;
    }
}

// Media Item Response
@Data @NoArgsConstructor
public class MediaItemResponse {
    private String id;
    private String description;
    private String filename;
    private String mimeType;
    private String baseUrl;
    private MediaMetadata mediaMetadata;

    @Data @NoArgsConstructor
    public static class MediaMetadata {
        private String creationTime;
        private String width;
        private String height;
    }
}

// Search
@Data @NoArgsConstructor
public class SearchRequest {
    private String albumId;
    private Integer pageSize;
    private String pageToken;
}

@Data @NoArgsConstructor
public class SearchResponse {
    private List<MediaItemResponse> mediaItems;
    private String nextPageToken;
}
```

### Service Implementation

```java
package com.muczynski.library.service;

import com.muczynski.library.dto.googlephotos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePhotosLibraryService {

    private final RestTemplate restTemplate;

    @Value("${google.photos.base-url}")
    private String baseUrl;

    @Value("${google.photos.upload-url}")
    private String uploadUrl;

    /**
     * Create an album for storing book covers
     */
    public String createAlbum(String accessToken, String title) {
        log.info("Creating Google Photos album: {}", title);

        AlbumCreateRequest request = new AlbumCreateRequest();
        AlbumCreateRequest.Album album = new AlbumCreateRequest.Album();
        album.setTitle(title);
        request.setAlbum(album);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AlbumCreateRequest> entity = new HttpEntity<>(request, headers);

        // Google Photos API returns the Album object directly, not wrapped
        ResponseEntity<AlbumResponse.Album> response = restTemplate.postForEntity(
            baseUrl + "/albums",
            entity,
            AlbumResponse.Album.class
        );

        String albumId = response.getBody().getId();
        log.info("Created album with ID: {}", albumId);
        return albumId;
    }

    /**
     * Upload photo bytes and add to album
     * @return The permanent media item ID
     */
    public String uploadPhoto(String accessToken, String albumId, byte[] photoBytes, String description) {
        log.info("Uploading photo to album: {}", albumId);

        // Step 1: Upload bytes
        String uploadToken = uploadBytes(accessToken, photoBytes, "image/jpeg");

        // Step 2: Create media item
        BatchCreateRequest request = new BatchCreateRequest();
        request.setAlbumId(albumId);

        BatchCreateRequest.NewMediaItem item = new BatchCreateRequest.NewMediaItem();
        item.setDescription(description);

        BatchCreateRequest.SimpleMediaItem simpleItem = new BatchCreateRequest.SimpleMediaItem();
        simpleItem.setUploadToken(uploadToken);
        item.setSimpleMediaItem(simpleItem);

        request.setNewMediaItems(List.of(item));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BatchCreateRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BatchCreateResponse> response = restTemplate.postForEntity(
            baseUrl + "/mediaItems:batchCreate",
            entity,
            BatchCreateResponse.class
        );

        String mediaItemId = response.getBody()
            .getNewMediaItemResults()
            .get(0)
            .getMediaItem()
            .getId();

        log.info("Uploaded photo with ID: {}", mediaItemId);
        return mediaItemId;
    }

    /**
     * Upload photo bytes and get upload token
     */
    private String uploadBytes(String accessToken, byte[] photoBytes, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("X-Goog-Upload-Content-Type", mimeType);

        HttpEntity<byte[]> entity = new HttpEntity<>(photoBytes, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            uploadUrl,
            entity,
            String.class
        );

        return response.getBody(); // Upload token
    }

    /**
     * Get media item by permanent ID
     */
    public MediaItemResponse getMediaItem(String accessToken, String mediaItemId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<MediaItemResponse> response = restTemplate.exchange(
            baseUrl + "/mediaItems/" + mediaItemId,
            HttpMethod.GET,
            entity,
            MediaItemResponse.class
        );

        return response.getBody();
    }

    /**
     * List all photos in an album with pagination
     */
    public List<MediaItemResponse> listAllInAlbum(String accessToken, String albumId) {
        log.info("Listing all photos in album: {}", albumId);

        List<MediaItemResponse> allItems = new ArrayList<>();
        String pageToken = null;

        do {
            SearchRequest request = new SearchRequest();
            request.setAlbumId(albumId);
            request.setPageSize(100);
            request.setPageToken(pageToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SearchRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<SearchResponse> response = restTemplate.postForEntity(
                baseUrl + "/mediaItems:search",
                entity,
                SearchResponse.class
            );

            SearchResponse body = response.getBody();
            if (body.getMediaItems() != null) {
                allItems.addAll(body.getMediaItems());
            }
            pageToken = body.getNextPageToken();

        } while (pageToken != null);

        log.info("Found {} total photos in album", allItems.size());
        return allItems;
    }
}
```

## Database Schema Updates

### Photo Entity Enhancement

```java
@Entity
@Getter
@Setter
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Existing fields
    @Lob
    @Column(length = Integer.MAX_VALUE)
    private byte[] image;

    private String contentType;
    private String caption;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

    private Integer photoOrder;

    // NEW: Google Photos integration fields
    @Column(name = "google_photos_id")
    private String googlePhotosId;  // Permanent media item ID

    @Column(name = "google_photos_album_id")
    private String googlePhotosAlbumId;  // Album containing this photo

    @Column(name = "google_photos_base_url")
    private String googlePhotosBaseUrl;  // Last known baseUrl (expires hourly)

    @Column(name = "google_photos_last_sync")
    private Instant googlePhotosLastSync;  // Last time we synced with Google Photos

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_location")
    private StorageLocation storageLocation = StorageLocation.LOCAL;
}

public enum StorageLocation {
    LOCAL,           // Stored in our database
    GOOGLE_PHOTOS,   // Stored in Google Photos (app-created)
    BOTH             // Stored in both locations
}
```

### Migration SQL

```sql
ALTER TABLE photo
ADD COLUMN google_photos_id VARCHAR(255),
ADD COLUMN google_photos_album_id VARCHAR(255),
ADD COLUMN google_photos_base_url VARCHAR(1000),
ADD COLUMN google_photos_last_sync TIMESTAMP,
ADD COLUMN storage_location VARCHAR(20) DEFAULT 'LOCAL';

CREATE INDEX idx_photo_google_id ON photo(google_photos_id);
```

## Integration Workflow

### Uploading a Book Cover Photo

```java
@Service
@RequiredArgsConstructor
public class BookPhotoUploadService {

    private final GooglePhotosLibraryService googlePhotosService;
    private final GooglePhotosService existingService; // For token management
    private final PhotoRepository photoRepository;

    @Value("${google.photos.book-covers-album-id}")
    private String bookCoversAlbumId;

    public Photo uploadBookCover(Book book, byte[] photoBytes, String username) {
        // Get valid access token
        String accessToken = existingService.getValidAccessToken(username);

        // Build description
        String description = String.format("Cover of '%s' by %s",
            book.getTitle(),
            book.getAuthor().getName()
        );

        // Upload to Google Photos
        String mediaItemId = googlePhotosService.uploadPhoto(
            accessToken,
            bookCoversAlbumId,
            photoBytes,
            description
        );

        // Create Photo entity
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setContentType("image/jpeg");
        photo.setCaption(description);
        photo.setGooglePhotosId(mediaItemId);
        photo.setGooglePhotosAlbumId(bookCoversAlbumId);
        photo.setStorageLocation(StorageLocation.GOOGLE_PHOTOS);
        photo.setGooglePhotosLastSync(Instant.now());

        // Optionally store locally as backup
        photo.setImage(photoBytes);
        photo.setStorageLocation(StorageLocation.BOTH);

        return photoRepository.save(photo);
    }

    public byte[] downloadBookCover(Photo photo, String username) {
        // If stored locally, return immediately
        if (photo.getImage() != null) {
            return photo.getImage();
        }

        // Otherwise fetch from Google Photos
        if (photo.getGooglePhotosId() != null) {
            String accessToken = existingService.getValidAccessToken(username);
            MediaItemResponse item = googlePhotosService.getMediaItem(
                accessToken,
                photo.getGooglePhotosId()
            );

            // Download using baseUrl
            return existingService.downloadPhoto(item.getBaseUrl());
        }

        throw new RuntimeException("Photo not found in any storage location");
    }
}
```

## Security Considerations

### OAuth Scope Management

**Recommended Scopes for Library Application**:
```
https://www.googleapis.com/auth/photoslibrary.appendonly
https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata
https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata
https://www.googleapis.com/auth/photospicker.mediaitems.readonly
```

### Update OAuth Configuration

In your Google Cloud Console:
1. Navigate to **APIs & Services > Credentials**
2. Edit OAuth 2.0 Client ID
3. Update **Scopes** to match above list
4. Remove deprecated scopes

### Token Security

- Store access tokens encrypted in database
- Implement automatic token refresh (already in `GooglePhotosService`)
- Set token expiry with 5-minute buffer for refresh
- Never log full access tokens

## Testing Strategy

### Unit Tests

```java
@SpringBootTest
class GooglePhotosLibraryServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private GooglePhotosLibraryService service;

    @Test
    void testCreateAlbum() {
        // Mock response - API returns Album object directly
        AlbumResponse.Album album = new AlbumResponse.Album();
        album.setId("test-album-id");
        album.setTitle("Test Album");

        when(restTemplate.postForEntity(anyString(), any(), eq(AlbumResponse.Album.class)))
            .thenReturn(ResponseEntity.ok(album));

        String albumId = service.createAlbum("token", "Test Album");

        assertEquals("test-album-id", albumId);
    }

    @Test
    void testUploadPhoto() {
        // Test upload flow
        // Mock both upload bytes and batchCreate responses
    }
}
```

### Integration Tests

Use Google OAuth Playground to:
1. Generate test access tokens with new scopes
2. Manually test album creation
3. Upload test photos
4. Verify permanent IDs persist

## Monitoring & Observability

### Metrics to Track

- **Upload Success Rate**: Track successful vs. failed uploads
- **Token Refresh Rate**: Monitor how often tokens are refreshed
- **API Latency**: Measure response times for Google Photos API calls
- **Storage Costs**: Compare local storage vs. Google Photos reliance

### Logging Strategy

```java
log.info("Google Photos operation: action={}, mediaItemId={}, albumId={}, duration={}ms",
    action, mediaItemId, albumId, duration);
```

Log levels:
- **INFO**: Successful operations, token refreshes
- **WARN**: Approaching rate limits, token expiry warnings
- **ERROR**: API failures, authentication issues

## Migration Plan

### Phase 1: Preparation (Week 1)
- [ ] Update OAuth scopes in Google Cloud Console
- [ ] Add database columns for Google Photos integration
- [ ] Create DTOs and service layer
- [ ] Write unit tests

### Phase 2: Parallel Implementation (Week 2-3)
- [ ] Implement new upload flow alongside existing
- [ ] Test with sample books
- [ ] Verify permanent IDs stored correctly
- [ ] Monitor for 403 errors from deprecated scopes

### Phase 3: Migration (Week 4)
- [ ] Update UI to use new upload flow
- [ ] Deprecate old search functionality
- [ ] Add Picker API for user photo selection
- [ ] Update documentation

### Phase 4: Cleanup (Week 5)
- [ ] Remove deprecated scope references
- [ ] Archive old GooglePhotosService methods
- [ ] Performance optimization
- [ ] User communication about changes

## Cost Analysis

### Google Photos API Quotas

| Operation | Quota | Notes |
|-----------|-------|-------|
| Requests per day | 10,000 | Default quota |
| Upload bandwidth | 75 MB per user per day | Per-user limit |
| Storage | Unlimited | Uses user's Google Photos storage |

### Recommendations

- **Cache baseUrls** for up to 50 minutes (10-minute buffer before expiry)
- **Batch operations** when possible (batchCreate supports up to 50 items)
- **Store locally** for frequently accessed photos (book covers)
- **Use Google Photos** for archival/backup storage

## Troubleshooting

### Common Issues

#### 403 Forbidden Error
**Cause**: Using deprecated scopes or requesting access to non-app-created content
**Solution**:
1. Verify scopes in OAuth configuration
2. Check token scopes using tokeninfo endpoint
3. Ensure only requesting app-created media items

#### 401 Unauthorized Error
**Cause**: Expired or invalid access token
**Solution**:
1. Check token expiry timestamp
2. Trigger token refresh
3. Verify refresh token still valid

#### Empty baseUrl or Null Response
**Cause**: Media item ID invalid or not app-created
**Solution**:
1. Verify media item ID stored correctly
2. Confirm item was created by your app
3. Check if item was deleted from user's library

## References

- [Google Photos Library API Reference](https://developers.google.com/photos/library/reference/rest)
- [2025 API Updates](https://developers.google.com/photos/library/guides/about-updates)
- [OAuth Authorization Guide](https://developers.google.com/photos/library/guides/authorization)
- [Picker API Documentation](https://developers.google.com/photos/picker/reference/rest)

## Appendix: Complete Java Example

See the Spring Boot implementation examples in the main content above. For a complete working example, refer to:
- `GooglePhotosLibraryService.java` - Core API client
- `BookPhotoUploadService.java` - Integration with library domain
- DTOs in `com.muczynski.library.dto.googlephotos` package

---

**Document Version**: 1.0
**Last Updated**: 2025-11-14
**Author**: Library Development Team
**Status**: Draft for Review
