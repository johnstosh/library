# Photo Storage & Google Photos Integration

## Overview
The application uses Google Photos API for storing and managing book and author photos.

## Architecture

### Photo Entity
- Stores photo metadata in PostgreSQL database
- Binary image data stored as `@Lob` (Large Object)
- Links to Google Photos via `permanentId` and media item URLs
- Supports both book photos and author photos (polymorphic relationship)
- Cascade delete: deleting a book/author deletes associated photos

### Google Photos Service
- `GooglePhotosService` handles uploads, downloads, and album management
- OAuth2 flow for authorization
- Batch operations via Google Photos Library API
- Photo export functionality for backup to Google Photos

## Features

### Photo Upload
- Upload photos to books and authors
- Store both in database and optionally in Google Photos
- Support for multiple photos per book/author
- Photo ordering (photoOrder field)

### Photo Import from Feed
- Import books from Google Photos feed
- Associate photos with books during import
- Download photos from Google Photos and store locally

### Photo Manipulation
- **Cropping**: Browser-based cropping using Cropper.js
  - Implemented in `photo-crop.js`
- **Rotation**: Server-side rotation (clockwise/counter-clockwise)
  - Endpoints: `/api/books/{bookId}/photos/{photoId}/rotate-cw` and `rotate-ccw`

### Photo Management
- Reorder photos (move left/right)
- Delete photos
- Update photo captions
- View photo thumbnails in lists

## Caching

### Thumbnail Cache
- Browser-based thumbnail caching using IndexedDB
- Cache key: photo ID + checksum
- Reduces bandwidth and improves performance
- Implemented in `thumbnail-cache.js`

### Book Cache
- Browser-based book cache using IndexedDB
- Caches book data including first photo ID and checksum
- Compares `lastModified` timestamps to detect changes
- Implemented in `book-cache.js`

## Export/Import

### Photo Export (Backup)
- Separate from JSON database export
- Endpoint: `/api/photo-export/**`
- Backs up photos to Google Photos
- Photos contain large binary data (too big for JSON export)

### JSON Export Exclusion
- **IMPORTANT**: Photos are NOT included in JSON database export
- Photo binary data would make response too large
- Use dedicated Photo Export feature instead
- See `feature-design-import-export.md` for details

## API Endpoints

### Book Photos
- `POST /api/books/{bookId}/photos` - Upload photo
- `POST /api/books/{bookId}/photos/from-google-photos` - Add photos from Google Photos
- `GET /api/books/{bookId}/photos` - List photos for book
- `PUT /api/books/{bookId}/photos/{photoId}` - Update photo metadata
- `DELETE /api/books/{bookId}/photos/{photoId}` - Delete photo
- `PUT /api/books/{bookId}/photos/{photoId}/rotate-cw` - Rotate clockwise
- `PUT /api/books/{bookId}/photos/{photoId}/rotate-ccw` - Rotate counter-clockwise
- `PUT /api/books/{bookId}/photos/{photoId}/move-left` - Move photo left in order
- `PUT /api/books/{bookId}/photos/{photoId}/move-right` - Move photo right in order

### Author Photos
- Similar endpoints under `/api/authors/{authorId}/photos/**`

## Frontend Implementation

### JavaScript Modules
- `js/books-photo.js` - Book photo management UI
- `js/authors-photo.js` - Author photo management UI
- `js/books-from-feed.js` - Import books from Google Photos feed
- `js/photos.js` - Google Photos integration utilities
- `js/thumbnail-cache.js` - Thumbnail caching
- `js/photo-crop.js` - Photo cropping utilities

## Related Files
- `Photo.java` - Photo entity with `@Lob` fields
- `PhotoService.java` - Photo business logic
- `GooglePhotosService.java` - Google Photos API integration
- `PhotoController.java` - Photo REST API
- `photos-design.md` - Detailed photo storage design document
