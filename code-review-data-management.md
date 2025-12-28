# Code Review: Data Management

## Date
2025-12-28

## Scope
Data Management feature including JSON import/export, Photo export, and Books from Feed functionality.

## Summary
The Data Management feature has a working backend and frontend for JSON import/export, but the Photo Export download functionality is completely missing. The frontend expects a ZIP download endpoint that doesn't exist in the backend. Books from Feed is fully implemented. Several documentation issues were found.

---

## Bugs Found

### Bug #1: Documentation wrong about Photo Export ZIP Download Endpoint
**Severity:** HIGH
**Location:** Backend - PhotoExportController.java
**Issue:** Frontend calls `GET /api/photo-export` expecting a ZIP file blob download, but this endpoint does NOT exist in PhotoExportController.

**Frontend expectation (data-management.ts:43-53):**
```typescript
export async function exportPhotos(): Promise<Blob> {
  const response = await fetch('/api/photo-export', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to export photos')
  }

  return response.blob()
}
```

**Backend reality:** PhotoExportController only has:
- `GET /stats`
- `GET /photos`
- `POST /export-all` (triggers Google Photos upload, doesn't return ZIP)
- `POST /export/{photoId}`
- `POST /import/{photoId}`
- `POST /import-all`
- `POST /verify/{photoId}`
- `POST /unlink/{photoId}`

**Result:** When users click "Export Photos" button in Data Management page, the request will fail with 404.

**Expected behavior:** The front-end should have the functionality of the 'main' branch which uses the backend endpoints.


---

### Bug #2: Missing Import/Export Endpoints Documentation
**Severity:** MEDIUM
**Location:** endpoints.md
**Issue:** endpoints.md does NOT document any of the import/export endpoints despite having a complete implementation.

**Missing documentation:**
- `GET /api/import/json` - Export database to JSON
- `POST /api/import/json` - Import database from JSON
- `GET /api/photo-export/stats` - Get photo export statistics
- `GET /api/photo-export/photos` - Get all photos with export status
- `POST /api/photo-export/export-all` - Export all photos to Google Photos
- `POST /api/photo-export/export/{photoId}` - Export single photo
- `POST /api/photo-export/import/{photoId}` - Import single photo from Google Photos
- `POST /api/photo-export/import-all` - Import all photos from Google Photos
- `POST /api/photo-export/verify/{photoId}` - Verify photo's permanent ID
- `POST /api/photo-export/unlink/{photoId}` - Unlink photo from Google Photos

**Similar bugs in other features:** Books from Feed endpoints are also missing from endpoints.md (see Bug #3).

---

### Bug #3: Missing Books from Feed Endpoints Documentation
**Severity:** MEDIUM
**Location:** endpoints.md
**Issue:** endpoints.md does NOT document any Books from Feed endpoints.

**Missing documentation:**
- `GET /api/books-from-feed/saved-books` - Get books needing processing
- `POST /api/books-from-feed/process-single/{bookId}` - Process single book with AI
- `POST /api/books-from-feed/process-saved` - Process all saved photos with AI
- `POST /api/books-from-feed/save-from-picker` - Save photos from Google Picker
- `POST /api/books-from-feed/picker-session` - Create picker session
- `GET /api/books-from-feed/picker-session/{sessionId}` - Get picker session status
- `GET /api/books-from-feed/picker-session/{sessionId}/media-items` - Get picker media items

---

### Bug #4: Incorrect Documentation About Photo Export in JSON
**Severity:** LOW
**Location:** feature-design-import-export.md
**Issue:** Documentation states photos are "NOT included in JSON export" but the explanation is incomplete.

**Current documentation (lines 22-24):**
> - **Photos are NOT included in JSON export**
> - Reason: Photos contain large binary data (image bytes) that would make the response too large
> - Photos should be managed separately via the Photo Export feature

**Actual implementation:** The ImportService.exportData() method (lines 512-555) explicitly sets `dto.setPhotos(null)` and has commented-out code that could export photo METADATA (not binary data). The comments explain:
- Photo metadata includes permanent IDs, captions, ordering
- Even metadata was disabled "per design spec" to keep exports "lightweight and fast"
- Binary image data was never going to be included

**What's misleading:** The docs imply the issue is only about binary data size, but actually even lightweight metadata is excluded by design.

**Better documentation would clarify:**
1. Photo binary data is never included (too large)
2. Photo metadata (IDs, captions, ordering) is also excluded by design as sub-objects of authors, books, etc
3. Photos can be reconnected during import via book/author matching
4. Use separate google Photo SYNC (export/import) for complete backup
5. The JSON export has a separate section for photo metadata.

---

### Bug #5: Missing Copyright Header
**Severity:** LOW
**Location:** ImportService.java
**Issue:** ImportService.java (line 1) is missing the copyright header.

**Found:** `package com.muczynski.library.service;`

**Expected:**
```java
/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
```

**Similar bugs in other features:** This was a common issue in previous code reviews and has been largely fixed.

---

## Code Not Matching Documentation

### Issue #1: Photo Export Feature Mismatch
**Location:** feature-design-import-export.md vs PhotoExportController.java
**Issue:** Documentation describes "Photo Export" as a backup/download system, but implementation is actually a Google Photos sync system.

**Documentation says (lines 47-63):**
- Purpose: "Separate system for backing up photos to Google Photos"
- Functionality: Download photos as ZIP for backup
- Endpoints: `/api/photo-export/**`

**Implementation reality:**
- PhotoExportController provides Google Photos SYNC functionality
- Can export TO Google Photos (upload)
- Can import FROM Google Photos (download)
- Can verify, unlink, get stats
- Does NOT provide ZIP download for local backup

**Impact:** The system is designed for cloud sync, not local backup. Clarify the documentation.

---

### Issue #2: Photo Metadata Export Disabled by Design
**Location:** feature-design-import-export.md vs ImportService.java
**Issue:** ImportService has commented-out code for photo metadata export with explanation "disabled per design spec" but docs don't mention this design decision clearly.

**Code (ImportService.java:518-552):**
```java
// IMPORTANT: Photos are NOT exported in JSON export (too large, even metadata can be significant)
// Photos should be managed separately via the Photo Export feature
// This ensures JSON exports remain lightweight and fast
// Photo metadata including permanent IDs, captions, and ordering is preserved in the database
// and will be reconnected during import via book/author matching

// The following code was used to export photo metadata, but has been disabled per design spec:
/* [commented out code for exporting photo DTOs] */

// Explicitly set photos to null to ensure they're not included in export
dto.setPhotos(null);
```

**Documentation (feature-design-import-export.md:22-24):**
Only says photos excluded because of "large binary data", doesn't mention metadata exclusion decision.
TODO: Clarify documentation as mentioned earlier.

---


### DTO Usage
**PhotoExportController:** Returns Map<String, Object> instead of DTOs ⚠️
- This violates the "always use DTOs" pattern from CLAUDE.md
- Should have PhotoStatsDto, PhotoInfoDto, etc.

---


## Recommendations

1. **HIGH PRIORITY:** Clarify documentation for photo sync (Export/import)
2. **MEDIUM PRIORITY:** Add all import/export endpoints to endpoints.md
4. **LOW PRIORITY:** Add copyright header to ImportService.java
5. **LOW PRIORITY:** Update feature-design-import-export.md to clarify photo metadata exclusion decision
6. **LOW PRIORITY:** Consider converting PhotoExportController to use DTOs instead of Map<String, Object>

---

