# Library Endpoints

## GET /api/libraries/statistics
Returns statistics for all libraries including book count and active loans.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of LibraryStatisticsDto
```json
[
  {
    "libraryId": 1,
    "libraryName": "St. Martin de Porres",
    "bookCount": 150,
    "activeLoansCount": 12
  }
]
```

**Use Case:**
- Powers Libraries page statistics display
- Shows book inventory and circulation at a glance

---

**Related:** LibraryController.java, LibraryService.java, LibraryStatisticsDto.java, feature-design-libraries.md
