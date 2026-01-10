# Import/Export Endpoints

## JSON Import/Export Endpoints

### GET /api/import/json
Exports database to JSON format for backup/migration.

**Authentication:** Librarian only

**Response:** ImportRequestDto containing:
- Libraries
- Authors
- Users (including hashed passwords)
- Books
- Loans
- **Photos:** NOT INCLUDED - photos are excluded due to size

**Important Notes:**
- Photos are intentionally excluded from JSON export to prevent response size issues
- Photo data should be managed separately via the Photo Export feature (`/api/photo-export`)
- The JSON export is designed for backing up metadata and structural data only
- Photo files are backed up to Google Photos via the separate photo export functionality

---

### POST /api/import/json
Imports database from JSON format.

**Authentication:** Librarian only

**Request Body:** ImportRequestDto (same structure as export)

**Behavior:**
- Merges data with existing records (doesn't delete existing data)
- Matches entities by natural keys (e.g., library name, author name, book title+author)
- Photos in the import file are processed but image bytes are not expected

---

### GET /api/import/stats
Returns database statistics with total counts for each entity type.

**Authentication:** Librarian only

**Response:** DatabaseStatsDto containing:
- `libraryCount` - Total number of libraries
- `bookCount` - Total number of books
- `authorCount` - Total number of authors
- `userCount` - Total number of users
- `loanCount` - Total number of loans

**Example Response:**
```json
{
  "libraryCount": 5,
  "bookCount": 300,
  "authorCount": 150,
  "userCount": 25,
  "loanCount": 50
}
```

**Purpose:**
- Provides accurate database counts for the Data Management page
- Used for generating export filenames with accurate statistics
- Returns actual database counts (not cached/paginated frontend data)

---

**Related:** ImportController.java, ImportService.java, ImportRequestDto.java, DatabaseStatsDto.java, feature-design-import-export.md
