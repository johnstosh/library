# Import/Export System Design

## Overview
The application supports importing and exporting library data for backup, migration, and data management.

## JSON Database Import/Export

### Endpoint
- `GET /api/import/json` - Export database to JSON
- `POST /api/import/json` - Import database from JSON
- **Authentication**: Librarian only

### What's Included
The JSON export includes:
- Libraries
- Authors
- Users (including hashed passwords)
- Books
- Loans

### What's NOT Included
- **Photos are NOT included in JSON export (neither binary data nor metadata)**
- **Why binary data is excluded**: Photos contain large binary data (image bytes) that would make the response too large
- **Why metadata is excluded**: Even lightweight photo metadata (permanent IDs, captions, ordering) is excluded by design to keep exports "lightweight and fast" (per design spec in ImportService.java)
- **Photo metadata locations**: Photo metadata is preserved in the database as sub-objects of Books and Authors with foreign keys
- **Photo reconnection**: Photos can be reconnected during import via book/author matching using natural keys
- **How to backup photos**: Use the separate Photo Export feature (Google Photos sync) to backup/restore photo binary data
- **Complete backup workflow**:
  1. Export JSON for metadata (libraries, authors, books, users, loans)
  2. Export photos to Google Photos using `/api/photo-export/export-all` (creates cloud backup with permanent IDs)
  3. To restore: Import JSON first, then import photos from Google Photos using `/api/photo-export/import-all`

### Database Statistics Display
The Data Management page displays real-time database statistics showing:
- **Libraries** - Total number of library branches
- **Books** - Total number of books in the catalog
- **Authors** - Total number of authors
- **Users** - Total number of user accounts
- **Loans** - Total number of loan records

These statistics are displayed in a grid format below the "Database Export/Import" header, providing a quick overview of the database contents before export. The same counts are used in the export filename.

### Export Format
- DTO: `ImportRequestDto`
- Contains collections of each entity type
- References between entities use natural keys (e.g., library name, author name, book title+author)
- **Filename Format**: `{library-name}-{book-count}-books-{author-count}-authors-{user-count}-users-{loan-count}-loans-{date}.json`
  - Example: `st-martin-de-porres-125-books-47-authors-12-users-18-loans-2025-12-29.json`
  - Library name is sanitized (lowercase, special chars replaced with hyphens)

### Import Behavior
- **Merges** data with existing records (doesn't delete existing data)
- Matches entities by natural keys:
  - Libraries: by name
  - Authors: by name
  - Books: by title + author name
  - Users: by username
  - Loans: creates new loans
- Handles missing references gracefully
- Logs warnings for unresolved references

### Implementation
- `ImportService.exportToJson()` - Creates export DTO
- `ImportService.importFromJson()` - Processes import DTO
- Photos explicitly set to `null` in export to prevent inclusion

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
