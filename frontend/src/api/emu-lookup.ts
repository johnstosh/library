// (c) Copyright 2025 by Muczynski
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface EmuLookupResultDto {
  bookId: number
  success: boolean
  audioAvailable?: boolean
  paperAvailable?: boolean
  ebookAvailable?: boolean
  matchedTitle?: string
  errorMessage?: string
}

/**
 * Lookup EMU Halle Library availability for a single book.
 */
export function useLookupSingleEmu() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookId: number) =>
      api.post<EmuLookupResultDto>(`/emu-lookup/lookup/${bookId}`),
    onSuccess: (_, bookId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(bookId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.availability() })
    },
  })
}

/**
 * Lookup EMU Halle Library availability for multiple books with progress tracking.
 * Processes books sequentially (via the single-book endpoint) to provide a definite
 * progress indicator, since there is no backend bulk/async job for this lookup.
 *
 * @param onProgress - callback function called after each book is processed
 */
export function useLookupBulkEmuWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (bookIds: number[]) => {
      const results: EmuLookupResultDto[] = []
      const total = bookIds.length

      for (let i = 0; i < bookIds.length; i++) {
        const bookId = bookIds[i]
        try {
          const result = await api.post<EmuLookupResultDto>(`/emu-lookup/lookup/${bookId}`)
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
