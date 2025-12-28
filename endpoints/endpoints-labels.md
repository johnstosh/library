# Book Label Endpoints

## GET /api/labels/generate
Generates and downloads book pocket labels PDF for selected books.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Query Parameters:**
- `bookIds` (array of Long, required) - Book IDs to generate labels for

**Example:**
```
GET /api/labels/generate?bookIds=1&bookIds=2&bookIds=3
```

**Response:** PDF file (application/pdf) with download headers
- Content-Type: application/pdf
- Content-Disposition: attachment; filename="book-labels.pdf"

**Label Details:**
- Format: Avery 6572 book pocket labels
- Contains: Book title, author, LOC call number formatted for spine
- Only books with LOC call numbers are included
- Generated using iText 8
- Custom LOC formatting via `formatLocForSpine()` utility

**Error Responses:**
- 401: User not authenticated
- 403: User does not have LIBRARIAN authority
- 400: No bookIds provided or invalid bookIds
- 500: PDF generation failed

**Use Case:**
- Librarians generate labels for newly cataloged books
- Accessible via "Generate Labels" button in Books page bulk actions toolbar
- Select multiple books and generate labels in one PDF

---

**Related:** LabelsController.java, LabelsPdfService.java, feature-design-loc.md
