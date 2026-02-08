# Grok-4 Thumbnail Disappearance Analysis

## All Bugs and Race Conditions Causing Thumbnails to Disappear

After thoroughly analyzing the provided code, architecture, and considerations (including React render cycles, TanStack Query behaviors, useMemo/useEffect interactions, refetch mechanics, query key changes, and state transitions), I've identified **all** bugs and race conditions that could cause book thumbnails to disappear after initially loading. "Disappear" here means the thumbnails are visible (loaded and displayed via `<img src={loadedUrl} />`) but then vanish—either because the `<img>` src becomes invalid (e.g., revoked blob URL), the component shows a loading/fallback state (e.g., spinner or "-"), or the entire row/table unmounts (removing the `ThrottledThumbnail` components).

I focused on scenarios where this happens **after initial loading** (e.g., due to refetches, prop changes, or state transitions), not just on initial mount. Bugs are grouped by category, with explanations of how they cause disappearance, relevant code snippets, and why they align with the hints (e.g., stale closures, query key changes, refetches with staleTime:0).

### 1. Bugs in useBooks Hook Leading to Temporary allBooks.length === 0 or Partial allBooks, Triggering isLoading=true and Table Unmount

These cause the DataTable to show a spinner (unmounting all rows and `ThrottledThumbnail` components), making **all** thumbnails disappear temporarily. This happens after initial loading if summaries refetch (common due to staleTime:0) and detect changes (e.g., new/updated books on server), altering booksToFetch.

#### Bug: Query Key for byIds Includes booksToFetch (Changing Keys on ID Set Changes) Combined with placeholderData: keepPreviousData Leads to Incorrect fetchedBooks During Transitions

- **Description**: The byIds query key (['books', 'byIds', ids.join(','), filter]) changes whenever booksToFetch changes (e.g., from [1,2,3,4,5] to [6,7] or []). This switches the useQuery to a new cache entry. With placeholderData: keepPreviousData (assumed to be `(prev) => prev`), fetchedBooks temporarily becomes the **previous** data (from the old key's IDs, unrelated to current booksToFetch).
  - In allBooks useMemo, the condition `if (booksToFetch.length > 0 && !fetchedBooks) return []` **does not trigger** because !fetchedBooks is false ([] is truthy; unrelated data is truthy).
  - It proceeds to build fetchedBooksMap from the **wrong/unrelated** fetchedBooks (old IDs).
  - For current summaries:
    - If map.get(summary.id) finds a matching ID (rare, since IDs differ), it uses a wrong book.
    - Else, falls back to queryClient.getQueryData (cache).
    - If the current booksToFetch IDs have no cache (e.g., new books) or outdated cache (mismatched lastModified), cache may return undefined or old data.
  - Result: Some books are undefined, filtered out via `.filter((book): book is BookDto => book !== undefined)`.
  - If enough are filtered (e.g., all are new books with no cache), allBooks.length === 0.
- **How Thumbnails Disappear**: If allBooks.length === 0 && (summariesLoading || fetchingBooks) (fetchingBooks=true during fetch), isLoading=true. DataTable unmounts all rows/thumbnails, showing spinner. Thumbnails vanish until fetch completes.
- **Why After Loading?** Initial load works, but post-load summaries refetch (staleTime:0 + refetchOnMount:true or window focus) can change lastModified or add new summaries, altering booksToFetch. This triggers key change + wrong placeholder → temporary length=0 → unmount.

#### Bug: allBooks Computation Filters Out Undefined Books, Causing Partial Lists Even If length >0 (Missing Rows)

- **Description**: When booksToFetch changes (key change, as above), and fetchedBooks is wrong/unrelated placeholder, allBooks filters out undefined books (no map entry + no/outdated cache). Even if length >0 (some cached books succeed), the list is partial (missing new/updated books).
- **How Thumbnails Disappear**: DataTable renders fewer rows (missing books = missing `ThrottledThumbnail`s). Thumbnails for missing rows vanish until fetch completes.

#### Bug: allBooks Returns [] When booksToFetch.length >0 && !fetchedBooks, Preventing Partial Display of Cached Books

- **Description**: This condition forces allBooks=[] during fetches, even if some books are cached/up-to-date (not in booksToFetch).
- **How Thumbnails Disappear**: Forces length=0 → isLoading=true → table unmount. Without keepPreviousData, fetchedBooks=undefined on key change, triggering this reliably.

### 2. Bugs in ThrottledThumbnail Leading to Revocation of Blob URLs or Reset to Loading State

These cause individual thumbnails to disappear (e.g., img breaks or shows loading/fallback) without unmounting the table. Common if book data updates (new url/checksum) or during refetches (new book objects).

#### Bug: Stale Closure in useEffect Cleanup Captures Old loadedUrl, Leading to Incorrect Revocation (Including Shared Cached Blob URLs)

- **Description**: The cleanup closure captures loadedUrl from the render when useEffect ran (before any setLoadedUrl in the body). It often captures null/old values, but in prop change scenarios, it can capture a cached blobUrl even if new props intend revocation.
- **How Thumbnails Disappear**: Revoking a shared cachedBlobUrl invalidates src for **all** `ThrottledThumbnail`s using it (browser can't load revoked blob). Affected thumbnails vanish suddenly (broken img) while components remain mounted.

#### Bug: Deps Change Resets to Loading State (setLoadedUrl(null) + isLoading=true), Causing Temporary Disappearance

- **Description**: If [photoId, url, checksum] change (e.g., book update from refetch gives new url/checksum), effect runs: old cleanup (may revoke wrong), then setLoadedUrl(null) + setIsLoading(true) + queue. Renders loading (animate-pulse) or fallback ("-").
- **How Thumbnails Disappear**: Thumbnail temporarily shows loading/fallback instead of img (disappears until queue resolves).

### 3. Other Contributing Factors (Not Direct Bugs but Amplify Disappearance)

- **Frequent Summaries Refetches (staleTime:0 + refetchOnMount:true)**: Always stale, refetches on mount/focus, detecting changes → triggers above bugs.
- **DataTable Unmounts Everything on isLoading=true**: Amplifies hook bugs by unmounting all thumbnails at once.
- **clearThumbnailQueue on BooksPage Unmount**: Rejects pending queues. If table remounts (from isLoading toggle), thumbnails reload from scratch.
- **No staleTime on byIds Query**: Defaults to 0, but since enabled conditionally, minor.

### Recommended Fixes

1. Add `placeholderData: keepPreviousData` to summaries query to prevent summaries from becoming undefined during refetches
2. Remove the guard `if (booksToFetch.length > 0 && !fetchedBooks) return []` that prevents showing cached books during transitions
3. Use a `useRef` to stabilize the books array — preserve the last good data during transient empty states
