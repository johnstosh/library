# Photo Storage & Google Photos Integration

## Overview
The application uses Google Photos API for storing and managing book and author photos. Photos are stored both locally in PostgreSQL (as binary LOBs) and optionally in Google Photos for backup and sharing.

## Architecture

### Photo Entity
**Database Storage:**
- Binary image data stored as `@Lob` (Large Object) in PostgreSQL
- Metadata stored in `photos` table with following key fields:
  - `id` - Primary key
  - `content_type` - MIME type (e.g., image/jpeg, image/png)
  - `caption` - Optional photo description
  - `book_id` - Foreign key to books table (nullable)
  - `author_id` - Foreign key to authors table (nullable)
  - `image_checksum` - MD5 hash for change detection
  - `date_taken` - Timestamp when photo was taken
  - `deleted` - Soft delete flag
  - `display_order` - Photo ordering within book/author
  - `permanent_id` - Google Photos media item ID

**Relationships:**
- Polymorphic: Each photo belongs to either a book OR an author
- Cascade delete: Deleting a book/author deletes associated photos
- Multiple photos per book/author supported
- Photos ordered by `display_order` field (ascending)

### Google Photos Service
- `GooglePhotosService` handles OAuth2 flow, uploads, and downloads
- Integrates with Google Photos Library API
- Manages access token refresh automatically
- Downloads photos from Google Photos using media item IDs
- Supports batch photo import from Google Photos Picker

### Photo Service
- `PhotoService` contains core business logic:
  - Photo upload (from file or Google Photos)
  - Thumbnail generation (server-side resizing)
  - Photo ordering (move left/right)
  - Soft delete and restore
  - Checksum computation
  - Photo cropping/replacement

## Features

### Photo Upload

**Local Upload:**
- Upload photos from local files via multipart form data
- Supported formats: JPEG, PNG, GIF
- Server stores original image in database
- Generates checksum for change detection
- Assigns display order automatically

**Google Photos Import:**
- Add photos directly from Google Photos library
- Downloads photo bytes from Google Photos
- Stores `permanentId` for future reference
- Links photo to book or author
- Batch import supported with error handling

### Photo Manipulation

**Rotation:**
- Server-side rotation in 90° increments
- Clockwise and counter-clockwise
- Rotation stored in `rotation_degrees` field
- Applied on image retrieval

**Cropping:**
- Replace photo with cropped version
- Endpoint: `PUT /api/photos/{id}/crop`
- Accepts multipart file upload
- Preserves photo metadata and ordering

**Ordering:**
- Reorder photos within book/author gallery
- Move left: decreases display order
- Move right: increases display order
- Other photos automatically reordered

### Photo Management

**Soft Delete:**
- Photos marked as deleted but not removed from database
- `deleted` flag set to true
- Can be restored later
- Endpoint: `DELETE /api/photos/{id}`

**Restore:**
- Undelete soft-deleted photos
- Sets `deleted` flag to false
- Endpoint: `POST /api/photos/{id}/restore`

**Hard Delete:**
- Permanent deletion from database
- Used via book/author photo endpoints
- Cannot be undone

**Caption Editing:**
- Update photo caption via `PUT /api/books/{bookId}/photos/{photoId}`
- Caption stored in database
- Displayed in photo galleries

### Photo Access

**Full Image:**
- Endpoint: `GET /api/photos/{id}/image`
- Returns full-resolution image bytes
- Content-Type header set to photo's MIME type
- Public access (no authentication required)

**Thumbnails:**
- Endpoint: `GET /api/photos/{id}/thumbnail?width={width}`
- Server-side thumbnail generation
- Maintains aspect ratio
- Configurable width parameter
- Public access (no authentication required)

## Caching Strategy

### TanStack Query Caching (Current Implementation)
- Frontend uses TanStack Query v5 for server state management
- Automatic caching and cache invalidation
- Cache keys based on query parameters
- Configurable stale time and cache time
- **Note:** Original IndexedDB caching design was replaced with TanStack Query

### Cache Invalidation
- Mutations invalidate related queries
- Photo changes trigger refetch of book/author data
- `lastModified` timestamps used for change detection
- Photos include checksum for versioning

## Export/Import

### Photo Export (Google Photos Backup)
- Separate from JSON database export
- Endpoints: `/api/photo-export/**`
- Uploads all photos to Google Photos cloud storage
- Updates `permanentId` for each uploaded photo
- See `feature-design-import-export.md` for details

### Photo Import (Google Photos Sync)
- Downloads photos from Google Photos using `permanentId`
- Restores photo bytes to local database
- Used after database restore from JSON backup
- Individual or batch import supported

### JSON Export Exclusion
- **IMPORTANT**: Photo binary data NOT included in JSON export
- Photo metadata (ID, caption, permanentId) included in JSON
- Photo bytes would make JSON response too large
- Use dedicated Photo Export feature for backing up images

## API Endpoints

### Book Photos
- `POST /api/books/{bookId}/photos` - Upload photo (librarian only)
- `POST /api/books/{bookId}/photos/from-google-photos` - Import from Google Photos (librarian only)
- `GET /api/books/{bookId}/photos` - List photos (public)
- `PUT /api/books/{bookId}/photos/{photoId}` - Update metadata (librarian only)
- `DELETE /api/books/{bookId}/photos/{photoId}` - Delete photo (librarian only)
- `PUT /api/books/{bookId}/photos/{photoId}/rotate-cw` - Rotate clockwise (librarian only)
- `PUT /api/books/{bookId}/photos/{photoId}/rotate-ccw` - Rotate counter-clockwise (librarian only)
- `PUT /api/books/{bookId}/photos/{photoId}/move-left` - Reorder left (librarian only)
- `PUT /api/books/{bookId}/photos/{photoId}/move-right` - Reorder right (librarian only)

### Author Photos
- `POST /api/authors/{authorId}/photos` - Upload photo (librarian only)
- `POST /api/authors/{authorId}/photos/from-google-photos` - Import from Google Photos (librarian only)
- `GET /api/authors/{authorId}/photos` - List photos (public)
- `DELETE /api/authors/{authorId}/photos/{photoId}` - Delete photo (librarian only)
- `PUT /api/authors/{authorId}/photos/{photoId}/rotate-cw` - Rotate clockwise (librarian only)
- `PUT /api/authors/{authorId}/photos/{photoId}/rotate-ccw` - Rotate counter-clockwise (librarian only)
- `PUT /api/authors/{authorId}/photos/{photoId}/move-left` - Reorder left (librarian only)
- `PUT /api/authors/{authorId}/photos/{photoId}/move-right` - Reorder right (librarian only)

### Photo Direct Access
- `GET /api/photos/{id}/image` - Get full image (public)
- `GET /api/photos/{id}/thumbnail?width={width}` - Get thumbnail (public)
- `DELETE /api/photos/{id}` - Soft delete (librarian only)
- `POST /api/photos/{id}/restore` - Restore deleted photo (librarian only)
- `PUT /api/photos/{id}/crop` - Replace with cropped version (librarian only)

See `endpoints.md` for complete API documentation including request/response formats.

## Frontend Implementation

### React Components (TypeScript)
- **PhotoGallery Component** - Displays photo grid for books/authors
- **PhotoUploadModal** - Handles photo upload and Google Photos import
- **PhotoSection** - Photo management section in book/author detail pages
- Photo components integrated into:
  - `frontend/src/pages/books/BookDetailModal.tsx`
  - `frontend/src/pages/authors/AuthorDetailModal.tsx`

### API Integration
- **TanStack Query hooks** for photo operations:
  - `usePhotos` - Fetch photos for book/author
  - `useUploadPhoto` - Upload photo mutation
  - `useDeletePhoto` - Delete photo mutation
  - `useRotatePhoto` - Rotate photo mutation
  - `useMovePhoto` - Reorder photo mutation

### Photo Features
- Google Photos Picker integration
- Photo cropping UI
- Photo rotation controls
- Photo ordering (drag and drop or buttons)
- Photo deletion with confirmation
- Caption editing

### Caching
- TanStack Query automatic caching
- Query invalidation on mutations
- Optimistic updates for better UX
- Stale-while-revalidate strategy

## Security

### Authentication & Authorization
- **Public endpoints**: Image and thumbnail access (no auth required)
- **Librarian-only endpoints**: Upload, edit, delete, rotate, reorder
- `@PreAuthorize("hasAuthority('LIBRARIAN')")` on write operations
- `@PreAuthorize("permitAll()")` on read operations

### Google Photos OAuth
- OAuth2 flow for user authorization
- Access token stored securely
- Token refresh handled automatically
- User must grant Google Photos permissions

### Google Photos OAuth Configuration

Librarians can configure Google Photos OAuth credentials through the Global Settings page (`/global-settings`).

**Configuration Fields:**
- **Client ID**: Editable text field for Google OAuth Client ID
- **Client Secret**: Password field for Google OAuth Client Secret (starts with "GOCSPX-")

**Priority Order (Fallback Logic):**
1. Database (`GlobalSettings` entity) - highest priority
2. Environment variable `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`
3. `application.properties` - lowest priority (default/fallback)

**How to Configure:**
1. Navigate to Global Settings (`/global-settings`) as a librarian
2. Scroll to "Google Photos API" section
3. Enter Client ID and/or Client Secret
4. Click "Update Settings"
5. Leave a field blank to keep its existing value

**Validation:**
- Client Secret should start with "GOCSPX-" prefix
- Minimum length of 20 characters
- Validation messages displayed below the form

**Note:** This is separate from Google SSO (user authentication), which has its own Client ID and Client Secret for login purposes.

## Performance Considerations

### Database
- Photo binary data stored as LOB (Large Object)
- Indexed by `book_id`, `author_id`, and `display_order`
- Soft delete allows recovery without re-upload
- Checksum enables change detection

### Thumbnails
- Generated on-demand with caching
- Reduces bandwidth for list views
- Width parameter for flexible sizing
- Server-side resizing using ImageIO
- **Image Type Handling:**
  - JPEG images use `BufferedImage.TYPE_INT_RGB` (no alpha channel)
  - PNG/other formats use `BufferedImage.TYPE_INT_ARGB` (with alpha channel)
  - Prevents black thumbnail generation for JPEG images
- **Error Handling:**
  - Returns JSON error responses on failure
  - Detailed logging for debugging thumbnail generation issues
  - Validates ImageIO write success

### Google Photos
- Batch operations reduce API calls
- Photo download cached in database
- `permanentId` enables re-download if needed

## Testing

### Unit Tests
- **PhotoServiceTest** - 7 tests covering upload, list, rotate, delete, restore, update
- Mocks for PhotoRepository, BookRepository, PhotoMapper
- Tests checksum computation and ordering logic

### Integration Tests
- **PhotoControllerTest** - Tests covering API endpoints
  - Tests authentication/authorization (public vs librarian)
  - Tests image and thumbnail generation with various formats (JPEG, PNG)
  - Tests soft delete and restore operations
  - Tests JSON error responses
  - Tests thumbnail color preservation (verifies thumbnails aren't black)
- **PhotoServiceIntegrationTest** - Service-level thumbnail tests
  - Tests thumbnail generation for JPEG images (TYPE_INT_RGB)
  - Tests thumbnail generation for PNG images (TYPE_INT_ARGB)
  - Tests aspect ratio preservation
  - Tests color preservation across different image types
  - Tests multiple thumbnail widths

## Related Files

**Backend:**
- `src/main/java/com/muczynski/library/domain/Photo.java` - Photo entity
- `src/main/java/com/muczynski/library/service/PhotoService.java` - Business logic
- `src/main/java/com/muczynski/library/service/GooglePhotosService.java` - Google Photos API
- `src/main/java/com/muczynski/library/controller/PhotoController.java` - REST API
- `src/main/java/com/muczynski/library/dto/PhotoDto.java` - Data transfer object
- `src/main/java/com/muczynski/library/mapper/PhotoMapper.java` - MapStruct mapper

**Frontend:**
- `frontend/src/api/photos.ts` - Photo API functions
- `frontend/src/components/photos/` - Photo React components
- `frontend/src/types/dtos.ts` - PhotoDto TypeScript interface

**Documentation:**
- `endpoints.md` - Complete API endpoint documentation
- `photos-design.md` - Detailed photo storage design (legacy, may be outdated)
- `feature-design-import-export.md` - Photo export/import details

## Frontend Display Guidelines

**IMPORTANT: Photo Aspect Ratio Preservation**
- Photo thumbnails MUST preserve their original aspect ratio when displayed as primary content
- DO NOT use `aspect-square` class on photo gallery containers
- DO NOT use `object-cover` on photo gallery thumbnails (causes cropping)
- USE `object-contain` to display full image without cropping
- USE `h-auto` to allow natural height based on aspect ratio
- Backend generates thumbnails with correct aspect ratio (e.g., 400x533 for 3:4 images)
- Frontend must respect and display these aspect ratios without forcing square containers

**Exception:** Small table thumbnails (e.g., 48x48px in DataManagementPage) MAY use `object-cover` with fixed dimensions for consistent table layout, as these are decorative previews, not primary content.

**Example (PhotoGallery.tsx):**
```tsx
{/* CORRECT - Primary photo display */}
<div className="bg-gray-100 flex items-center justify-center min-h-[200px]">
  <img className="w-full h-auto object-contain" />
</div>

{/* WRONG - causes cropping of primary content */}
<div className="aspect-square bg-gray-100">
  <img className="w-full h-full object-cover" />
</div>

{/* OK - Tiny table preview (not primary content) */}
<img className="w-12 h-12 object-cover rounded" />
```

**Current Usage:**
- ✅ `PhotoGallery.tsx` - Uses `object-contain` for aspect ratio preservation
- ✅ `DataManagementPage.tsx` - Uses `object-cover` on 48x48px table thumbnails (acceptable)

## Known Limitations

1. **IndexedDB Caching Not Implemented** - Original design included browser-based IndexedDB caching for thumbnails and book data. Current implementation uses TanStack Query caching instead.

2. **Caption Edit UI Missing** - While backend supports caption editing, dedicated UI for editing captions may not be fully implemented in all contexts.

3. **Google Photos API Scopes** - May be using deprecated scopes that expire March 2025. Needs verification and update to current Google Photos API scopes.

4. **Photo Entity Schema** - Some fields in the Photo entity may not match the original `photos-design.md` specification. This document reflects actual implementation.

5. **Frontend Tests Missing** - No Playwright or React Testing Library tests for photo components currently exist.
