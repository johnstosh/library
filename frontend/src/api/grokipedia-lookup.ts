// (c) Copyright 2025 by Muczynski
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface GrokipediaLookupResultDto {
  bookId?: number
  authorId?: number
  name: string
  success: boolean
  grokipediaUrl?: string
  errorMessage: string
}

// Lookup Grokipedia URL for a single book
export function useLookupSingleBookGrokipedia() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (bookId: number) => {
      const results = await api.post<GrokipediaLookupResultDto[]>('/books/grokipedia-lookup-bulk', [bookId])
      return results[0]
    },
    onSuccess: (_, bookId) => {
      // Invalidate the specific book query
      queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(bookId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Lookup Grokipedia URLs for multiple books (bulk)
export function useLookupBulkBooksGrokipedia() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookIds: number[]) =>
      api.post<GrokipediaLookupResultDto[]>('/books/grokipedia-lookup-bulk', bookIds),
    onSuccess: () => {
      // Invalidate all book queries
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

/**
 * Lookup Grokipedia URLs for multiple books with progress tracking.
 * Processes books sequentially so the toolbar can show n/total.
 */
export function useLookupBulkBooksGrokipediaWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (bookIds: number[]) => {
      const results: GrokipediaLookupResultDto[] = []
      const total = bookIds.length

      for (let i = 0; i < bookIds.length; i++) {
        const bookId = bookIds[i]
        try {
          const batch = await api.post<GrokipediaLookupResultDto[]>('/books/grokipedia-lookup-bulk', [bookId])
          results.push(batch[0] ?? {
            bookId,
            name: `Book ${bookId}`,
            success: false,
            errorMessage: 'No result returned',
          })
          queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(bookId) })
        } catch (error) {
          results.push({
            bookId,
            name: `Book ${bookId}`,
            success: false,
            errorMessage: error instanceof Error ? error.message : 'Unknown error',
          })
        }
        onProgress?.(i + 1, total)
      }

      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
    },
  })
}

// Lookup Grokipedia URLs for multiple authors (bulk)
export function useLookupBulkAuthorsGrokipedia() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (authorIds: number[]) =>
      api.post<GrokipediaLookupResultDto[]>('/authors/grokipedia-lookup-bulk', authorIds),
    onSuccess: () => {
      // Invalidate all author queries
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
    },
  })
}

/**
 * Lookup Grokipedia URLs for multiple authors with progress tracking.
 * Processes authors sequentially so the toolbar can show n/total.
 */
export function useLookupBulkAuthorsGrokipediaWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (authorIds: number[]) => {
      const results: GrokipediaLookupResultDto[] = []
      const total = authorIds.length

      for (let i = 0; i < authorIds.length; i++) {
        const authorId = authorIds[i]
        try {
          const batch = await api.post<GrokipediaLookupResultDto[]>('/authors/grokipedia-lookup-bulk', [authorId])
          results.push(batch[0] ?? {
            authorId,
            name: `Author ${authorId}`,
            success: false,
            errorMessage: 'No result returned',
          })
          queryClient.invalidateQueries({ queryKey: queryKeys.authors.detail(authorId) })
        } catch (error) {
          results.push({
            authorId,
            name: `Author ${authorId}`,
            success: false,
            errorMessage: error instanceof Error ? error.message : 'Unknown error',
          })
        }
        onProgress?.(i + 1, total)
      }

      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
    },
  })
}
