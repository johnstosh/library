# Photo Export Endpoints (Google Photos Sync)

## GET /api/photo-export/stats
Returns statistics about photo export status.

**Authentication:** Public (permitAll) ⚠️ **Should be librarian-only**

**Response:** Map containing:
```json
{
  "totalPhotos": 150,
  "exportedPhotos": 120,
  "notExportedPhotos": 30
}
```

**Use Case:**
- Display photo sync status on Data Management page
- Shows how many photos have been backed up to Google Photos

---

## GET /api/photo-export/photos
Returns all photos with their export status.

**Authentication:** Public (permitAll) ⚠️ **Should be librarian-only**

**Response:** Array of photo information maps:
```json
[
  {
    "id": 1,
    "caption": "Book cover",
    "permanentId": "abc123...",
    "hasBeenUploaded": true,
    "bookTitle": "The Great Gatsby",
    "authorName": null
  }
]
```

**Use Case:**
- View which photos have been synced to Google Photos
- Identify photos that need backup

---

## POST /api/photo-export/export-all
Uploads all photos to Google Photos for backup.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Success message or error

**Behavior:**
- Uploads all local photos to Google Photos
- Updates permanent ID for each photo
- Skips photos already uploaded

**Use Case:**
- Backup all library photos to Google Photos cloud storage
- One-click photo backup operation

---

## POST /api/photo-export/export/{photoId}
Uploads a single photo to Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to export

**Response:** Success message or 404 if photo not found

**Use Case:**
- Selective photo backup to Google Photos
- Re-upload modified photo

---

## POST /api/photo-export/import/{photoId}
Downloads a single photo from Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to import

**Response:** Success message or error

**Behavior:**
- Fetches photo from Google Photos using permanent ID
- Updates local photo data with downloaded bytes
- Requires photo to have valid permanent ID

**Use Case:**
- Restore photo from Google Photos backup
- Download updated photo from cloud

---

## POST /api/photo-export/import-all
Downloads all photos from Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Success message or error

**Behavior:**
- Fetches all photos with permanent IDs from Google Photos
- Updates local database with downloaded photo bytes
- Skips photos without permanent IDs

**Use Case:**
- Restore all photos from Google Photos backup
- Sync photos after database restore from JSON

---

## POST /api/photo-export/verify/{photoId}
Verifies that a photo's permanent ID is valid in Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to verify

**Response:** Verification result with photo metadata

**Use Case:**
- Check if photo still exists in Google Photos
- Troubleshoot sync issues

---

## POST /api/photo-export/unlink/{photoId}
Removes the Google Photos permanent ID from a photo.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to unlink

**Response:** Success message or 404 if photo not found

**Behavior:**
- Clears permanent ID field
- Marks photo as not uploaded
- Does NOT delete photo from Google Photos (only removes link)

**Use Case:**
- Prepare photo for re-upload
- Fix sync issues

---

**Related:** PhotoExportController.java, PhotoExportService.java, GooglePhotosService.java, feature-design-photos.md
