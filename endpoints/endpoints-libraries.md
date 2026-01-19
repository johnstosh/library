# Branch Endpoints

## GET /api/branches/statistics
Returns statistics for all libraries including book count and active loans.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of LibraryStatisticsDto
```json
[
  {
    "branchId": 1,
    "branchName": "St. Martin de Porres",
    "bookCount": 150,
    "activeLoansCount": 12
  }
]
```

**Use Case:**
- Powers Branches page statistics display
- Shows book inventory and circulation at a glance

---

**Related:** BranchController.java, BranchService.java, BranchStatisticsDto.java, feature-design-libraries.md
