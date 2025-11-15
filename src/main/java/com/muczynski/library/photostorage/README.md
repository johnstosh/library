# Google Photos Storage Package

This package provides a complete implementation of the Google Photos Library API 2025 specification for managing app-created content.

## Package Structure

```
com.muczynski.library.photostorage/
├── client/
│   └── GooglePhotosLibraryClient.java    # Low-level REST API client
├── config/
│   └── PhotoStorageConfig.java           # Configuration properties
├── dto/
│   ├── AlbumCreateRequest.java           # Album creation request
│   ├── AlbumResponse.java                # Album response
│   ├── BatchCreateRequest.java           # Media item batch create request
│   ├── BatchCreateResponse.java          # Media item batch create response
│   ├── BatchUpdateRequest.java           # Metadata update request
│   ├── BatchUpdateResponse.java          # Metadata update response
│   ├── MediaItemResponse.java            # Media item details
│   ├── SearchRequest.java                # Search/list request
│   └── SearchResponse.java               # Search/list response
└── service/
    └── GooglePhotosStorageService.java   # High-level business logic service
```

## OAuth Scopes Required (2025)

Add these scopes to your OAuth configuration:

```
https://www.googleapis.com/auth/photoslibrary.appendonly
https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata
https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata
```

**Important**: The old scopes (`photoslibrary.readonly`, `photoslibrary.sharing`, `photoslibrary`) were removed on March 31, 2025 and will cause 403 errors.

## Configuration

Add to `application.yml`:

```yaml
google:
  photos:
    base-url: https://photoslibrary.googleapis.com/v1
    upload-url: https://photoslibrary.googleapis.com/v1/uploads
    picker-base-url: https://photospicker.googleapis.com/v1
    book-covers-album-id: ${GOOGLE_PHOTOS_BOOK_COVERS_ALBUM_ID:}
    author-photos-album-id: ${GOOGLE_PHOTOS_AUTHOR_PHOTOS_ALBUM_ID:}
    default-page-size: 100
    cache-base-urls: true
    base-url-cache-duration-minutes: 50
```

## Usage Examples

### 1. Create an Album (One-time Setup)

```java
@Autowired
private GooglePhotosStorageService storageService;

// Run once during initial setup
String albumId = storageService.initializeBookCoversAlbum(accessToken);
// Output: Album ID to configure in application.yml
```

### 2. Upload a Photo

```java
// Upload from bytes
String mediaItemId = storageService.uploadPhotoToAlbum(
    accessToken,
    albumId,
    photoBytes,
    "image/jpeg",
    "Cover of The Great Gatsby by F. Scott Fitzgerald"
);

// Upload from file
String mediaItemId = storageService.uploadPhotoFromFile(
    accessToken,
    albumId,
    Paths.get("/path/to/photo.jpg"),
    "Description here"
);

// Store this mediaItemId in your Photo entity!
```

### 3. Fetch a Photo by ID

```java
// Get metadata
MediaItemResponse photo = storageService.getPhotoById(accessToken, mediaItemId);
System.out.println("Photo URL: " + photo.getBaseUrl() + "=w800");

// Download full resolution
byte[] photoBytes = storageService.downloadPhotoById(accessToken, mediaItemId);

// Get sized URL
String thumbnailUrl = storageService.getSizedPhotoUrl(accessToken, mediaItemId, 300, 200);
```

### 4. Update Photo Metadata

```java
// Update description only
storageService.updatePhotoDescription(
    accessToken,
    mediaItemId,
    "Updated: The Great Gatsby - First Edition"
);

// Note: ID remains the same!
```

### 5. Replace a Photo (New ID)

```java
// Upload new version (gets NEW ID)
String newMediaItemId = storageService.replacePhoto(
    accessToken,
    albumId,
    newPhotoBytes,
    "image/jpeg",
    "New version description"
);

// IMPORTANT: Update your database with the NEW ID!
```

### 6. List All Photos in Album

```java
// Get all photos (handles pagination automatically)
List<MediaItemResponse> allPhotos = storageService.listAllPhotosInAlbum(
    accessToken,
    albumId
);

for (MediaItemResponse photo : allPhotos) {
    System.out.println(photo.getId() + " → " + photo.getFilename());
}

// Or process with callback (memory efficient for large albums)
storageService.listPhotosWithCallback(accessToken, albumId, (photos, pageNumber) -> {
    System.out.println("Processing page " + pageNumber + ": " + photos.size() + " photos");
    // Process each page as it's fetched
});
```

## Integration with Existing Photo Entity

### Update Photo Entity

```java
@Entity
@Getter
@Setter
public class Photo {
    // Existing fields...

    // NEW: Google Photos integration
    @Column(name = "google_photos_id")
    private String googlePhotosId;  // Permanent media item ID from uploadPhotoToAlbum()

    @Column(name = "google_photos_album_id")
    private String googlePhotosAlbumId;

    @Column(name = "storage_location")
    @Enumerated(EnumType.STRING)
    private StorageLocation storageLocation = StorageLocation.LOCAL;
}

public enum StorageLocation {
    LOCAL,           // Stored in database only
    GOOGLE_PHOTOS,   // Stored in Google Photos only
    BOTH             // Stored in both (recommended for important photos)
}
```

### Service Integration Example

```java
@Service
@RequiredArgsConstructor
public class BookPhotoUploadService {

    private final GooglePhotosStorageService googlePhotosStorage;
    private final GooglePhotosService existingService; // For token management
    private final PhotoRepository photoRepository;

    public Photo uploadBookCover(Book book, byte[] photoBytes, String username) {
        // Get valid access token (uses existing token refresh logic)
        String accessToken = existingService.getValidAccessToken(username);

        // Get album ID from config
        String albumId = googlePhotosStorage.getBookCoversAlbumId();

        // Upload to Google Photos
        String mediaItemId = googlePhotosStorage.uploadPhotoToAlbum(
            accessToken,
            albumId,
            photoBytes,
            "image/jpeg",
            String.format("Cover of '%s' by %s", book.getTitle(), book.getAuthor().getName())
        );

        // Create Photo entity
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setContentType("image/jpeg");
        photo.setGooglePhotosId(mediaItemId);
        photo.setGooglePhotosAlbumId(albumId);
        photo.setStorageLocation(StorageLocation.BOTH);
        photo.setImage(photoBytes); // Store locally as backup

        return photoRepository.save(photo);
    }

    public byte[] getBookCover(Photo photo, String username) {
        // Try local first
        if (photo.getImage() != null) {
            return photo.getImage();
        }

        // Fall back to Google Photos
        if (photo.getGooglePhotosId() != null) {
            String accessToken = existingService.getValidAccessToken(username);
            return googlePhotosStorage.downloadPhotoById(accessToken, photo.getGooglePhotosId());
        }

        throw new RuntimeException("Photo not found");
    }
}
```

## Important Notes

### Permanent IDs Never Change

The `mediaItem.id` returned from `uploadPhotoToAlbum()` is **permanent** and **immutable**. It never changes, even if:
- Photo metadata is updated
- Photo is moved between albums
- Photo filename changes

### Replacing Photos Creates New IDs

To "update" the actual image content, you must:
1. Upload a new photo → **get NEW ID**
2. Update your database with the NEW ID
3. Optionally keep or delete the old photo

### baseUrl Expiration

The `baseUrl` field in `MediaItemResponse` expires after ~1 hour. Either:
- Re-fetch when needed (recommended)
- Cache for max 50 minutes
- Store locally if frequently accessed

### App-Created Content Only

This API can **only** access photos uploaded by your app. You cannot:
- Read the user's entire photo library
- Search for photos not created by your app
- Access photos from other apps

For user-selected photos, use the **Google Photos Picker API** instead.

## Error Handling

```java
try {
    String mediaItemId = storageService.uploadPhotoToAlbum(...);
} catch (HttpClientErrorException e) {
    if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
        // 403: Check OAuth scopes or API enabled in Google Cloud Console
    } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        // 401: Access token expired, refresh it
    }
}
```

## Testing

Use Google OAuth Playground (https://developers.google.com/oauthplayground/) to:
1. Generate test access tokens with the new 2025 scopes
2. Manually test album creation
3. Test photo upload workflow
4. Verify permanent IDs persist correctly

## See Also

- [photos-design.md](../../../../../photos-design.md) - Complete design document
- [Google Photos Library API Reference](https://developers.google.com/photos/library/reference/rest)
- [2025 API Updates](https://developers.google.com/photos/library/guides/about-updates)
