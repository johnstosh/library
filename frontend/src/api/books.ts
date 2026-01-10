// (c) Copyright 2025 by Muczynski
import React, { useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookDto, BookSummaryDto } from '@/types/dtos'

// Hook to get all books with optimized lastModified caching
export function useBooks(filter?: 'all' | 'most-recent' | 'without-loc' | '3-letter-loc' | 'without-grokipedia') {
  const queryClient = useQueryClient()

  // Step 1: Fetch summaries (ID + lastModified)
  const { data: summaries, isLoading: summariesLoading } = useQuery({
    queryKey: queryKeys.books.summaries(),
    queryFn: () => api.get<BookSummaryDto[]>('/books/summaries'),
    staleTime: Infinity, // Summaries never stale - we use them to detect changes
  })

  // Step 2: Determine which books need fetching
  const booksToFetch = useMemo(() => {
    if (!summaries) return []

    return summaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [summaries, queryClient])

  // Step 3: Batch fetch changed books
  const { data: fetchedBooks, isLoading: fetchingBooks } = useQuery({
    queryKey: queryKeys.books.list(filter),
    queryFn: async () => {
      // Apply filter on backend for special filters
      if (filter === 'most-recent') {
        return api.get<BookDto[]>('/books/most-recent-day')
      } else if (filter === 'without-loc') {
        return api.get<BookDto[]>('/books/without-loc')
      } else if (filter === '3-letter-loc') {
        return api.get<BookDto[]>('/books/by-3letter-loc')
      } else if (filter === 'without-grokipedia') {
        return api.get<BookDto[]>('/books/without-grokipedia')
      } else if (filter === 'all') {
        // Fetch all books directly for 'all' filter
        return api.get<BookDto[]>('/books')
      } else if (booksToFetch.length > 0) {
        // Only fetch books that changed
        return api.post<BookDto[]>('/books/by-ids', booksToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && (filter === 'most-recent' || filter === 'without-loc' || filter === '3-letter-loc' || filter === 'without-grokipedia' || filter === 'all' || booksToFetch.length > 0),
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

    // For filtered views (including 'all'), return the fetched results directly
    if (filter === 'most-recent' || filter === 'without-loc' || filter === '3-letter-loc' || filter === 'without-grokipedia' || filter === 'all') {
      return fetchedBooks || []
    }

    // For other views, get books from individual caches
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

// Hook to delete multiple books
export function useDeleteBooks() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) => api.post('/books/delete-bulk', ids),
    onSuccess: () => {
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

// Hook to generate book metadata from first photo using AI
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
