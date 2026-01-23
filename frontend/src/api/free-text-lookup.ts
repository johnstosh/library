// (c) Copyright 2025 by Muczynski
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface FreeTextLookupResultDto {
  bookId: number
  bookTitle: string
  authorName?: string
  success: boolean
  freeTextUrl?: string
  providerName?: string
  errorMessage?: string
  providersSearched: string[]
}

/**
 * Lookup free online text URLs for multiple books with progress tracking.
 * Processes books sequentially to provide definite progress indicator.
 *
 * @param onProgress - callback function called after each book is processed
 */
export function useLookupBulkFreeTextWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (bookIds: number[]) => {
      const results: FreeTextLookupResultDto[] = []
      const total = bookIds.length

      for (let i = 0; i < bookIds.length; i++) {
        const bookId = bookIds[i]
        try {
          const result = await api.post<FreeTextLookupResultDto>(
            `/free-text/lookup/${bookId}`
          )
          results.push(result)
          // Update the individual book cache
          queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(bookId) })
        } catch (error) {
          results.push({
            bookId,
            bookTitle: `Book ${bookId}`,
            success: false,
            errorMessage: error instanceof Error ? error.message : 'Unknown error',
            providersSearched: [],
          })
        }
        // Report progress after each book
        onProgress?.(i + 1, total)
      }

      return results
    },
    onSuccess: () => {
      // Invalidate all book queries to refresh the UI with new URLs
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
    },
  })
}

/**
 * Lookup free online text URLs for multiple books (bulk).
 * Searches across multiple providers like Project Gutenberg, Internet Archive, etc.
 * This version processes all books in a single request without progress updates.
 */
export function useLookupBulkFreeText() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookIds: number[]) =>
      api.post<FreeTextLookupResultDto[]>('/free-text/lookup-bulk', bookIds),
    onSuccess: () => {
      // Invalidate all book queries to refresh the UI with new URLs
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
    },
  })
}

/**
 * Get list of available free text providers.
 */
export async function getFreeTextProviders(): Promise<string[]> {
  return api.get<string[]>('/free-text/providers')
}
