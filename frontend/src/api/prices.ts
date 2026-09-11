// (c) Copyright 2025 by Muczynski
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookPriceDto, BookPriceLookupResultDto } from '@/types/dtos'

export function usePrices() {
  return useQuery({
    queryKey: queryKeys.prices.list(),
    queryFn: () => api.get<BookPriceDto[]>('/prices'),
  })
}

export function useLookupSinglePrice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookId: number) =>
      api.post<BookPriceLookupResultDto>(`/prices/lookup/${bookId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.prices.all })
    },
  })
}

/**
 * Lookup AbeBooks prices for multiple books sequentially so the carousel
 * can show definite progress.
 */
export function useLookupBulkPricesWithProgress(
  onProgress?: (completed: number, total: number) => void,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (bookIds: number[]) => {
      const results: BookPriceLookupResultDto[] = []
      const total = bookIds.length

      for (let i = 0; i < bookIds.length; i++) {
        const bookId = bookIds[i]
        try {
          const result = await api.post<BookPriceLookupResultDto>(`/prices/lookup/${bookId}`)
          results.push(result)
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
      queryClient.invalidateQueries({ queryKey: queryKeys.prices.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}
