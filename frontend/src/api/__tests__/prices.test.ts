// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import {
  PRICE_LOOKUP_BACKOFF_MS,
  PRICE_LOOKUP_CANCELLED_MESSAGE,
  PRICE_LOOKUP_PAUSE_MS,
  lookupPricesForIds,
} from '../prices'
import type { BookPriceLookupResultDto } from '@/types/dtos'

function ok(bookId: number): BookPriceLookupResultDto {
  return { bookId, success: true, bookTitle: `Book ${bookId}` }
}

function rateLimited(bookId: number): BookPriceLookupResultDto {
  return { bookId, success: false, rateLimited: true, errorMessage: 'AbeBooks rate limited' }
}

describe('lookupPricesForIds', () => {
  it('pauses between books', async () => {
    const lookup = vi.fn(async (id: number) => ok(id))
    const sleep = vi.fn(async () => undefined)

    await lookupPricesForIds([1, 2, 3], lookup, undefined, sleep)

    expect(lookup).toHaveBeenCalledTimes(3)
    expect(sleep).toHaveBeenCalledTimes(2)
    expect(sleep).toHaveBeenNthCalledWith(1, PRICE_LOOKUP_PAUSE_MS)
    expect(sleep).toHaveBeenNthCalledWith(2, PRICE_LOOKUP_PAUSE_MS)
  })

  it('backs off then cancels remaining books when still rate limited', async () => {
    const lookup = vi.fn(async (id: number) => (id === 2 ? rateLimited(id) : ok(id)))
    const sleep = vi.fn(async () => undefined)

    const results = await lookupPricesForIds([1, 2, 3, 4], lookup, undefined, sleep)

    expect(results).toHaveLength(4)
    expect(results[0].success).toBe(true)
    expect(results[1].rateLimited).toBe(true)
    expect(results[2]).toMatchObject({
      bookId: 3,
      cancelled: true,
      errorMessage: PRICE_LOOKUP_CANCELLED_MESSAGE,
    })
    expect(results[3].cancelled).toBe(true)
    expect(lookup.mock.calls.map((c) => c[0])).toEqual([1, 2, 2, 2])
    expect(sleep).toHaveBeenCalledWith(PRICE_LOOKUP_PAUSE_MS)
    expect(sleep).toHaveBeenCalledWith(PRICE_LOOKUP_BACKOFF_MS[0])
    expect(sleep).toHaveBeenCalledWith(PRICE_LOOKUP_BACKOFF_MS[1])
  })

  it('continues after a rate-limit that recovers on backoff', async () => {
    let emmaTries = 0
    const lookup = vi.fn(async (id: number) => {
      if (id === 2) {
        emmaTries += 1
        return emmaTries === 1 ? rateLimited(id) : ok(id)
      }
      return ok(id)
    })
    const sleep = vi.fn(async () => undefined)

    const results = await lookupPricesForIds([1, 2, 3], lookup, undefined, sleep)

    expect(results.map((r) => r.success)).toEqual([true, true, true])
    expect(results.some((r) => r.cancelled)).toBe(false)
    expect(lookup.mock.calls.map((c) => c[0])).toEqual([1, 2, 2, 3])
  })
})
