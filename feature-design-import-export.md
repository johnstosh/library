# Import/Export System Design

## Overview
The application supports importing and exporting library data for backup, migration, and data management.

## JSON Database Import/Export

### Endpoints
- `GET /api/import/json` - Export database to JSON
- `POST /api/import/json` - Import database from JSON
- `GET /api/import/stats` - Get database statistics (total counts)
- **Authentication**: Librarian only

### What's Included
The JSON export includes:
- Libraries
- Authors
- Users (including hashed passwords)
- Books
- Loans
- **Photo metadata** (permanent IDs, captions, ordering, export status)

### What's NOT Included
- **Photo binary data** (actual image bytes) - too large for JSON export
- **How to backup photo images**: Use the separate Photo Export feature (Google Photos sync) to backup/restore photo binary data
- **Complete backup workflow**:
  1. Export JSON for metadata (libraries, authors, books, users, loans, photo metadata)
  2. Export photos to Google Photos using `/api/photo-export/export-all` (creates cloud backup with permanent IDs)
  3. To restore: Import JSON first, then import photos from Google Photos using `/api/photo-export/import-all`

### Photo Metadata in Export
Photo metadata is now included in JSON export for complete backup:
- `permanentId` - Google Photos permanent ID for reconnecting to cloud storage
- `contentType` - MIME type (image/jpeg, image/png, etc.)
- `caption` - User-provided caption
- `photoOrder` - Position in photo list
- `imageChecksum` - SHA-256 hash for photo identification/matching
- `exportStatus` - Status of Google Photos export
- `exportedAt` - Timestamp when photo was backed up
- `bookTitle` / `bookAuthorName` - Reference to associated book
- `authorName` - Reference to associated author (for author-only photos)

### Database Statistics Display
The Data Management page displays real-time database statistics showing:
- **Libraries** - Total number of library branches
- **Books** - Total number of books in the catalog
- **Authors** - Total number of authors
- **Users** - Total number of user accounts
- **Loans** - Total number of loan records

These statistics are fetched from the `/api/import/stats` endpoint which returns actual database counts (not cached/paginated data from the frontend). The statistics are displayed in a grid format below the "Database Export/Import" header, providing a quick overview of the database contents before export. The same counts are used in the export filename.

**API Response (DatabaseStatsDto)**:
```json
{
  "libraryCount": 5,
  "bookCount": 300,
  "authorCount": 150,
  "userCount": 25,
  "loanCount": 50
}
```

### Export Format
- DTO: `ImportRequestDto`
- Contains collections of each entity type
- **Reference-based format**: Entities reference other entities by natural keys instead of embedding full objects
  - Books: Use `authorName` (string) instead of embedded author object
  - Loans: Use `bookTitle`, `bookAuthorName`, `username` instead of embedded book/user objects
  - Photos: Use `imageChecksum` (SHA-256) for photo identification
- **Filename Format**: `{library-name}-{book-count}-books-{author-count}-authors-{user-count}-users-{loan-count}-loans-{photo-count}-photos-{date}.json`
  - Example: `st-martin-de-porres-125-books-47-authors-12-users-18-loans-42-photos-2025-12-29.json`
  - Library name is sanitized (lowercase, special chars replaced with hyphens)
  - Photo count represents the total photo records in the database (photo-metadata, not binary data which is stored in Google Photos)

### New vs Old Format
The export format changed to use lightweight references instead of embedded objects:

**New Format (Current - Export Only):**
```json
{
  "books": [{
    "title": "Book Title",
    "authorName": "Author Name",
    "libraryName": "Library Name"
  }],
  "loans": [{
    "bookTitle": "Book Title",
    "bookAuthorName": "Author Name",
    "username": "johndoe",
    "loanDate": "2025-01-01"
  }]
}
```

**Old Format (Deprecated - Import Only):**
```json
{
  "books": [{
    "title": "Book Title",
    "author": {"name": "Author Name", "dateOfBirth": "..."},
    "libraryName": "Library Name"
  }],
  "loans": [{
    "book": {"title": "Book Title", "author": {"name": "Author Name"}},
    "user": {"username": "johndoe", "authorities": ["USER"]},
    "loanDate": "2025-01-01"
  }]
}
```

**Backward Compatibility:**
- **Import**: Supports BOTH old format (embedded objects) and new format (references)
- **Export**: Only exports new format (references)

### Import Behavior
- **Merges** data with existing records (doesn't delete existing data)
- **Format detection**: Automatically detects and handles both old and new formats
  - For books: Checks `authorName` first, falls back to `author.name`
  - For loans: Checks reference fields first (`bookTitle`, `username`), falls back to embedded objects
  - For photos: Matches by `imageChecksum` (SHA-256) first, then `permanentId`, then book/author + photoOrder
- Matches entities by natural keys:
  - Libraries: by name
  - Authors: by name
  - Books: by title + author name
  - Users: by username
  - Loans: by book ID + user ID + loan date
  - Photos: by imageChecksum, permanentId, or book/author + photoOrder
- Handles missing references gracefully
- Logs warnings for unresolved references

### Implementation
- `ImportService.exportData()` - Creates export DTO (includes photo metadata)
- `ImportService.importData()` - Processes import DTO
- Photo metadata exported via `PhotoMetadataProjection` (excludes binary data for performance)

## Photo Export (Google Photos Sync System)

### Endpoints
- `/api/photo-export/**` - Google Photos sync endpoints
- **Authentication**: Mostly librarian-only (some endpoints currently public ⚠️ security issue)
- See `endpoints.md` for complete endpoint documentation

### Purpose
- **Cloud sync system** for bidirectional photo synchronization with Google Photos
- **NOT a local ZIP download system** - syncs with Google Photos cloud storage
- Photos contain large binary data that cannot be included in JSON export
- Allows incremental photo sync without exporting entire database

### Key Features
1. **Upload to Google Photos** (`export-all`, `export/{id}`)
   - Uploads local photos to Google Photos
   - Stores permanent ID for each photo
   - Enables cloud backup of photo binary data

2. **Download from Google Photos** (`import-all`, `import/{id}`)
   - Fetches photos from Google Photos using permanent IDs
   - Updates local database with downloaded photo bytes
   - Enables restore from cloud backup

3. **Sync Management** (`verify/{id}`, `unlink/{id}`)
   - Verify photo existence in Google Photos
   - Unlink photos to prepare for re-upload
   - Troubleshoot sync issues

4. **Status Tracking** (`stats`, `photos`)
   - View photo sync statistics
   - Identify photos needing backup
   - Monitor sync progress

### Implementation Details
- **No ZIP download endpoint** - photos are synced to/from Google Photos, not packaged as ZIP
- **Permanent IDs**: Photos store Google Photos permanent IDs for cloud linking
- **Bidirectional sync**: Can upload (export) and download (import)
- **Per-photo operations**: Can sync individual photos or all photos in bulk
- **Status tracking**: Photo entity tracks sync status with permanent ID field

### Google Photos API Fallback
When fetching a photo from Google Photos for import, the system uses a fallback strategy:
1. **Primary**: `GET /v1/mediaItems/{id}` - Single-item endpoint
2. **Fallback**: `POST /v1/mediaItems:batchGet` - Batch endpoint (if primary returns 404)

This fallback is necessary because sometimes the single-item endpoint returns 404 while the batch endpoint succeeds. This can happen when:
- The photo was uploaded with a different OAuth client
- The photo's permissions differ between access methods
- Google Photos API inconsistencies

The `GooglePhotosLibraryClient.getMediaItem()` method automatically handles this fallback internally.

### React UI (Photo Import/Export Status Section)
The Data Management page includes a "Photo Import/Export Status" section with:

**Statistics Cards** (6-column grid):
- Total Photos - total count
- Exported - photos uploaded to Google Photos (green)
- Imported - photos downloaded from Google Photos (blue)
- Pending Export - photos with local data but no permanentId, excluding FAILED (yellow)
- Pending Import - photos with permanentId but no local data, excluding FAILED (gray)
- Failed - photos with FAILED export status (red)

**Status Derivation Logic**:
The photo status displayed in the list is derived from actual data to ensure it matches the statistics counts exactly:
1. **FAILED** - if `exportStatus == FAILED` (preserves failure for troubleshooting)
2. **IN_PROGRESS** - if `exportStatus == IN_PROGRESS` (preserves in-progress state)
3. **COMPLETED** - if photo has both `permanentId` AND `imageChecksum` (fully synced)
4. **PENDING_IMPORT** - if photo has `permanentId` but no `imageChecksum` (needs download from Google Photos)
5. **PENDING** - if photo has `imageChecksum` but no `permanentId` (has image, needs export to Google Photos)
6. **NO_IMAGE** - if photo has neither `imageChecksum` nor `permanentId` (no data to export)

**Note on Counting**: Pending Export and Pending Import counts exclude FAILED photos to prevent double-counting. A failed photo is counted in "Failed" only, not in both "Failed" and its pending category.

**Action Buttons**:
- **Export All Pending Photos** - batch upload pending photos to Google Photos
- **Import All Pending Photos** - batch download photos from Google Photos
- **Refresh Status** - reload statistics and photo list

**Photo Details Table** with columns:
- Photo thumbnail
- Title/Author (book title + author OR author name)
- LOC Call Number (formatted for spine display)
- Status badge (Completed/Failed/In Progress/Pending Import/Pending/No Image)
- Exported At timestamp
- Permanent ID (truncated with tooltip)
- Actions per photo:
  - **Paste Image** (always available) - paste an image from clipboard (Ctrl+V) to update photo
  - Export (if hasImage && !permanentId)
  - Import (if permanentId && !hasImage)
  - View in Google Photos (if permanentId)
  - Verify (if permanentId)
  - Unlink (if permanentId)
  - Delete

**Paste Image Feature**:
- Click the upload icon (↑) button in the Actions column to enter paste mode
- A blue notification banner appears with instructions
- Press Ctrl+V (or Cmd+V on Mac) to paste an image from clipboard
- The image is automatically uploaded to replace/update the photo
- Click "Cancel" in the notification banner to exit paste mode
- Uses the `/api/photos/{id}/crop` endpoint to update the photo image

**Status Messages and Notifications**:
- Success/error messages appear in a **floating box at the top of the window**
- Messages remain visible while scrolling through the photo table
- Error messages include detailed information (e.g., "Failed to import photo #42: Photo not found in Google Photos")
- Messages use position: fixed with z-50 to stay above page content
- Shadow styling provides clear visual separation from content below

**Data-test Attributes**:
- `photo-export-section` - main section wrapper
- `photos-header` - section title
- `export-all-photos-btn`, `import-all-photos-btn`, `refresh-photos-btn`
- `export-stats` - statistics grid
- `stats-total`, `stats-exported`, `stats-imported`, `stats-pending-export`, `stats-pending-import`, `stats-failed`
- `photos-table`, `photos-table-body`
- `paste-photo-{id}` - paste image button for specific photo

## LOC Bulk Lookup Import/Export

### Purpose
- Import/export LOC call number lookup results
- Allows batch processing of books for LOC lookup
- Shares lookup results between instances

### Functionality
- Export books with/without LOC numbers
- Import LOC numbers from external source
- Bulk update books with LOC call numbers

## Books from Google Photos Feed

### Endpoint
- `/api/books-from-feed/**` - Import books from Google Photos
- **Authentication**: Librarian only

### Purpose
- Import books directly from Google Photos feed
- Automatically associates photos with imported books
- Streamlines book cataloging workflow

### Process
1. Fetch photos from Google Photos feed
2. Extract book information (title, author) from photo metadata or AI
3. Create book records
4. Associate photos with books
5. Download and store photos locally

## Data Integrity

### Foreign Key Handling
- Import resolves references by natural keys
- Missing references logged as warnings
- Import continues even if some references can't be resolved

### Duplicate Prevention
- Import uses "find or create" pattern
- Existing entities matched by natural keys
- Updates existing entities rather than creating duplicates

## Related Files
- `ImportService.java` - Core import/export logic
- `ImportController.java` - REST API
- `ImportRequestDto.java` - JSON structure
- `PhotoExportService.java` - Photo export functionality
- `BooksFromFeedController.java` - Google Photos feed import
- `endpoints.md` - API documentation
- `feature-design-photos.md` - Photo storage details
