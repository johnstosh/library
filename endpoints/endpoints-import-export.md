# Import/Export Endpoints

## JSON Import/Export Endpoints

### GET /api/import/json
Exports database to JSON format for backup/migration.

**Authentication:** Librarian only

**Response:** ImportRequestDto containing:
- Branches
- Authors
- Users (including hashed passwords)
- Books
- Loans
- Favorites (username, list name, book title+author or author name)
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
- `branchCount` - Total number of branches
- `bookCount` - Total number of books
- `authorCount` - Total number of authors
- `userCount` - Total number of users
- `loanCount` - Total number of loans

**Example Response:**
```json
{
  "branchCount": 5,
  "bookCount": 300,
  "authorCount": 150,
  "userCount": 25,
  "loanCount": 50
}
```

### GET /api/import/favorite-stats
Returns favorite-list membership counts for the Data Management **Favorites Statistics** section.

**Authentication:** Librarian only

**Response:** array of `{ "listName", "bookCount", "authorCount" }`, sorted by `(bookCount + authorCount)` descending then name.

**Purpose:**
- Provides accurate database counts for the Data Management page
- Used for generating export filenames with accurate statistics
- Returns actual database counts (not cached/paginated frontend data)

---

### GET /api/import/availability-stats
Returns named book-count statistics for the Data Management Books Availability section.

**Authentication:** Librarian only

**Response:** BookAvailabilityStatsDto containing:
- `electronicResource` - books with `electronicResource = true`
- `hasCallNumber` - books with a non-blank LOC call number, excluding `WITHDRAWN` and `REQUESTED`
- `hasFreeOnlineText` - books with a non-blank free-text URL
- `hasFreeOnlineAudio` - books whose free-text URL contains "librivox"
- `withdrawn` / `requested` - books with those statuses
- `availableAtYdl` / `ydlPaper` / `ydlEbook` / `ydlAudio`
- `availableAtEmu` / `emuPaper` / `emuEbook` / `emuAudio`

Boolean flags are counted only when true (null/false are excluded).

**Example Response:**
```json
{
  "electronicResource": 3,
  "hasCallNumber": 8,
  "hasFreeOnlineText": 7,
  "hasFreeOnlineAudio": 2,
  "withdrawn": 1,
  "requested": 9,
  "availableAtYdl": 5,
  "ydlPaper": 2,
  "ydlEbook": 4,
  "ydlAudio": 1,
  "availableAtEmu": 6,
  "emuPaper": 3,
  "emuEbook": 2,
  "emuAudio": 1
}
```

---

**Related:** ImportController.java, ImportService.java, ImportRequestDto.java, DatabaseStatsDto.java, BookAvailabilityStatsDto.java, feature-design-import-export.md
