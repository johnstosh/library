// (c) Copyright 2025 by Muczynski
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface WccLookupResultDto {
  bookId: number
  success: boolean
  audioAvailable?: boolean
  paperAvailable?: boolean
  ebookAvailable?: boolean
  matchedTitle?: string
  errorMessage?: string
  matches?: { recordKey: string; url?: string }[]
}

/**
 * Lookup Washtenaw Community College availability for a single book.
 */
export function useLookupSingleWcc() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookId: number) =>
      api.post<WccLookupResultDto>(`/wcc-lookup/lookup/${bookId}`, undefined, { requireAuth: false }),
    onSuccess: (_, bookId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(bookId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

/**
 * Lookup Washtenaw Community College availability for multiple books with progress tracking.
 * Processes books sequentially (via the single-book endpoint) to provide a definite
 * progress indicator, since there is no backend bulk/async job for this lookup.
 *
 * @param onProgress - callback function called after each book is processed
 */
export function useLookupBulkWccWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (bookIds: number[]) => {
      const results: WccLookupResultDto[] = []
      const total = bookIds.length

      for (let i = 0; i < bookIds.length; i++) {
        const bookId = bookIds[i]
        try {
          const result = await api.post<WccLookupResultDto>(`/wcc-lookup/lookup/${bookId}`, undefined, { requireAuth: false })
          results.push(result)
          queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(bookId) })
        } catch (error) {
          results.push({
            bookId,
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
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}
