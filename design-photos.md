# Photo System Design

This document describes how the photo system works and the requirements for entities associated with photos.

## Overview

Photos are stored in the database as binary data (LOB) with optional backup to Google Photos. Each photo can belong to a book, author, or loan (checkout card photos).

## Database Storage

### Photo Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key (auto-generated) |
| `image` | byte[] | Binary image data (LAZY loaded) |
| `contentType` | String | MIME type (e.g., image/jpeg) |
| `caption` | String | Optional description |
| `photoOrder` | Integer | Ordering within gallery |
| `imageChecksum` | String | SHA-256 hash for change tracking |
| `dateTaken` | LocalDateTime | Original photo timestamp |
| `deletedAt` | LocalDateTime | Soft delete timestamp |

### Google Photos Integration Fields

| Field | Type | Description |
|-------|------|-------------|
| `permanentId` | String | Google Photos media item ID |
| `exportedAt` | LocalDateTime | When exported to Google Photos |
| `exportStatus` | Enum | PENDING, IN_PROGRESS, COMPLETED, FAILED |
| `exportErrorMessage` | String | Error message if export failed |

### Relationships (Polymorphic)

- `@ManyToOne book` - Foreign key to Book (nullable)
- `@ManyToOne author` - Foreign key to Author (nullable)
- `@ManyToOne loan` - Foreign key to Loan (nullable)

Each photo belongs to exactly one entity. Cascade delete is enabled - deleting a book/author automatically deletes associated photos.

## Photo Operations

### Upload
- **Local file upload**: POST to `/api/books/{id}/photos` or `/api/authors/{id}/photos`
- **Google Photos import**: POST to `/api/books/{id}/photos/from-google-photos`
- Photos automatically assigned `photoOrder` based on max existing order + 1
- Checksum computed on upload for change tracking

### Manipulation
- **Rotate**: Server-side 90° rotation using AffineTransform with proper image type handling
  - Uses TYPE_INT_RGB for JPEG (no alpha channel support)
  - Uses TYPE_INT_ARGB for PNG and other formats
  - Fills JPEG backgrounds with white to avoid black artifacts
  - Applies rendering hints for better quality (bilinear interpolation, antialiasing)
- **Crop**: Creates new photo at original position, shifts original right
- **Reorder**: Move left/right operations with position swapping

### Deletion
- **Hard delete**: Used for book photos (`DELETE /api/books/{id}/photos/{photoId}`)
- **Soft delete**: Sets `deletedAt` timestamp (`DELETE /api/photos/{id}`)
- **Restore**: Clears `deletedAt` (`POST /api/photos/{id}/restore`)

### Thumbnail Generation
- Server-side scaling with aspect ratio preservation
- EXIF orientation correction applied automatically
- Different handling for JPEG (RGB) vs PNG (ARGB)
- Endpoint: `GET /api/photos/{id}/thumbnail?width={width}`

## API Endpoints

### Public Endpoints (No Auth Required)
```
GET /api/photos/{id}/image          - Full resolution image
GET /api/photos/{id}/thumbnail      - Thumbnail with width param
GET /api/books/{bookId}/photos      - List book photos
GET /api/authors/{authorId}/photos  - List author photos
```

### Librarian-Only Endpoints
```
POST   /api/books/{id}/photos                     - Upload photo
POST   /api/books/{id}/photos/from-google-photos  - Import from Google
PUT    /api/books/{id}/photos/{photoId}/rotate-cw - Rotate clockwise
PUT    /api/books/{id}/photos/{photoId}/rotate-ccw - Rotate counter-clockwise
PUT    /api/books/{id}/photos/{photoId}/move-left - Reorder left
PUT    /api/books/{id}/photos/{photoId}/move-right - Reorder right
DELETE /api/books/{id}/photos/{photoId}           - Delete photo
PUT    /api/photos/{id}/crop                      - Crop photo
```

## Frontend Components

### PhotoSection
Main component for photo management on book/author pages.
```tsx
<PhotoSection
  entityType="book" | "author"
  entityId={id}
  entityName={name}
/>
```

### PhotoGallery
Grid display with controls for librarians:
- Thumbnail grid (2-4 columns responsive)
- Action buttons on hover (delete, rotate, move)
- Lightbox modal for full viewing
- Click to open in new tab

### PhotoUploadModal
Upload dialog with:
- File validation (image/* only, max 10MB)
- Client-side cropping via react-cropper
- Rotation controls
- Preview before upload

### PhotoViewPage
Public photo viewer at `/photos/:id`:
- Full resolution display
- Error handling for missing photos
- No authentication required

## Caching

### Frontend (TanStack Query)
```typescript
queryKeys.photos = {
  all: ['photos'],
  book: (bookId) => ['photos', 'book', bookId],
  author: (authorId) => ['photos', 'author', authorId],
  image: (id, checksum) => ['photos', 'image', id, checksum],
  thumbnail: (id, checksum, width) => ['photos', 'thumbnail', id, checksum, width],
}
```

- **Stale time**: 5 minutes
- **GC time**: 30 minutes
- Cache invalidated on upload/delete/modify operations

### Backend
- LAZY loading for image bytes prevents unnecessary memory usage
- Projection queries (`PhotoMetadataProjection`) exclude image data
- Checksums enable efficient change detection

## Google Photos Export/Import

### Export Flow
1. Photos with checksum but no `permanentId` are candidates for export
2. Batch processing (up to 50 photos per batch, Google API limit)
3. Upload to Google Photos, receive `permanentId`
4. Mark as COMPLETED with timestamp

### Import Flow
1. Photos with `permanentId` but missing local data need import
2. Batch download (20 photos at a time)
3. Store image bytes in database
4. Compute checksum for verification

**Note**: Photos are NOT included in JSON database export. Use Photo Export for backup.

## ZIP Import

The system supports bulk importing photos from a ZIP file. Photos are automatically matched to entities based on filename patterns.

### Filename Format

```
{type}-{name}[-{n}].{ext}
```

- **type**: `book`, `author`, or `loan`
- **name**: Entity identifier (sanitized - lowercase, special chars replaced with dashes)
- **n** (optional): Number suffix for multiple photos of same entity
- **ext**: Image extension (jpg, jpeg, png, gif, webp)

### Examples

| Filename | Description |
|----------|-------------|
| `book-the-great-gatsby.jpg` | Photo for book titled "The Great Gatsby" |
| `book-war-and-peace-2.png` | Second photo for "War and Peace" |
| `author-mark-twain.jpg` | Photo for author "Mark Twain" |
| `loan-moby-dick-johnsmith.jpg` | Checkout card for "Moby Dick" borrowed by user "johnsmith" |

### Matching Algorithm

1. **Books**: Title is searched case-insensitively with partial match, then exact sanitized match
2. **Authors**: Name is searched case-insensitively with partial match, then exact sanitized match
3. **Loans**: Book title + username are matched against existing loans

### API Endpoint

```
POST /api/photos/import-zip  (Librarian only)
```

Request: `multipart/form-data` with `file` parameter containing ZIP archive

Response:
```json
{
  "totalFiles": 10,
  "successCount": 7,
  "failureCount": 2,
  "skippedCount": 1,
  "items": [
    {
      "filename": "book-moby-dick.jpg",
      "status": "SUCCESS",
      "entityType": "book",
      "entityName": "Moby Dick",
      "entityId": 42,
      "photoId": 123
    },
    {
      "filename": "readme.txt",
      "status": "SKIPPED",
      "errorMessage": "Filename format not recognized..."
    }
  ]
}
```

### Frontend Integration

Access via **Data Management** page (`/data-management`):
- Upload button in Photo Export/Import section
- Results table shows status of each file
- Success/failure messages with counts

## Requirements for Participating Entities

For an entity type to support photos:

### Backend Requirements

1. **Entity Relationship**: Add `@OneToMany` relationship with cascade delete:
```java
@OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Photo> photos = new ArrayList<>();
```

2. **Controller Endpoints**: Add photo management endpoints

3. **Service Methods**: Implement photo operations in PhotoService or entity service

### Frontend Requirements

1. **API Hooks**: Add TanStack Query hooks for photo operations
2. **PhotoSection**: Include in view/edit pages
3. **Query Key**: Add to queryKeys configuration

## Memory Efficiency

- **Lazy Loading**: Image bytes loaded only when accessed
- **Projection Queries**: List operations exclude image data
- **Batch Processing**: Large operations processed in chunks
- **Checksum Migration**: One photo at a time to prevent OOM

## Security

- **Public**: Image/thumbnail retrieval, photo lists
- **Librarian**: All modification operations
- **Google OAuth**: Per-user token management for Google Photos integration
