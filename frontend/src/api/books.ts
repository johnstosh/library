// (c) Copyright 2025 by Muczynski
import React, { useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookDto, BookSummaryDto, BulkDeleteResultDto } from '@/types/dtos'

// Hook to get all books with optimized lastModified caching
export function useBooks(filter?: 'all' | 'most-recent' | 'without-loc' | '3-letter-loc' | 'without-grokipedia') {
  const queryClient = useQueryClient()

  // Determine the filter endpoint - all filter endpoints now return BookSummaryDto
  const getFilterEndpoint = (f: typeof filter) => {
    switch (f) {
      case 'most-recent': return '/books/most-recent-day'
      case 'without-loc': return '/books/without-loc'
      case '3-letter-loc': return '/books/by-3letter-loc'
      case 'without-grokipedia': return '/books/without-grokipedia'
      default: return '/books/summaries'
    }
  }

  // Step 1: Fetch summaries (ID + lastModified) from appropriate endpoint
  const filterEndpoint = getFilterEndpoint(filter)
  const { data: summaries, isLoading: summariesLoading } = useQuery({
    queryKey: filter ? queryKeys.books.filterSummaries(filter) : queryKeys.books.summaries(),
    queryFn: () => api.get<BookSummaryDto[]>(filterEndpoint),
    staleTime: Infinity, // Summaries never stale - we use them to detect changes
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
  const { data: fetchedBooks, isLoading: fetchingBooks } = useQuery({
    queryKey: queryKeys.books.byIds(booksToFetch, filter),
    queryFn: async () => {
      if (filter === 'all') {
        // For 'all' filter, fetch all books directly (no caching optimization)
        return api.get<BookDto[]>('/books')
      } else if (booksToFetch.length > 0) {
        // Only fetch books that changed
        return api.post<BookDto[]>('/books/by-ids', booksToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && (filter === 'all' || booksToFetch.length > 0),
  })

  // Populate individual book caches when books are fetched
  React.useEffect(() => {
    fetchedBooks?.forEach((book) => {
      queryClient.setQueryData(queryKeys.books.detail(book.id), book)
    })
  }, [fetchedBooks, queryClient])

  // Step 4: Get all cached books for display
  const allBooks = useMemo(() => {
    if (!summaries) return []

    // For 'all' filter, return the fetched results directly
    if (filter === 'all') {
      return fetchedBooks || []
    }

    // For filtered views and default, get books from individual caches
    return summaries
      .map((summary) => queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id)))
      .filter((book): book is BookDto => book !== undefined)
  }, [summaries, queryClient, fetchedBooks, filter])

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
