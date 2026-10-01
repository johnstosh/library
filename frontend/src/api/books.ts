// (c) Copyright 2025 by Muczynski
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { api } from './client'
import { postByIdsInBatches } from './byIds'
import { queryKeys } from '@/config/queryClient'
import { DEFAULT_PRICE_OLDER_DAYS, type BookChipFilters } from '@/utils/bookChipFilters'
import type {
  BookDto,
  BookSummaryDto,
  BulkDeleteResultDto,
  GenreLookupResultDto,
  ReadingDifficultyLookupResultDto,
} from '@/types/dtos'

/** Must match AskGrok.READING_DIFFICULTY_BATCH_SIZE. */
export const READING_DIFFICULTY_LOOKUP_BATCH_SIZE = 10

/** Books and Prices catalog filters. Omitted means the unfiltered loan-form catalog. */
export interface BookListFilters {
  q?: string
  labels?: readonly string[]
  statuses?: readonly string[]
  readingDifficulties?: readonly string[]
  bindings?: readonly string[]
  favoriteLists?: readonly string[]
  desireToPurchase?: readonly (string | number)[]
  priceOlderDays?: number
  chips: BookChipFilters
}

const BOOK_CHIP_API_KEYS: Record<keyof BookChipFilters, string> = {
  hasYdlAudio: 'hasYdlAudio',
  hasYdlBook: 'hasYdlBook',
  hasYdlEbook: 'hasYdlEbook',
  hasEmuAudio: 'hasEmuAudio',
  hasEmuBook: 'hasEmuBook',
  hasEmuEbook: 'hasEmuEbook',
  hasAclaAudio: 'hasAclaAudio',
  hasAclaBook: 'hasAclaBook',
  hasAclaEbook: 'hasAclaEbook',
  freeText: 'freeText',
  audio: 'audio',
  mostRecent: 'mostRecent',
  withoutGrokipedia: 'withoutGrokipedia',
  withGrokipedia: 'withGrokipedia',
  withoutGenres: 'withoutGenres',
  withoutFreeTextUrls: 'withoutFreeTextUrls',
  withoutProperPlotOrDescription: 'withoutProperPlotOrDescription',
  withPrices: 'withPrices',
  noPrices: 'noPrices',
  priceOlder: 'priceOlder',
  lookupErrors: 'lookupErrors',
}

function appendList(params: URLSearchParams, key: string, values?: readonly (string | number)[]) {
  if (values && values.length > 0) params.set(key, values.join(','))
}

/** Query string for GET /books/filtered-summaries. Names match the controller. */
export function bookListFilterQuery(filters: BookListFilters): string {
  const params = new URLSearchParams()
  const q = (filters.q ?? '').trim()
  if (q) params.set('q', q)
  appendList(params, 'labels', filters.labels)
  appendList(params, 'status', filters.statuses)
  appendList(params, 'readingDifficulty', filters.readingDifficulties)
  appendList(params, 'binding', filters.bindings)
  appendList(params, 'favoriteLists', filters.favoriteLists)
  appendList(params, 'desireToPurchase', filters.desireToPurchase)
  for (const key of Object.keys(BOOK_CHIP_API_KEYS) as (keyof BookChipFilters)[]) {
    if (filters.chips[key]) params.set(BOOK_CHIP_API_KEYS[key], 'true')
  }
  if (filters.chips.priceOlder) {
    const days = filters.priceOlderDays != null && filters.priceOlderDays >= 1
      ? filters.priceOlderDays
      : DEFAULT_PRICE_OLDER_DAYS
    params.set('priceOlderDays', String(days))
  }
  return params.toString()
}

function summaryAddedTime(summary: BookSummaryDto): number {
  if (!summary.dateAddedToLibrary) return Number.NEGATIVE_INFINITY
  const time = new Date(summary.dateAddedToLibrary).getTime()
  return Number.isNaN(time) ? Number.NEGATIVE_INFINITY : time
}

/** Most recently added first. Missing dates stay at the end. */
function orderedBookSummaries(summaries: BookSummaryDto[]): BookSummaryDto[] {
  return [...summaries].sort((a, b) => {
    const byDate = summaryAddedTime(b) - summaryAddedTime(a)
    if (byDate !== 0) return byDate
    return a.id - b.id
  })
}

export interface CatalogWindow {
  /** Matching summary rows. This is the list size, not how many full rows have been loaded. */
  total: number
  hasMore: boolean
  loadMore: () => void
  isLoadingMore: boolean
}

// Hook to get books with optimized lastModified caching.
// Pass filters for the Books and Prices pages (GET /books/filtered-summaries).
// Call with no argument for the unfiltered summary list (GET /books/summaries).
// pageSize limits full-row loads to that many matching books at a time.
// Full rows are loaded from /books/by-ids at most 100 ids at a time.
export function useBooks(filters?: BookListFilters, options?: { pageSize?: number }) {
  const queryClient = useQueryClient()
  const pageSize = options?.pageSize
  const filterQuery = filters ? bookListFilterQuery(filters) : ''
  const summariesEndpoint = filters
    ? (filterQuery ? `/books/filtered-summaries?${filterQuery}` : '/books/filtered-summaries')
    : '/books/summaries'
  const summariesQueryKey = filters
    ? queryKeys.books.filteredSummaries(filterQuery)
    : queryKeys.books.summaries()
  const windowKey = pageSize ? `${summariesEndpoint}` : ''
  const [loadedCount, setLoadedCount] = useState(pageSize ?? 0)
  const totalRef = useRef(0)
  const pendingRef = useRef(false)

  useEffect(() => {
    if (!pageSize) return
    pendingRef.current = false
    setLoadedCount(pageSize)
  }, [windowKey, pageSize])

  // Step 1: Fetch summaries (ID + lastModified).
  const {
    data: summaries,
    isLoading: summariesLoading,
    isFetching: summariesFetching,
    isPlaceholderData,
    error: summariesError,
  } = useQuery({
    queryKey: summariesQueryKey,
    queryFn: () => api.get<BookSummaryDto[]>(summariesEndpoint),
    staleTime: 30 * 1000, // 30 seconds: prevents duplicate fetches on rapid mounts/re-renders while keeping data reasonably fresh
    refetchOnMount: true, // Refetch on mount only if data is stale (older than staleTime)
    placeholderData: keepPreviousData, // Prevent summaries from becoming undefined during refetches
  })

  const summariesReady = summaries !== undefined && !isPlaceholderData
  if (summariesReady) totalRef.current = summaries.length

  const visibleSummaries = useMemo(() => {
    if (!summariesReady || !summaries) return []
    const ordered = pageSize ? orderedBookSummaries(summaries) : summaries
    return pageSize ? ordered.slice(0, loadedCount) : ordered
  }, [summaries, summariesReady, pageSize, loadedCount])

  // Step 2: Determine which visible books need fetching based on cache.
  const booksToFetch = useMemo(() => {
    return visibleSummaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [visibleSummaries, queryClient])

  // Step 3: Batch fetch changed books using /books/by-ids
  const { data: fetchedBooks, isLoading: fetchingBooks, isFetching: byIdsFetching, error: byIdsError } = useQuery({
    queryKey: queryKeys.books.byIds(booksToFetch),
    queryFn: () => postByIdsInBatches<BookDto>('/books/by-ids', booksToFetch),
    enabled: summariesReady && booksToFetch.length > 0,
    placeholderData: keepPreviousData,
  })

  useEffect(() => {
    pendingRef.current = byIdsFetching
  }, [byIdsFetching, loadedCount])

  // Populate individual book caches when books are fetched
  React.useEffect(() => {
    fetchedBooks?.forEach((book) => {
      queryClient.setQueryData(queryKeys.books.detail(book.id), book)
    })
  }, [fetchedBooks, queryClient])

  // Step 4: Get all books for display
  // IMPORTANT: We must use fetchedBooks directly here, not rely on the cache.
  // The cache is populated by a useEffect which runs AFTER this useMemo,
  // so reading from cache would return stale/missing data on first render.
  const allBooks = useMemo(() => {
    if (!summariesReady) return []

    // Build a map of newly fetched books for quick lookup
    const fetchedBooksMap = new Map<number, BookDto>()
    fetchedBooks?.forEach((book) => {
      fetchedBooksMap.set(book.id, book)
    })

    // Get books: prefer freshly fetched books, then fall back to cache.
    // Windowed lists keep summary order (most recent first). Unwindowed
    // callers still sort after the full rows are in hand.
    const books = visibleSummaries
      .map((summary) => {
        const fetched = fetchedBooksMap.get(summary.id)
        if (fetched) return fetched
        return queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id))
      })
      .filter((book): book is BookDto => book !== undefined)

    if (pageSize) return books
    return books.sort((a, b) => {
      const dateA = a.dateAddedToLibrary ? new Date(a.dateAddedToLibrary).getTime() : 0
      const dateB = b.dateAddedToLibrary ? new Date(b.dateAddedToLibrary).getTime() : 0
      return dateB - dateA
    })
  }, [summariesReady, visibleSummaries, queryClient, fetchedBooks, pageSize])

  // Stabilize: prevent transient empty states from causing thumbnail disappearance.
  // During refetch cascades (e.g., 'online' event → summaries refetch → query key change),
  // allBooks can briefly become [] before new data arrives. The ref preserves the last
  // good data so the UI never flickers. A settled empty summary list is a real result.
  const summariesSettledEmpty =
    summaries !== undefined && summaries.length === 0 && !summariesFetching && !byIdsFetching
  const previousBooksRef = useRef<BookDto[]>([])
  React.useEffect(() => {
    if (allBooks.length > 0) {
      previousBooksRef.current = allBooks
    } else if (summariesSettledEmpty) {
      previousBooksRef.current = []
    }
  }, [allBooks, summariesSettledEmpty])

  const stableBooks = allBooks.length > 0
    ? allBooks
    : summariesSettledEmpty
      ? []
      : previousBooksRef.current

  // isFetching is true for the ENTIRE duration of both network calls:
  // - summaries fetch (phase 1) AND by-ids fetch (phase 2) must both complete before hiding the indicator.
  const isLoadingMore = Boolean(pageSize) && byIdsFetching && loadedCount > pageSize!
  const isFetching = summariesFetching || (byIdsFetching && !isLoadingMore)
  const total = totalRef.current
  const hasMore = Boolean(pageSize) && summariesReady && loadedCount < total
  const loadMore = () => {
    if (!pageSize || !hasMore || summariesFetching || byIdsFetching || pendingRef.current) return
    pendingRef.current = true
    setLoadedCount((count) => count + pageSize)
  }

  return {
    data: stableBooks,
    total,
    hasMore,
    loadMore,
    isLoadingMore,
    isLoading: stableBooks.length === 0 && (summariesLoading || fetchingBooks),
    isFetching,
    error: summariesError || byIdsError,
  }
}

export function useBookCount() {
  return useQuery({
    queryKey: queryKeys.books.count(),
    queryFn: () => api.get<{ count: number }>('/books/count'),
    staleTime: 30 * 1000,
  })
}

// Hook to get a single book
export function useBook(id: number) {
  return useQuery({
    queryKey: queryKeys.books.detail(id),
    queryFn: () => api.get<BookDto>(`/books/${id}`),
    enabled: !!id,
  })
}

// Hook to create a book
export function useCreateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (book: Partial<BookDto>) => api.post<BookDto>('/books', book),
    onSuccess: () => {
      // Invalidate summaries to trigger re-fetch
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

// Hook to update a book
export function useUpdateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, book }: { id: number; book: Partial<BookDto> }) =>
      api.put<BookDto>(`/books/${id}`, book),
    onSuccess: (data, variables) => {
      // Update the detail cache and invalidate summaries
      queryClient.setQueryData(queryKeys.books.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

// Hook to delete a book
export function useDeleteBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/books/${id}`),
    onSuccess: (_, id) => {
      // Remove from cache and invalidate summaries
      queryClient.removeQueries({ queryKey: queryKeys.books.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

// Hook to delete multiple books (returns partial success result)
export function useDeleteBooks() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) => api.post<BulkDeleteResultDto>('/books/delete-bulk', ids),
    onSuccess: (result) => {
      // Remove deleted books from cache
      result.deletedIds.forEach((id) => {
        queryClient.removeQueries({ queryKey: queryKeys.books.detail(id) })
      })
      // Invalidate summaries to trigger re-fetch
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

// Hook to clone a book
export function useCloneBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.post<BookDto>(`/books/${id}/clone`),
    onSuccess: () => {
      // Invalidate summaries to trigger re-fetch
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

// Hook to suggest LOC call number using Grok AI
export function useSuggestLocNumber() {
  return useMutation({
    mutationFn: (params: { title: string; author?: string }) =>
      api.post<{ suggestion: string }>('/books/suggest-loc', params),
  })
}

// Hook to generate book metadata from photos using AI
export function useBookFromImage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.put<BookDto>(`/books/${id}/book-by-photo`),
    onSuccess: (data, id) => {
      // Update the detail cache and invalidate summaries
      queryClient.setQueryData(queryKeys.books.detail(id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to generate book metadata from first photo only using AI
export function useBookFromFirstPhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.put<BookDto>(`/books/${id}/book-from-first-photo`),
    onSuccess: (data, id) => {
      // Update the detail cache and invalidate summaries
      queryClient.setQueryData(queryKeys.books.detail(id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to generate book metadata from images for multiple books
export function useBulkBookFromImage(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (ids: number[]) => {
      const results: { id: number; success: boolean; book?: BookDto; error?: string }[] = []
      const total = ids.length
      for (let i = 0; i < ids.length; i++) {
        const id = ids[i]
        try {
          const book = await api.put<BookDto>(`/books/${id}/book-by-photo`)
          results.push({ id, success: true, book })
          // Update cache for each book as it's processed
          queryClient.setQueryData(queryKeys.books.detail(id), book)
        } catch (error) {
          results.push({
            id,
            success: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          })
        }
        onProgress?.(i + 1, total)
      }
      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

/**
 * Generate full book metadata (plot, description, author bio, etc.) from title and author
 * for multiple selected books. Follows pattern of useBulkBookFromImage but first fetches
 * current title/author from detail (to supply to backend endpoint), then calls PUT.
 * Updates cache per book. Progress reported sequentially.
 */
export function useBulkBookFromTitleAuthor(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (ids: number[]) => {
      const results: { id: number; success: boolean; book?: BookDto; error?: string }[] = []
      const total = ids.length
      for (let i = 0; i < ids.length; i++) {
        const id = ids[i]
        try {
          // GET current book detail to read title and author (per spec; could use cache but GET ensures fresh)
          const bookDetail = await api.get<BookDto>(`/books/${id}`)
          const title = bookDetail.title?.trim() || ''
          const authorName = (bookDetail.author || '').trim()

          if (!title) {
            throw new Error('Book has no title')
          }

          const updatedBook = await api.put<BookDto>(
            `/books/${id}/book-from-title-author`,
            { title, authorName }
          )
          results.push({ id, success: true, book: updatedBook })
          // Update cache immediately for UI reactivity
          queryClient.setQueryData(queryKeys.books.detail(id), updatedBook)
        } catch (error) {
          results.push({
            id,
            success: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          })
        }
        onProgress?.(i + 1, total)
      }
      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to lookup genres for a single book using Grok AI.
// On success, seeds the individual book cache with the returned BookDto so no follow-up
// by-ids fetch is needed. Callers are responsible for invalidating the summaries list once
// all lookups are complete (see BulkActionsToolbar and BookFormPage).
export function useLookupGenres() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.post<GenreLookupResultDto>(`/books/${id}/lookup-genres`),
    onSuccess: (data, id) => {
      if (data.updatedBook) {
        queryClient.setQueryData(queryKeys.books.detail(id), data.updatedBook)
      }
    },
  })
}

/**
 * Lookup genres for multiple books with progress tracking.
 * Processes books sequentially so the toolbar can show n/total.
 */
export function useLookupBulkGenresWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (ids: number[]) => {
      const results: GenreLookupResultDto[] = []
      const total = ids.length

      for (let i = 0; i < ids.length; i++) {
        const id = ids[i]
        try {
          const result = await api.post<GenreLookupResultDto>(`/books/${id}/lookup-genres`)
          results.push(result)
          if (result.updatedBook) {
            queryClient.setQueryData(queryKeys.books.detail(id), result.updatedBook)
          }
        } catch (error) {
          results.push({
            bookId: id,
            success: false,
            errorMessage: error instanceof Error ? error.message : String(error),
          })
        }
        onProgress?.(i + 1, total)
      }

      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Extract title and author from the book's photo using AI.
// The backend returns a preview DTO and does not persist the book. Do not write
// that preview into the book detail cache, or Cancel would keep the extracted
// title/author. A new author may have been created, so refresh the author list.
export function useTitleAuthorFromPhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.put<BookDto>(`/books/${id}/title-author-from-photo`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to generate full book metadata from title and author using AI
export function useBookFromTitleAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, title, authorName }: { id: number; title: string; authorName: string }) =>
      api.put<BookDto>(`/books/${id}/book-from-title-author`, { title, authorName }),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(queryKeys.books.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to lookup genres for multiple books using Grok AI
export function useLookupGenresBulk() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => api.post<GenreLookupResultDto[]>('/books/lookup-genres-bulk', ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

/**
 * Fill reading difficulty for multiple books with progress tracking.
 * Sends up to READING_DIFFICULTY_LOOKUP_BATCH_SIZE IDs per request.
 */
export function useLookupBulkReadingDifficultyWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (ids: number[]) => {
      const results: ReadingDifficultyLookupResultDto[] = []
      const total = ids.length

      for (let i = 0; i < ids.length; i += READING_DIFFICULTY_LOOKUP_BATCH_SIZE) {
        const batch = ids.slice(i, i + READING_DIFFICULTY_LOOKUP_BATCH_SIZE)
        try {
          const batchResults = await api.post<ReadingDifficultyLookupResultDto[]>(
            '/books/lookup-reading-difficulty-bulk',
            batch,
          )
          for (const result of batchResults) {
            results.push(result)
            if (result.updatedBook) {
              queryClient.setQueryData(queryKeys.books.detail(result.bookId), result.updatedBook)
            }
          }
        } catch (error) {
          for (const id of batch) {
            results.push({
              bookId: id,
              success: false,
              errorMessage: error instanceof Error ? error.message : String(error),
            })
          }
        }
        onProgress?.(Math.min(i + batch.length, total), total)
      }

      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}
