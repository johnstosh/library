# Books from Feed Endpoints

## GET /api/books-from-feed/saved-books
Returns books saved from Google Photos that need processing.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of saved book data including:
- Photo information
- Processing status
- AI-generated metadata (if processed)

**Use Case:**
- View books imported from Google Photos feed
- Identify books needing AI processing

---

## POST /api/books-from-feed/process-single/{bookId}
Processes a single saved book with AI to extract metadata.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `bookId` - Saved book ID to process

**Response:** Processed book data with AI-extracted metadata:
- Title
- Author
- Publication year
- Publisher
- Other metadata

**Requirements:**
- User must have xAI API key configured
- Book must have associated photo

**Use Case:**
- Extract book metadata from photo using AI
- Convert saved photos into catalog entries

---

## POST /api/books-from-feed/process-saved
Processes all saved books with AI in batch.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of processed books with AI-extracted metadata

**Behavior:**
- Processes each saved book sequentially
- Uses AI to extract metadata from photos
- Updates processing status for each book

**Use Case:**
- Bulk process imported photos from Google Photos feed
- Automated book cataloging from photos

---

## POST /api/books-from-feed/save-from-picker
Saves photos selected from Google Photos Picker.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Array of Google Photos media item IDs
```json
{
  "mediaItemIds": ["abc123", "def456", "ghi789"]
}
```

**Response:** Success message with count of saved books

**Behavior:**
- Downloads photos from Google Photos
- Creates saved book entries
- Stores photos for later processing

**Use Case:**
- Import book photos from Google Photos using picker UI
- First step in Books from Feed workflow

---

## POST /api/books-from-feed/picker-session
Creates a new Google Photos Picker session.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Picker session data including:
```json
{
  "sessionId": "session-uuid",
  "pickerUrl": "https://photos.google.com/picker/...",
  "expiresAt": "2025-01-15T12:00:00"
}
```

**Use Case:**
- Initialize Google Photos Picker for selecting photos
- Get picker URL to embed in UI

---

## GET /api/books-from-feed/picker-session/{sessionId}
Returns the status of a Google Photos Picker session.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `sessionId` - Session UUID

**Response:** Session status including:
```json
{
  "sessionId": "session-uuid",
  "status": "COMPLETED",
  "selectedCount": 5,
  "expiresAt": "2025-01-15T12:00:00"
}
```

**Use Case:**
- Check if user has finished selecting photos
- Monitor picker session progress

---

## GET /api/books-from-feed/picker-session/{sessionId}/media-items
Returns media items selected in a picker session.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `sessionId` - Session UUID

**Response:** Array of selected media item IDs
```json
{
  "mediaItemIds": ["abc123", "def456", "ghi789"]
}
```

**Use Case:**
- Retrieve photos selected by user in picker
- Process selected photos into saved books

---

**Related:** BooksFromFeedController.java, BooksFromFeedService.java, GooglePhotosService.java, AskGrok.java
