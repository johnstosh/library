// (c) Copyright 2025 by Muczynski
import React, { useMemo, useRef } from 'react'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookDto, BookSummaryDto, BulkDeleteResultDto, GenreLookupResultDto } from '@/types/dtos'

// Hook to get all books with optimized lastModified caching.
// All filtering is done client-side in BooksPage (via booksChips store).
// When selectedLabels are provided, the backend pre-filters to only books with those labels
// (AND logic), then client-side chips apply on top.
export function useBooks(selectedLabels?: string[]) {
  const queryClient = useQueryClient()
  const hasLabels = selectedLabels != null && selectedLabels.length > 0

  // When labels are active, fetch from /books/by-labels endpoint
  const labelEndpoint = hasLabels
    ? `/books/by-labels?labels=${encodeURIComponent((selectedLabels ?? []).join(','))}`
    : null

  // Step 1: Fetch summaries (ID + lastModified).
  // Always use /books/summaries for the unfiltered case; client-side chips handle all other filtering.
  const { data: summaries, isLoading: summariesLoading, isFetching: summariesFetching } = useQuery({
    queryKey: hasLabels
      ? queryKeys.books.labelSummaries(selectedLabels ?? [])
      : queryKeys.books.summaries(),
    queryFn: () => api.get<BookSummaryDto[]>(hasLabels ? labelEndpoint! : '/books/summaries'),
    staleTime: 30 * 1000, // 30 seconds: prevents duplicate fetches on rapid mounts/re-renders while keeping data reasonably fresh
    refetchOnMount: true, // Refetch on mount only if data is stale (older than staleTime)
    placeholderData: keepPreviousData, // Prevent summaries from becoming undefined during refetches
  })

  // Step 2: Determine which books need fetching based on cache
  const booksToFetch = useMemo(() => {
    if (!summaries) return []

    return summaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [summaries, queryClient])

  // Step 3: Batch fetch changed books using /books/by-ids
  const { data: fetchedBooks, isLoading: fetchingBooks, isFetching: byIdsFetching } = useQuery({
    queryKey: queryKeys.books.byIds(booksToFetch),
    queryFn: async () => {
      if (booksToFetch.length > 0) {
        // Only fetch books that changed
        return api.post<BookDto[]>('/books/by-ids', booksToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && booksToFetch.length > 0,
    placeholderData: keepPreviousData,
  })

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
    if (!summaries) return []

    // Build a map of newly fetched books for quick lookup
    const fetchedBooksMap = new Map<number, BookDto>()
    fetchedBooks?.forEach((book) => {
      fetchedBooksMap.set(book.id, book)
    })

    // Get books: prefer freshly fetched books, then fall back to cache
    const books = summaries
      .map((summary) => {
        // First check if we just fetched this book
        const fetched = fetchedBooksMap.get(summary.id)
        if (fetched) return fetched
        // Otherwise check cache (for books that didn't need refetching)
        return queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id))
      })
      .filter((book): book is BookDto => book !== undefined)

    // Sort by dateAddedToLibrary descending (most recent first)
    // This ensures consistent ordering regardless of cache state
    return books.sort((a, b) => {
      const dateA = a.dateAddedToLibrary ? new Date(a.dateAddedToLibrary).getTime() : 0
      const dateB = b.dateAddedToLibrary ? new Date(b.dateAddedToLibrary).getTime() : 0
      return dateB - dateA // Descending order (most recent first)
    })
  }, [summaries, queryClient, fetchedBooks])

  // Stabilize: prevent transient empty states from causing thumbnail disappearance.
  // During refetch cascades (e.g., 'online' event → summaries refetch → query key change),
  // allBooks can briefly become [] before new data arrives. The ref preserves the last
  // good data so the UI never flickers.
  const previousBooksRef = useRef<BookDto[]>([])
  React.useEffect(() => {
    if (allBooks.length > 0) {
      previousBooksRef.current = allBooks
    }
  }, [allBooks])

  const stableBooks = allBooks.length > 0 ? allBooks : previousBooksRef.current

  // isFetching is true for the ENTIRE duration of both network calls:
  // - summaries fetch (phase 1) AND by-ids fetch (phase 2) must both complete before hiding the indicator.
  const isFetching = summariesFetching || byIdsFetching

  return {
    data: stableBooks,
    isLoading: stableBooks.length === 0 && (summariesLoading || fetchingBooks),
    isFetching,
  }
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
      queryClient.invalidateQueries({ queryKey: queryKeys.books.list('3-letter-loc') })
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
      queryClient.invalidateQueries({ queryKey: queryKeys.books.list('3-letter-loc') })
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
      queryClient.invalidateQueries({ queryKey: queryKeys.books.list('3-letter-loc') })
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
      queryClient.invalidateQueries({ queryKey: queryKeys.books.list('3-letter-loc') })
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
      queryClient.invalidateQueries({ queryKey: queryKeys.books.list('3-letter-loc') })
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
export function useBulkBookFromImage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (ids: number[]) => {
      const results: { id: number; success: boolean; book?: BookDto; error?: string }[] = []
      for (const id of ids) {
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

// Hook to extract title and author from book's photo using AI
export function useTitleAuthorFromPhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.put<BookDto>(`/books/${id}/title-author-from-photo`),
    onSuccess: (data, id) => {
      queryClient.setQueryData(queryKeys.books.detail(id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
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
