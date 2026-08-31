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

export interface GrokipediaLookupRequest {
  ids: number[]
  slow?: boolean
}

function grokipediaLookupPath(resource: 'books' | 'authors', slow?: boolean) {
  const base = `/${resource}/grokipedia-lookup-bulk`
  return slow ? `${base}?slow=true` : base
}

// Lookup Grokipedia URL for a single book
export function useLookupSingleBookGrokipedia() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ bookId, slow }: { bookId: number; slow?: boolean }) => {
      const results = await api.post<GrokipediaLookupResultDto[]>(
        grokipediaLookupPath('books', slow),
        [bookId]
      )
      return results[0]
    },
    onSuccess: (_, { bookId }) => {
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
    mutationFn: ({ ids, slow }: GrokipediaLookupRequest) =>
      api.post<GrokipediaLookupResultDto[]>(grokipediaLookupPath('books', slow), ids),
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
    mutationFn: async ({ ids, slow }: GrokipediaLookupRequest) => {
      const results: GrokipediaLookupResultDto[] = []
      const total = ids.length

      for (let i = 0; i < ids.length; i++) {
        const bookId = ids[i]
        try {
          const batch = await api.post<GrokipediaLookupResultDto[]>(
            grokipediaLookupPath('books', slow),
            [bookId]
          )
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
    mutationFn: ({ ids, slow }: GrokipediaLookupRequest) =>
      api.post<GrokipediaLookupResultDto[]>(grokipediaLookupPath('authors', slow), ids),
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
    mutationFn: async ({ ids, slow }: GrokipediaLookupRequest) => {
      const results: GrokipediaLookupResultDto[] = []
      const total = ids.length

      for (let i = 0; i < ids.length; i++) {
        const authorId = ids[i]
        try {
          const batch = await api.post<GrokipediaLookupResultDto[]>(
            grokipediaLookupPath('authors', slow),
            [authorId]
          )
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
