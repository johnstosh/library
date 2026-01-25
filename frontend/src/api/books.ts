// (c) Copyright 2025 by Muczynski
import React, { useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookDto, BookSummaryDto, BulkDeleteResultDto, GenreLookupResultDto } from '@/types/dtos'

// Hook to get all books with optimized lastModified caching
export function useBooks(filter?: 'all' | 'most-recent' | 'without-loc' | '3-letter-loc' | 'without-grokipedia') {
  const queryClient = useQueryClient()

  // Determine the filter endpoint - all filter endpoints now return BookSummaryDto
  // For 'all' filter, we also use the summaries endpoint to enable caching
  const getFilterEndpoint = (f: typeof filter) => {
    switch (f) {
      case 'most-recent': return '/books/most-recent-day'
      case 'without-loc': return '/books/without-loc'
      case '3-letter-loc': return '/books/by-3letter-loc'
      case 'without-grokipedia': return '/books/without-grokipedia'
      case 'all':
      default: return '/books/summaries'
    }
  }

  // Step 1: Fetch summaries (ID + lastModified) from appropriate endpoint
  // For all filters including 'all', we use the summaries endpoint to enable caching
  const filterEndpoint = getFilterEndpoint(filter)
  const { data: summaries, isLoading: summariesLoading } = useQuery({
    queryKey: filter ? queryKeys.books.filterSummaries(filter) : queryKeys.books.summaries(),
    queryFn: () => api.get<BookSummaryDto[]>(filterEndpoint),
    staleTime: 0, // Always check for fresh data when filter changes
    refetchOnMount: true, // Always refetch when component mounts or filter changes
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
  // For all filters (including 'all'), we now use the optimized caching approach
  const { data: fetchedBooks, isLoading: fetchingBooks } = useQuery({
    queryKey: queryKeys.books.byIds(booksToFetch, filter),
    queryFn: async () => {
      if (booksToFetch.length > 0) {
        // Only fetch books that changed
        return api.post<BookDto[]>('/books/by-ids', booksToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && booksToFetch.length > 0,
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

    // Wait for fetchedBooks to complete if we have books to fetch
    if (booksToFetch.length > 0 && !fetchedBooks) {
      return []
    }

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
  }, [summaries, queryClient, fetchedBooks, booksToFetch])

  return {
    data: allBooks,
    isLoading: summariesLoading || fetchingBooks,
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

// Hook to lookup genres for a single book using Grok AI
export function useLookupGenres() {
  return useMutation({
    mutationFn: (id: number) => api.post<GenreLookupResultDto>(`/books/${id}/lookup-genres`),
  })
}

// Hook to lookup genres for multiple books using Grok AI
export function useLookupGenresBulk() {
  return useMutation({
    mutationFn: (ids: number[]) => api.post<GenreLookupResultDto[]>('/books/lookup-genres-bulk', ids),
  })
}
