// (c) Copyright 2025 by Muczynski
import type { BookDto, BookPriceDto } from '@/types/dtos'

/** Default "price older than N days" window on the Books page. */
export const DEFAULT_PRICE_OLDER_DAYS = 90

/**
 * Independent boolean chip filters shared by the Books and Search pages.
 * All active chips AND together with genre labels — more buttons on = fewer results.
 *
 * Row 1: hasYdlAudio, hasYdlBook, hasYdlEbook, hasEmuAudio, hasEmuBook, hasEmuEbook
 * Row 2: freeText, audio, mostRecent
 * Row 3: withoutLoc, withoutGrokipedia, withGrokipedia,
 *   withoutGenres, withoutFreeTextUrls
 * Pricing (Books, librarians): withPrices, noPrices, priceOlder, lookupErrors
 *
 * Status (in-library, electronic-resource, lost, withdrawn, on-order, requested)
 * is a separate OR group — see bookStatus.ts — not a boolean chip.
 */
export interface BookChipFilters {
  hasYdlAudio: boolean
  hasYdlBook: boolean
  hasYdlEbook: boolean
  hasEmuAudio: boolean
  hasEmuBook: boolean
  hasEmuEbook: boolean
  freeText: boolean
  audio: boolean
  mostRecent: boolean
  withoutLoc: boolean
  withoutGrokipedia: boolean
  withGrokipedia: boolean
  withoutGenres: boolean
  withoutFreeTextUrls: boolean
  withPrices: boolean
  noPrices: boolean
  priceOlder: boolean
  lookupErrors: boolean
}

export const defaultBookChipFilters: BookChipFilters = {
  hasYdlAudio: false,
  hasYdlBook: false,
  hasYdlEbook: false,
  hasEmuAudio: false,
  hasEmuBook: false,
  hasEmuEbook: false,
  freeText: false,
  audio: false,
  // Shared default is off (Search). The Books page turns mostRecent on when
  // the /books URL has no other filters (see bookFilterParams.ts).
  mostRecent: false,
  withoutLoc: false,
  withoutGrokipedia: false,
  withGrokipedia: false,
  withoutGenres: false,
  withoutFreeTextUrls: false,
  withPrices: false,
  noPrices: false,
  priceOlder: false,
  lookupErrors: false,
}

const TEMP_TITLE_RE = /^\d{4}-\d{1,2}-\d{1,2}/

export function isAnyChipActive(chips: BookChipFilters): boolean {
  return (Object.keys(chips) as (keyof BookChipFilters)[]).some((key) => chips[key])
}

/** True if any chip other than mostRecent is on. */
export function isOtherBookChipActive(chips: BookChipFilters): boolean {
  return (Object.keys(chips) as (keyof BookChipFilters)[])
    .filter((key) => key !== 'mostRecent')
    .some((key) => chips[key])
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

function isMissingGrokipediaUrl(value: string | null | undefined): boolean {
  const trimmed = value?.trim()
  return !trimmed || trimmed === '-'
}

/**
 * Apply all chip filters to a book list (AND logic).
 * A book must satisfy every active chip. Status is applied separately
 * by {@code applyBookStatusFilter}.
 * "Recent Arrivals" matches dateAddedToLibrary on the most recent day UTC
 * (cutoff = UTC start of max-1 day) or a temporary date-format title.
 */
export function applyChipFilters<T extends Pick<
  BookDto,
  | 'locNumber'
  | 'electronicResource'
  | 'freeTextUrl'
  | 'title'
  | 'dateAddedToLibrary'
  | 'grokipediaUrl'
  | 'tagsList'
  | 'ydlAudioAvailable'
  | 'ydlPaperAvailable'
  | 'ydlEbookAvailable'
  | 'emuAudioAvailable'
  | 'emuPaperAvailable'
  | 'emuEbookAvailable'
>>(books: T[], chips: BookChipFilters): T[] {
  let maxDate: Date | null = null
  if (chips.mostRecent) {
    for (const b of books) {
      if (b.dateAddedToLibrary) {
        const d = new Date(b.dateAddedToLibrary)
        if (!maxDate || d > maxDate) maxDate = d
      }
    }
  }
  let cutoff: Date | null = null
  if (maxDate) {
    cutoff = new Date(maxDate)
    cutoff.setUTCHours(0, 0, 0, 0)
    cutoff.setUTCDate(cutoff.getUTCDate() - 1)
  }

  return books.filter((book) => {
    if (chips.hasYdlAudio && book.ydlAudioAvailable !== true) return false
    if (chips.hasYdlBook && book.ydlPaperAvailable !== true) return false
    if (chips.hasYdlEbook && book.ydlEbookAvailable !== true) return false
    if (chips.hasEmuAudio && book.emuAudioAvailable !== true) return false
    if (chips.hasEmuBook && book.emuPaperAvailable !== true) return false
    if (chips.hasEmuEbook && book.emuEbookAvailable !== true) return false
    if (chips.freeText && isBlank(book.freeTextUrl)) return false
    if (chips.audio) {
      if (!book.freeTextUrl || !book.freeTextUrl.toLowerCase().includes('librivox')) return false
    }

    if (chips.mostRecent) {
      const isTempTitle = TEMP_TITLE_RE.test(book.title ?? '')
      if (!isTempTitle) {
        if (!book.dateAddedToLibrary) return false
        const bookDate = new Date(book.dateAddedToLibrary)
        if (!cutoff || bookDate < cutoff) return false
      }
    }
    if (chips.withoutLoc) {
      if (!isBlank(book.locNumber)) return false
      // Electronic resources are not shelved, so they do not need LOC numbers.
      if (book.electronicResource === true) return false
    }
    if (chips.withoutGrokipedia && !isMissingGrokipediaUrl(book.grokipediaUrl)) return false
    if (chips.withGrokipedia && isMissingGrokipediaUrl(book.grokipediaUrl)) return false
    if (chips.withoutGenres && book.tagsList && book.tagsList.length > 0) return false
    if (chips.withoutFreeTextUrls && !isBlank(book.freeTextUrl)) return false

    return true
  })
}

/** Stored when AbeBooks returned a real SearchResults page with no usable listing. */
export const NO_MATCHING_LISTING = 'No matching listing'

/** True when a row is an actual AbeBooks listing, not a failed/cancelled lookup. */
export function isSavedPriceListing(price: BookPriceDto): boolean {
  return price.priceDollars != null && !price.lookupError
}

/**
 * Lookup Errors filter: rate-limited, HTTP errors, cancelled, and other
 * failures — not "No matching listing" (that is a successful empty search).
 */
export function isLookupError(price: BookPriceDto): boolean {
  const err = price.lookupError?.trim()
  return Boolean(err) && err !== NO_MATCHING_LISTING
}

export interface BookPriceFilterOptions {
  withPrices?: boolean
  noPrices: boolean
  priceOlder: boolean
  priceOlderDays: number
  lookupErrors?: boolean
}

/**
 * Books/Prices price chips. withPrices keeps books that have a usable listing.
 * noPrices and priceOlder OR when both are on: no usable listing (missing rows,
 * "No matching listing", rate-limited) or a successful lookup older than
 * {@code priceOlderDays}. withPrices ANDs with that pair. lookupErrors ANDs
 * books that have a rate-limited/HTTP/other error (not "No matching listing").
 */
export function applyBookPriceFilters<T extends { id: number }>(
  books: T[],
  prices: BookPriceDto[],
  options: BookPriceFilterOptions,
  now = Date.now(),
): T[] {
  const withPrices = Boolean(options.withPrices)
  const lookupErrors = Boolean(options.lookupErrors)
  if (!withPrices && !options.noPrices && !options.priceOlder && !lookupErrors) {
    return books
  }
  const latestSavedByBook = new Map<number, number>()
  const lookupErrorBookIds = new Set<number>()
  for (const price of prices) {
    if (isLookupError(price)) {
      lookupErrorBookIds.add(price.bookId)
    }
    if (!isSavedPriceListing(price)) {
      continue
    }
    const parsed = price.lookedUpAt ? Date.parse(price.lookedUpAt) : Number.NaN
    const ts = Number.isFinite(parsed) ? parsed : 0
    const prev = latestSavedByBook.get(price.bookId)
    if (prev == null || ts > prev) {
      latestSavedByBook.set(price.bookId, ts)
    }
  }
  const days = options.priceOlderDays > 0 ? options.priceOlderDays : DEFAULT_PRICE_OLDER_DAYS
  const cutoff = now - days * 24 * 60 * 60 * 1000
  return books.filter((book) => {
    const latest = latestSavedByBook.get(book.id)
    const hasPricing = latest != null
    if (withPrices && !hasPricing) {
      return false
    }
    if (options.noPrices && options.priceOlder) {
      if (hasPricing && latest >= cutoff) return false
    } else if (options.noPrices) {
      if (hasPricing) return false
    } else if (options.priceOlder) {
      if (!hasPricing || latest >= cutoff) return false
    }
    if (lookupErrors && !lookupErrorBookIds.has(book.id)) {
      return false
    }
    return true
  })
}
