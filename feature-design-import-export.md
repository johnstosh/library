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
- **Photos are NOT included in JSON export**
- Reason: Photos contain large binary data (image bytes) that would make the response too large
- Photos should be managed separately via the Photo Export feature

### Export Format
- DTO: `ImportRequestDto`
- Contains collections of each entity type
- References between entities use natural keys (e.g., library name, author name, book title+author)

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

## Photo Export (Separate System)

### Endpoint
- `/api/photo-export/**` - Photo backup endpoints
- **Authentication**: Authenticated users

### Purpose
- Separate system for backing up photos to Google Photos
- Photos contain large binary data that cannot be included in JSON export
- Allows incremental photo backup without exporting entire database

### Implementation
- Uploads photos to Google Photos
- Tracks export status in Photo entity:
  - `exportedAt` - Timestamp of last export
  - `exportStatus` - Status of export (SUCCESS, FAILED, PENDING)
  - `exportErrorMessage` - Error details if failed

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
