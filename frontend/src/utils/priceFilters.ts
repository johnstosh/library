// (c) Copyright 2025 by Muczynski
import type { BookPriceDto } from '@/types/dtos'
import type { BookCoverType } from '@/types/enums'

export interface PriceChipFilters {
  hardcover: boolean
  softcover: boolean
  libraryBinding: boolean
  otherUnknown: boolean
  hasListing: boolean
  lookupFailed: boolean
  recent: boolean
}

/** Default window for the Prices "Looked up recently" chip. */
export const DEFAULT_RECENT_HOURS = 24

export const defaultPriceChipFilters: PriceChipFilters = {
  hardcover: false,
  softcover: false,
  libraryBinding: false,
  otherUnknown: false,
  hasListing: false,
  lookupFailed: false,
  recent: false,
}

export function priceChipsFromSearchParams(params: URLSearchParams): PriceChipFilters {
  return {
    hardcover: params.get('hardcover') === 'true',
    softcover: params.get('softcover') === 'true',
    libraryBinding: params.get('libraryBinding') === 'true',
    otherUnknown: params.get('otherUnknown') === 'true',
    hasListing: params.get('hasListing') === 'true',
    lookupFailed: params.get('lookupFailed') === 'true',
    recent: params.get('recent') === 'true',
  }
}

export function maxTotalFromSearchParams(params: URLSearchParams): string {
  return params.get('maxTotal') ?? ''
}

export function recentHoursFromSearchParams(params: URLSearchParams): number {
  const raw = params.get('recentHours')
  const n = parseInt(raw ?? '', 10)
  return Number.isFinite(n) && n >= 1 ? n : DEFAULT_RECENT_HOURS
}

export function priceFilterParamsForUrl(state: {
  chips: PriceChipFilters
  maxTotal: string
  recentHours?: number
}): Record<string, string> {
  const params: Record<string, string> = {}
  if (state.chips.hardcover) params.hardcover = 'true'
  if (state.chips.softcover) params.softcover = 'true'
  if (state.chips.libraryBinding) params.libraryBinding = 'true'
  if (state.chips.otherUnknown) params.otherUnknown = 'true'
  if (state.chips.hasListing) params.hasListing = 'true'
  if (state.chips.lookupFailed) params.lookupFailed = 'true'
  if (state.chips.recent) {
    params.recent = 'true'
    const hours = state.recentHours != null && state.recentHours >= 1
      ? state.recentHours
      : DEFAULT_RECENT_HOURS
    params.recentHours = String(hours)
  }
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
  recentHours = DEFAULT_RECENT_HOURS,
): BookPriceDto[] {
  const maxTotal = parseMaxTotal(maxTotalRaw)
  const coverActive = chips.hardcover || chips.softcover || chips.libraryBinding || chips.otherUnknown
  const statusActive = chips.hasListing || chips.lookupFailed
  const hours = recentHours > 0 ? recentHours : DEFAULT_RECENT_HOURS
  const recentCutoff = now - hours * 60 * 60 * 1000

  return prices.filter((price) => {
    if (coverActive) {
      const coverOk =
        (chips.hardcover && price.cover === 'HARDCOVER') ||
        (chips.softcover && price.cover === 'SOFTCOVER') ||
        (chips.libraryBinding && price.cover === 'LIBRARY_BINDING') ||
        (chips.otherUnknown && isOtherOrUnknownCover(price.cover))
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

export function isOtherOrUnknownCover(cover: BookCoverType | string): boolean {
  return cover === 'OTHER' || cover === 'UNKNOWN'
}

export function coverLabel(cover: BookCoverType | string): string {
  if (cover === 'HARDCOVER') return 'Hardcover'
  if (cover === 'SOFTCOVER') return 'Softcover'
  if (cover === 'LIBRARY_BINDING') return 'Library Binding'
  if (cover === 'OTHER') return 'Other'
  return 'Unknown'
}
