// (c) Copyright 2025 by Muczynski
import React, { useMemo, useRef } from 'react'
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookPriceDto, BookPriceLookupResultDto, BookSummaryDto } from '@/types/dtos'

/** Pause between books so AbeBooks does not throttle the Cloud Run IP. */
export const PRICE_LOOKUP_PAUSE_MS = 1000

/** Every 10th lookup waits longer than the usual pause. */
export const PRICE_LOOKUP_TENTH_PAUSE_MS = 10000

export function priceLookupPauseMs(oneBasedIndex: number): number {
  return oneBasedIndex % 10 === 0 ? PRICE_LOOKUP_TENTH_PAUSE_MS : PRICE_LOOKUP_PAUSE_MS
}

/** Extra waits before retrying a rate-limited book; then the rest of the batch is cancelled. */
export const PRICE_LOOKUP_BACKOFF_MS = [4000, 8000]

export const PRICE_LOOKUP_CANCELLED_MESSAGE = 'Cancelled: AbeBooks rate limited'

// Hook to get prices with optimized lastModified caching (mirrors useBooks).
// Uses GET /api/prices/summaries + POST /api/prices/by-ids for only changed rows when no bookIds provided.
// When bookIds provided, uses POST /api/prices/by-book-ids (scoped to candidate books after non-price filters).
// Populates detail caches in both paths. Existing list/summaries path kept for full-catalog cases.
export function usePrices(options?: { enabled?: boolean; bookIds?: number[] | undefined }) {
  const queryClient = useQueryClient()

  const isScoped = options?.bookIds !== undefined
  const bookIds = options?.bookIds ?? []

  // Step 1: summaries only for full-catalog (unscoped) path; scoped skips summaries+cache logic.
  const { data: summaries, isLoading: summariesLoading, isFetching: summariesFetching, error: summariesError } = useQuery({
    queryKey: queryKeys.prices.summaries(),
    queryFn: () => api.get<BookSummaryDto[]>('/prices/summaries'),
    staleTime: 30 * 1000,
    refetchOnMount: true,
    placeholderData: keepPreviousData,
    enabled: (options?.enabled ?? true) && !isScoped,
  })

  // Scoped path uses direct /by-book-ids (no summaries/cache for simplicity; still seeds detail cache).
  // Unscoped uses full caching path.
  const pricesToFetch = useMemo(() => {
    if (isScoped || !summaries) return []
    return summaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<BookPriceDto>(queryKeys.prices.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [summaries, queryClient, isScoped])

  const { data: fetchedPrices, isLoading: fetchingPrices, isFetching: byIdsFetching, error: byIdsError } = useQuery({
    queryKey: isScoped
      ? [...queryKeys.prices.all, 'byBookIds', bookIds.join(',')]
      : queryKeys.prices.byIds(pricesToFetch),
    queryFn: async () => {
      if (isScoped) {
        if (bookIds.length === 0) return []
        return api.post<BookPriceDto[]>('/prices/by-book-ids', bookIds)
      }
      if (pricesToFetch.length > 0) {
        return api.post<BookPriceDto[]>('/prices/by-ids', pricesToFetch)
      }
      return []
    },
    enabled: (options?.enabled ?? true) && (isScoped ? true : summaries !== undefined && pricesToFetch.length > 0),
    placeholderData: keepPreviousData,
  })

  // Populate individual price detail caches (works for both scoped and full-catalog paths)
  React.useEffect(() => {
    fetchedPrices?.forEach((price) => {
      queryClient.setQueryData(queryKeys.prices.detail(price.id), price)
    })
  }, [fetchedPrices, queryClient])

  // Build final list: for scoped use fetchedPrices directly; for full-catalog prefer fetched + cache.
  const allPrices = useMemo(() => {
    if (isScoped) {
      return (fetchedPrices ?? []).sort((a, b) => a.id - b.id)
    }

    if (!summaries) return []

    const fetchedPricesMap = new Map<number, BookPriceDto>()
    fetchedPrices?.forEach((price) => {
      fetchedPricesMap.set(price.id, price)
    })

    const prices = summaries
      .map((summary) => {
        const fetched = fetchedPricesMap.get(summary.id)
        if (fetched) return fetched
        return queryClient.getQueryData<BookPriceDto>(queryKeys.prices.detail(summary.id))
      })
      .filter((price): price is BookPriceDto => price !== undefined)

    // Stable sort by id (prices have no natural date ordering like books)
    return prices.sort((a, b) => a.id - b.id)
  }, [summaries, queryClient, fetchedPrices, isScoped])

  // Stabilize to prevent transient empty states / flicker during refetches
  const previousPricesRef = useRef<BookPriceDto[]>([])
  React.useEffect(() => {
    if (allPrices.length > 0) {
      previousPricesRef.current = allPrices
    }
  }, [allPrices])

  const stablePrices = allPrices.length > 0 ? allPrices : previousPricesRef.current

  const isFetching = summariesFetching || byIdsFetching
  const isLoading = stablePrices.length === 0 && (summariesLoading || fetchingPrices)

  return {
    data: stablePrices,
    isLoading,
    isFetching,
    error: summariesError || byIdsError,
  }
}

export function useLookupSinglePrice() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookId: number) =>
      api.post<BookPriceLookupResultDto>(`/prices/lookup/${bookId}`),
    onSuccess: () => {
      // Invalidate summaries (and details via cache population) so next load picks up changes
      queryClient.invalidateQueries({ queryKey: queryKeys.prices.summaries() })
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
      await sleep(priceLookupPauseMs(i + 1))
    }
    const bookId = bookIds[i]
    console.info(`Price lookup ${i + 1}/${total} bookId=${bookId}`)
    let result = await lookupOne(bookId, lookup)
    for (let backoff = 0; result.rateLimited && backoff < PRICE_LOOKUP_BACKOFF_MS.length; backoff++) {
      console.warn(
        `Price lookup rate-limited bookId=${bookId}; retry backoff ${PRICE_LOOKUP_BACKOFF_MS[backoff]} ms`,
      )
      await sleep(PRICE_LOOKUP_BACKOFF_MS[backoff])
      result = await lookupOne(bookId, lookup)
    }
    results.push(result)
    if (result.rateLimited) {
      const remaining = bookIds.length - i - 1
      console.warn(
        `Price lookup still rate-limited bookId=${bookId}; cancelling ${remaining} remaining`,
      )
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
    if (!result.success) {
      console.warn(
        `Price lookup failed bookId=${bookId}: ${result.errorMessage ?? 'unknown error'}`,
      )
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
      // Invalidate summaries (triggers full cache refresh via summaries+byIds)
      queryClient.invalidateQueries({ queryKey: queryKeys.prices.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}
