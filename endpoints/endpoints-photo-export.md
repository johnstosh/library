# Photo Export Endpoints (Google Photos Sync + ZIP Download)

## GET /api/photo-export/zip-parts
Computes how the photo collection splits into alphabetically-bounded ZIP parts.
Called automatically by the Photos page on load to populate the download buttons.

**Authentication:** Librarian only

**Response:** Array of `PhotoZipPartDto`:
```json
[
  { "partNumber": 1, "totalParts": 3, "rangeLabel": "0-9, A-H", "photoCount": 316, "estimatedMb": 316, "startKey": "0", "endKey": "H" },
  { "partNumber": 2, "totalParts": 3, "rangeLabel": "I-R",      "photoCount": 291, "estimatedMb": 291, "startKey": "I", "endKey": "R" },
  { "partNumber": 3, "totalParts": 3, "rangeLabel": "S-Y",      "photoCount": 229, "estimatedMb": 229, "startKey": "S", "endKey": "Y" }
]
```

**Behavior:**
- Derives each photo's sort key from book title (or author name / loan's book title), stripping leading articles ("The ", "An ", "A "). Digits and symbols group under "0-9".
- Assumes 1 MB per photo; targets ≤ 400 MB per part.
- Splits recalculate dynamically — no configuration needed as the collection grows.
- See `feature-design-import-export.md` for full algorithm description.

---

## GET /api/photo-export/zip/{partNumber}
Streams one alphabetically-bounded ZIP part as a file download.

**Authentication:** Librarian only

**Path Parameter:** `partNumber` — 1-based part number (call `/zip-parts` first to discover the range)

**Response:** `application/zip` stream

**Filename format:** `{date}-library-photos-{branch}-part{N}of{M}-{partCount}-of-{totalCount}-photos-{range}.zip`
Example: `2026-05-09-library-photos-muczynski-part1of3-316-of-836-photos-0-9-a-h.zip`

**Behavior:**
- Streams photos whose sort key falls in [startKey, endKey] for the requested part.
- Photos missing local image bytes are fetched from Google Photos on the fly (requires valid OAuth token).

---

## GET /api/photo-export
Streams **all** photos as a single ZIP file (legacy endpoint, no size limit).

**Authentication:** Librarian only

**Response:** `application/zip` stream

**Filename format:** `{date}-library-photos-{branch}-{count}-photos.zip`

**Behavior:**
- Memory-efficient streaming — photos are loaded one at a time, never buffered entirely in RAM.
- Use `/zip-parts` + `/zip/{partNumber}` instead for collections over ~400 MB.

---

## GET /api/photo-export/stats
Returns photo sync statistics.

**Authentication:** Librarian only *(was incorrectly public in earlier versions)*

**Response:** `PhotoExportStatsDto`
```json
{
  "total": 836, "exported": 800, "imported": 790,
  "pendingExport": 36, "pendingImport": 10, "failed": 2, "inProgress": 0,
  "albumName": "Muczynski Library", "albumId": "ABC123..."
}
```

---

## GET /api/photo-export/photos
Returns all photos with export status (metadata only, no image bytes).

**Authentication:** Librarian only *(was incorrectly public in earlier versions)*

**Response:** Array of `PhotoExportInfoDto` — id, caption, permanentId, exportStatus, bookTitle, authorName, checksum, etc.

---

## GET /api/photo-export/photos/{photoId}
Returns export info for a single photo. Used by the frontend to refresh just the changed row after a single-photo export/import.

**Authentication:** Librarian only

---

## POST /api/photo-export/export-all
Uploads all pending photos to Google Photos in batches of 50.

**Authentication:** Librarian only

---

## POST /api/photo-export/export/{photoId}
Uploads a single photo to Google Photos and returns the updated `PhotoExportInfoDto`.

**Authentication:** Librarian only

---

## POST /api/photo-export/import-all
Downloads all photos that have a `permanentId` but no local image data, in batches of 20.

**Authentication:** Librarian only

---

## POST /api/photo-export/import/{photoId}
Downloads a single photo from Google Photos and returns the updated `PhotoExportInfoDto`.

**Authentication:** Librarian only

---

## POST /api/photo-export/verify/{photoId}
Checks that a photo's `permanentId` still resolves in Google Photos.

**Authentication:** Librarian only

**Response:** `PhotoVerifyResultDto` — `valid`, `message`, `filename`, `mimeType`

---

## POST /api/photo-export/unlink/{photoId}
Clears a photo's `permanentId` (marks it pending export again). Does **not** delete the photo from Google Photos.

**Authentication:** Librarian only

---

## POST /api/photo-export/backfill-checksums
Computes and saves `imageChecksum` for any photos that have local image bytes but no stored checksum. Safe to call multiple times (idempotent).

**Authentication:** Librarian only

**Response:** `{ "updated": 12 }`

---

**Related:** `PhotoExportController.java`, `PhotoExportService.java`, `PhotoZipPartDto.java`, `feature-design-import-export.md`, `feature-design-photos.md`
