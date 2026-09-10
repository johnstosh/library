// (c) Copyright 2025 by Muczynski
import type { BookPriceDto } from '@/types/dtos'
import type { BookCoverType } from '@/types/enums'

export interface PriceChipFilters {
  hardcover: boolean
  softcover: boolean
  hasListing: boolean
  lookupFailed: boolean
  recent: boolean
}

export const defaultPriceChipFilters: PriceChipFilters = {
  hardcover: false,
  softcover: false,
  hasListing: false,
  lookupFailed: false,
  recent: false,
}

const RECENT_DAYS = 30

export function priceChipsFromSearchParams(params: URLSearchParams): PriceChipFilters {
  return {
    hardcover: params.get('hardcover') === 'true',
    softcover: params.get('softcover') === 'true',
    hasListing: params.get('hasListing') === 'true',
    lookupFailed: params.get('lookupFailed') === 'true',
    recent: params.get('recent') === 'true',
  }
}

export function maxTotalFromSearchParams(params: URLSearchParams): string {
  return params.get('maxTotal') ?? ''
}

export function priceFilterParamsForUrl(state: {
  chips: PriceChipFilters
  maxTotal: string
}): Record<string, string> {
  const params: Record<string, string> = {}
  if (state.chips.hardcover) params.hardcover = 'true'
  if (state.chips.softcover) params.softcover = 'true'
  if (state.chips.hasListing) params.hasListing = 'true'
  if (state.chips.lookupFailed) params.lookupFailed = 'true'
  if (state.chips.recent) params.recent = 'true'
  const maxTotal = state.maxTotal.trim()
  if (maxTotal) params.maxTotal = maxTotal
  return params
}

export function parseMaxTotal(raw: string): number | null {
  const trimmed = raw.trim()
  if (!trimmed) return null
  const n = Number(trimmed)
  return Number.isFinite(n) && n >= 0 ? n : null
}

export function applyPriceFilters(
  prices: BookPriceDto[],
  chips: PriceChipFilters,
  maxTotalRaw: string,
  now = Date.now(),
): BookPriceDto[] {
  const maxTotal = parseMaxTotal(maxTotalRaw)
  const coverActive = chips.hardcover || chips.softcover
  const statusActive = chips.hasListing || chips.lookupFailed
  const recentCutoff = now - RECENT_DAYS * 24 * 60 * 60 * 1000

  return prices.filter((price) => {
    if (coverActive) {
      const coverOk =
        (chips.hardcover && price.cover === 'HARDCOVER') ||
        (chips.softcover && price.cover === 'SOFTCOVER')
      if (!coverOk) return false
    }
    if (statusActive) {
      const hasListing = price.priceDollars != null && !price.lookupError
      const failed = Boolean(price.lookupError) || price.priceDollars == null
      const statusOk = (chips.hasListing && hasListing) || (chips.lookupFailed && failed)
      if (!statusOk) return false
    }
    if (chips.recent) {
      if (!price.lookedUpAt) return false
      const lookedUp = Date.parse(price.lookedUpAt)
      if (!Number.isFinite(lookedUp) || lookedUp < recentCutoff) return false
    }
    if (maxTotal != null) {
      if (price.totalDollars == null) return false
      if (price.totalDollars >= maxTotal) return false
    }
    return true
  })
}

export function coverLabel(cover: BookCoverType | string): string {
  return cover === 'HARDCOVER' ? 'Hardcover' : 'Softcover'
}
