// (c) Copyright 2025 by Muczynski
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookPriceDto, BookPriceLookupResultDto } from '@/types/dtos'

/** Pause between books so AbeBooks does not throttle the Cloud Run IP. */
export const PRICE_LOOKUP_PAUSE_MS = 500

/** Extra waits before retrying a rate-limited book; then the rest of the batch is cancelled. */
export const PRICE_LOOKUP_BACKOFF_MS = [2000, 4000]

export const PRICE_LOOKUP_CANCELLED_MESSAGE = 'Cancelled: AbeBooks rate limited'

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

export async function lookupPricesForIds(
  bookIds: number[],
  lookup: (bookId: number) => Promise<BookPriceLookupResultDto>,
  onProgress?: (completed: number, total: number) => void,
  sleep: (ms: number) => Promise<void> = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
): Promise<BookPriceLookupResultDto[]> {
  const results: BookPriceLookupResultDto[] = []
  const total = bookIds.length

  for (let i = 0; i < bookIds.length; i++) {
    if (i > 0) {
      await sleep(PRICE_LOOKUP_PAUSE_MS)
    }
    const bookId = bookIds[i]
    let result = await lookupOne(bookId, lookup)
    for (let backoff = 0; result.rateLimited && backoff < PRICE_LOOKUP_BACKOFF_MS.length; backoff++) {
      await sleep(PRICE_LOOKUP_BACKOFF_MS[backoff])
      result = await lookupOne(bookId, lookup)
    }
    results.push(result)
    if (result.rateLimited) {
      for (let j = i + 1; j < bookIds.length; j++) {
        results.push({
          bookId: bookIds[j],
          success: false,
          cancelled: true,
          errorMessage: PRICE_LOOKUP_CANCELLED_MESSAGE,
        })
      }
      onProgress?.(total, total)
      return results
    }
    onProgress?.(i + 1, total)
  }

  return results
}

async function lookupOne(
  bookId: number,
  lookup: (bookId: number) => Promise<BookPriceLookupResultDto>,
): Promise<BookPriceLookupResultDto> {
  try {
    return await lookup(bookId)
  } catch (error) {
    return {
      bookId,
      success: false,
      errorMessage: error instanceof Error ? error.message : 'Unknown error',
    }
  }
}

/**
 * Lookup AbeBooks prices for multiple books sequentially so the carousel
 * can show definite progress. Pauses between books; if AbeBooks rate-limits,
 * backs off, then cancels the rest of the batch.
 */
export function useLookupBulkPricesWithProgress(
  onProgress?: (completed: number, total: number) => void,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookIds: number[]) =>
      lookupPricesForIds(
        bookIds,
        (bookId) => api.post<BookPriceLookupResultDto>(`/prices/lookup/${bookId}`),
        onProgress,
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.prices.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}
