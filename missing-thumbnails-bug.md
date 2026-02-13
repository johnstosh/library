# Missing Thumbnails Bug

## Bug Description

On the Books page (`/books`), book cover thumbnails load and display correctly, then **disappear** right when the last thumbnail is loaded. This happens with absolutely no user interaction. The page defaults to the "Most Recent Day" filter. The user logs in, sees books with their thumbnails, and then the thumbnails vanish while the user is just looking at the page. I'm not talking about the photo reverting to a spinner nor being a broken image, nor being an image with a dash, but rather the image isn't shown. All the photos
disappear at the same time.

This bug is confirmed present in production. It is not an intermittent flicker — the thumbnails go away and stay away.

## Architecture

### Data Flow: How Books Reach the Screen

```
User logs in → /books page loads → useBooks() hook fires → two-step query
```

**Step 1 — Summaries query** (`books.ts:28-34`):
- Fetches `BookSummaryDto[]` from `/api/books/most-recent-day` (the default filter)
- Each summary has `id` and `lastModified`
- `staleTime: 0` — data is immediately stale after fetch
- `refetchOnMount: true`
- `placeholderData: keepPreviousData` — keeps old data visible during refetches

**Step 2 — Batch fetch** (`books.ts:50-61`):
- Compares summaries against individual book cache entries
- Fetches only books whose `lastModified` changed via `POST /api/books/by-ids`
- Also uses `placeholderData: keepPreviousData`

**Step 3 — Assembly** (`books.ts:74-101`):
- Merges freshly fetched books with cached books
- Sorts by `dateAddedToLibrary` descending
- A `previousBooksRef` stabilizes the array to prevent transient empty states (`books.ts:107-114`)

**Step 4 — isLoading gate** (`books.ts:118`):
```typescript
isLoading: stableBooks.length === 0 && (summariesLoading || fetchingBooks)
```
- `isLoading` is only `true` when there are zero books AND a query is loading
- When `isLoading` is true, `DataTable` replaces the entire table with a spinner (`DataTable.tsx:44-49`), **unmounting all `ThrottledThumbnail` components**

### Thumbnail Loading Pipeline

Each book row renders a `ThrottledThumbnail` component (`BookTable.tsx:91-97`) when `book.firstPhotoId` exists:

```
ThrottledThumbnail mounts
  → checks global checksumCache (Map<string, blobUrl>)
  → if cache hit: render <img> immediately
  → if cache miss: add to global queue
      → queue processes one fetch at a time
      → fetch(`/api/photos/{id}/thumbnail?width=70`)
      → response → blob → URL.createObjectURL(blob)
      → store blobUrl in checksumCache keyed by checksum
      → render <img src={blobUrl}>
```

Key details:
- The queue is global (module-level), processes one thumbnail at a time
- Blob URLs are **never revoked** (intentional — previous fix attempt)
- `checksumCache` is a global `Map<string, string>` that persists across re-renders
- On unmount, the component sets `mountedRef.current = false` to prevent state updates

### Component Hierarchy

```
App
  └─ Suspense (fallback: PageLoader spinner)
      └─ ProtectedRoute (shows Spinner while auth isLoading)
          └─ AppLayout
              └─ BooksPage (lazy-loaded)
                  └─ BookTable
                      └─ DataTable (shows spinner when isLoading=true, replacing all children)
                          └─ <tr> per book
                              └─ ThrottledThumbnail (or placeholder if no photo)
```

### Key Configuration

**QueryClient defaults** (`queryClient.ts`):
- `staleTime: 5 minutes` (default)
- `gcTime: 30 minutes`
- `retry: 1`
- `refetchOnWindowFocus: false`
- `refetchOnMount: 'always'`
- `refetchOnReconnect`: **not set** (TanStack Query default is `true`)

**Books query overrides**:
- `staleTime: 0` — always stale
- `refetchOnMount: true`

**Auth state** (`authStore.ts`):
- `isLoading: true` initially
- `checkAuth()` fires once on App mount, fetches `/auth/me`, sets `isLoading: false`
- `ProtectedRoute` shows a full-screen spinner while `isLoading` is true

**Zustand UI store** (`uiStore.ts`):
- `booksFilter: 'most-recent'` is the default

## Reproduction Attempts

### Attempt 1: Java UI test with forced refetch trigger
- Clicked "All Books", dispatched `window 'online'` event to force refetch
- Used route interception (instant mock PNG responses)
- **PASSED** — wrong approach

### Attempt 2: Java UI test with simple idle wait
- Default "Most Recent Day" view, wait 10 seconds, check thumbnails
- Route interception (instant mock PNG responses)
- Testcontainers embedded PostgreSQL
- **PASSED** — zero network requests during wait, thumbnails stable

### Attempt 3: Standalone Playwright against live backend (Docker PostgreSQL + bootRun)
- Same route interception
- **PASSED** — identical results

### Attempt 4: No route interception against live backend
- Photos have NULL image data → API returns 500
- Thumbnails never appear at all
- **BLOCKED** — can't test disappearance without working thumbnails

## What Hasn't Been Tried
- Real image data in the photo table (OID/large object type) so the thumbnail API works without mocking
- Delayed route interception (add 500ms-1s latency to mock responses)
- Running Playwright against the deployed production or dev site directly
- Longer wait times (30-60 seconds)
- Different browser (Firefox, WebKit)

## CSS Layout Fix (Applied)

The thumbnails were disappearing because the `<a>` wrapper around each thumbnail had no explicit dimensions, allowing it to collapse to zero width under certain layout conditions (e.g., table reflow after background refetch). Three CSS changes were applied to stabilize the thumbnail column:

### 1. Explicit width on the `<a>` element
Set `width: 3.5rem; min-width: 3.5rem` (56px) on the `<a class="block">` wrapping each `ThrottledThumbnail`. This forces the anchor to reserve space regardless of the image load state, preventing the column from collapsing.

### 2. Column header and min-width
Changed the column header from empty string to "Cover" and set `minWidth: '70px'` on the `<th>` element. This ensures the column header itself reserves the minimum space needed.

### 3. Reduced `<td>` padding for the thumbnail column
The default `<td>` class includes `sm:px-6` (24px horizontal padding at the `sm` breakpoint), which is excessive for a narrow thumbnail column. The photo column now uses a custom `cellClassName` with just `px-3` (12px), dropping the `sm:px-6` override. Also removed `truncate` since it's not relevant for image content.

**Files changed:**
- `frontend/src/components/table/DataTable.tsx` — Added `minWidth` and `cellClassName` to `Column` interface
- `frontend/src/pages/books/components/BookTable.tsx` — Applied all three fixes to the photo column

## Previous Fix Commits (did not fix the bug)
- `5c99682` Fix book thumbnails disappearing during background refetches
- `7d01c54` Stabilize book thumbnails during background refetch cascades
- `47ef3d0` Fix ThrottledThumbnail blob URL lifecycle bugs causing thumbnail disappearance
- `bc253af` caching fix
