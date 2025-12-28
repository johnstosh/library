# Photo Management Endpoints

## Book Photos

### POST /api/books/{bookId}/photos
Upload a photo to a book.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `bookId` - Book ID to add photo to

**Request:** Multipart file upload with `file` parameter

**Response:** 201 Created with PhotoDto or error message

**Use Case:**
- Upload book cover or interior photos
- Add photos from local files

---

### POST /api/books/{bookId}/photos/from-google-photos
Add photos from Google Photos to a book.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `bookId` - Book ID to add photos to

**Request Body:**
```json
{
  "photos": [
    {
      "id": "google-photos-permanent-id",
      "url": "https://...",
      "mimeType": "image/jpeg"
    }
  ]
}
```

**Response:** 201 Created with:
```json
{
  "savedCount": 2,
  "failedCount": 0,
  "savedPhotos": [PhotoDto, ...],
  "failedPhotos": []
}
```

**Use Case:**
- Import photos from Google Photos library
- Bulk photo import for books

---

### GET /api/books/{bookId}/photos
List all photos for a book.

**Authentication:** Public (`permitAll()`)

**Path Parameter:** `bookId` - Book ID to retrieve photos for

**Response:** Array of PhotoDto

**Use Case:**
- Display book photo gallery
- Get photo metadata for book detail page

---

### PUT /api/books/{bookId}/photos/{photoId}
Update photo metadata (caption, etc.).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `bookId` - Book ID
- `photoId` - Photo ID to update

**Request Body:** PhotoDto with updated fields

**Response:** Updated PhotoDto

**Use Case:**
- Edit photo caption
- Update photo metadata

---

### DELETE /api/books/{bookId}/photos/{photoId}
Delete a photo from a book.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `bookId` - Book ID
- `photoId` - Photo ID to delete

**Response:** 204 No Content

**Use Case:**
- Remove unwanted photos from book
- Clean up duplicate photos

---

### PUT /api/books/{bookId}/photos/{photoId}/rotate-cw
Rotate photo 90 degrees clockwise.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `bookId` - Book ID
- `photoId` - Photo ID to rotate

**Response:** 200 OK

**Use Case:**
- Fix incorrectly oriented photos
- Adjust photo orientation after upload

---

### PUT /api/books/{bookId}/photos/{photoId}/rotate-ccw
Rotate photo 90 degrees counter-clockwise.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `bookId` - Book ID
- `photoId` - Photo ID to rotate

**Response:** 200 OK

**Use Case:**
- Fix incorrectly oriented photos
- Adjust photo orientation after upload

---

### PUT /api/books/{bookId}/photos/{photoId}/move-left
Move photo left in display order (decrease order index).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `bookId` - Book ID
- `photoId` - Photo ID to move

**Response:** 200 OK

**Use Case:**
- Reorder photos for book gallery
- Make specific photo appear first

---

### PUT /api/books/{bookId}/photos/{photoId}/move-right
Move photo right in display order (increase order index).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `bookId` - Book ID
- `photoId` - Photo ID to move

**Response:** 200 OK

**Use Case:**
- Reorder photos for book gallery
- Adjust photo display order

---

## Author Photos

### POST /api/authors/{authorId}/photos
Upload a photo to an author.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `authorId` - Author ID to add photo to

**Request:** Multipart file upload with `file` parameter

**Response:** 201 Created with PhotoDto or error message

**Use Case:**
- Upload author portrait or biography photos
- Add photos from local files

---

### POST /api/authors/{authorId}/photos/from-google-photos
Add photos from Google Photos to an author.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `authorId` - Author ID to add photos to

**Request Body:** Same format as book photos endpoint

**Response:** 201 Created with saved/failed counts and photo arrays

**Use Case:**
- Import author photos from Google Photos library
- Bulk photo import for authors

---

### GET /api/authors/{authorId}/photos
List all photos for an author.

**Authentication:** Public (`permitAll()`)

**Path Parameter:** `authorId` - Author ID to retrieve photos for

**Response:** Array of PhotoDto

**Use Case:**
- Display author photo gallery
- Get photo metadata for author detail page

---

### DELETE /api/authors/{authorId}/photos/{photoId}
Delete a photo from an author.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `authorId` - Author ID
- `photoId` - Photo ID to delete

**Response:** 204 No Content or 404 if not found

**Use Case:**
- Remove unwanted photos from author
- Clean up duplicate photos

---

### PUT /api/authors/{authorId}/photos/{photoId}/rotate-cw
Rotate author photo 90 degrees clockwise.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `authorId` - Author ID
- `photoId` - Photo ID to rotate

**Response:** 200 OK

**Use Case:**
- Fix incorrectly oriented author photos
- Adjust photo orientation after upload

---

### PUT /api/authors/{authorId}/photos/{photoId}/rotate-ccw
Rotate author photo 90 degrees counter-clockwise.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `authorId` - Author ID
- `photoId` - Photo ID to rotate

**Response:** 200 OK

**Use Case:**
- Fix incorrectly oriented author photos
- Adjust photo orientation after upload

---

### PUT /api/authors/{authorId}/photos/{photoId}/move-left
Move author photo left in display order.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `authorId` - Author ID
- `photoId` - Photo ID to move

**Response:** 200 OK

**Use Case:**
- Reorder author photos for gallery
- Make specific photo appear first

---

### PUT /api/authors/{authorId}/photos/{photoId}/move-right
Move author photo right in display order.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `authorId` - Author ID
- `photoId` - Photo ID to move

**Response:** 200 OK

**Use Case:**
- Reorder author photos for gallery
- Adjust photo display order

---

## Photo Direct Access

### GET /api/photos/{id}/image
Get full-size photo image bytes.

**Authentication:** Public (`permitAll()`)

**Path Parameter:** `id` - Photo ID

**Response:** Image bytes with appropriate Content-Type (image/jpeg, image/png, etc.)

**Use Case:**
- Display full-size photos in UI
- Download original photo
- Book/author detail page photo display

---

### GET /api/photos/{id}/thumbnail
Get thumbnail version of photo.

**Authentication:** Public (`permitAll()`)

**Path Parameter:** `id` - Photo ID

**Query Parameter:** `width` - Thumbnail width in pixels

**Response:** Thumbnail image bytes with appropriate Content-Type

**Use Case:**
- Display thumbnails in book/author galleries
- Optimize page load performance with smaller images
- Photo picker thumbnails

---

### DELETE /api/photos/{id}
Soft delete a photo (marks as deleted, doesn't remove from database).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `id` - Photo ID to delete

**Response:** 200 OK

**Use Case:**
- Remove photo from display without permanent deletion
- Allow photo restoration later
- Safe deletion with undo capability

---

### POST /api/photos/{id}/restore
Restore a soft-deleted photo.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `id` - Photo ID to restore

**Response:** 200 OK

**Use Case:**
- Undo accidental photo deletion
- Restore previously removed photo
- Recover soft-deleted photos

---

### PUT /api/photos/{id}/crop
Replace photo with cropped/edited version.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `id` - Photo ID to replace

**Request:** Multipart file upload with `file` parameter containing cropped image

**Response:** 200 OK

**Use Case:**
- Crop photo to remove unwanted areas
- Replace photo with edited version
- Update photo while preserving metadata and ordering

---

**Related:** BookController.java, AuthorController.java, PhotoController.java, PhotoService.java, PhotoDto.java, feature-design-photos.md
