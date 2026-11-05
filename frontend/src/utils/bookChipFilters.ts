// (c) Copyright 2025 by Muczynski
import type { BookDto, BookPriceDto } from '@/types/dtos'
import { authorNeedsCanonicalName } from '@/utils/canonicalAuthorName'
import { titleNeedsChicagoCase } from '@/utils/chicagoTitleCase'

/** Default "price older than N days" window on the Books page. */
export const DEFAULT_PRICE_OLDER_DAYS = 90

/** Minimum trimmed character length for a field to count as "proper" text (plotSummary or detailedDescription). */
export const PROPER_TEXT_MIN_CHARS = 400

/**
 * Independent boolean chip filters shared by the Books and Search pages.
 * Active chips AND together with genre labels, except Audio, Book, and Ebook
 * within one public library, which OR. A Without chip matches a false or unset flag.
 *
 * Row 1: has/without YDL, EMU, ACLA, WCC, and MSU audio, book, and ebook
 * Row 2: freeText, audio, mostRecent
 * Row 3: withoutGrokipedia, withGrokipedia, withoutGenres, withoutFreeTextUrls,
 *   withoutProperPlotOrDescription, titleNotChicago, authorNotCanonical
 * Pricing (Books, librarians): withPrices, noPrices, priceOlder, lookupErrors
 *
 * Status (in-library, electronic-resource, without-loc, lost, withdrawn,
 * on-order, requested) is a separate OR group — see bookStatus.ts — not a boolean chip.
 */
export interface BookChipFilters {
  hasYdlAudio: boolean
  hasYdlBook: boolean
  hasYdlEbook: boolean
  hasEmuAudio: boolean
  hasEmuBook: boolean
  hasEmuEbook: boolean
  hasAclaAudio: boolean
  hasAclaBook: boolean
  hasAclaEbook: boolean
  hasWccAudio: boolean
  hasWccBook: boolean
  hasWccEbook: boolean
  hasMsuAudio: boolean
  hasMsuBook: boolean
  hasMsuEbook: boolean
  withoutYdlAudio: boolean
  withoutYdlBook: boolean
  withoutYdlEbook: boolean
  withoutEmuAudio: boolean
  withoutEmuBook: boolean
  withoutEmuEbook: boolean
  withoutAclaAudio: boolean
  withoutAclaBook: boolean
  withoutAclaEbook: boolean
  withoutWccAudio: boolean
  withoutWccBook: boolean
  withoutWccEbook: boolean
  withoutMsuAudio: boolean
  withoutMsuBook: boolean
  withoutMsuEbook: boolean
  freeText: boolean
  audio: boolean
  mostRecent: boolean
  withoutGrokipedia: boolean
  withGrokipedia: boolean
  withoutGenres: boolean
  withoutFreeTextUrls: boolean
  withoutProperPlotOrDescription: boolean
  titleNotChicago: boolean
  authorNotCanonical: boolean
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
  hasAclaAudio: false,
  hasAclaBook: false,
  hasAclaEbook: false,
  hasWccAudio: false,
  hasWccBook: false,
  hasWccEbook: false,
  hasMsuAudio: false,
  hasMsuBook: false,
  hasMsuEbook: false,
  withoutYdlAudio: false,
  withoutYdlBook: false,
  withoutYdlEbook: false,
  withoutEmuAudio: false,
  withoutEmuBook: false,
  withoutEmuEbook: false,
  withoutAclaAudio: false,
  withoutAclaBook: false,
  withoutAclaEbook: false,
  withoutWccAudio: false,
  withoutWccBook: false,
  withoutWccEbook: false,
  withoutMsuAudio: false,
  withoutMsuBook: false,
  withoutMsuEbook: false,
  freeText: false,
  audio: false,
  // Shared default is off (Search). The Books page turns mostRecent on when
  // the /books URL has no other filters (see bookFilterParams.ts).
  mostRecent: false,
  withoutGrokipedia: false,
  withGrokipedia: false,
  withoutGenres: false,
  withoutFreeTextUrls: false,
  withoutProperPlotOrDescription: false,
  titleNotChicago: false,
  authorNotCanonical: false,
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

const AVAILABILITY_OPPOSITE: Partial<Record<keyof BookChipFilters, keyof BookChipFilters>> = {
  hasYdlAudio: 'withoutYdlAudio',
  withoutYdlAudio: 'hasYdlAudio',
  hasYdlBook: 'withoutYdlBook',
  withoutYdlBook: 'hasYdlBook',
  hasYdlEbook: 'withoutYdlEbook',
  withoutYdlEbook: 'hasYdlEbook',
  hasEmuAudio: 'withoutEmuAudio',
  withoutEmuAudio: 'hasEmuAudio',
  hasEmuBook: 'withoutEmuBook',
  withoutEmuBook: 'hasEmuBook',
  hasEmuEbook: 'withoutEmuEbook',
  withoutEmuEbook: 'hasEmuEbook',
  hasAclaAudio: 'withoutAclaAudio',
  withoutAclaAudio: 'hasAclaAudio',
  hasAclaBook: 'withoutAclaBook',
  withoutAclaBook: 'hasAclaBook',
  hasAclaEbook: 'withoutAclaEbook',
  withoutAclaEbook: 'hasAclaEbook',
  hasWccAudio: 'withoutWccAudio',
  withoutWccAudio: 'hasWccAudio',
  hasWccBook: 'withoutWccBook',
  withoutWccBook: 'hasWccBook',
  hasWccEbook: 'withoutWccEbook',
  withoutWccEbook: 'hasWccEbook',
  hasMsuAudio: 'withoutMsuAudio',
  withoutMsuAudio: 'hasMsuAudio',
  hasMsuBook: 'withoutMsuBook',
  withoutMsuBook: 'hasMsuBook',
  hasMsuEbook: 'withoutMsuEbook',
  withoutMsuEbook: 'hasMsuEbook',
}

/** Turning a format chip on clears its Without pair, and the reverse. */
export function toggleBookChip(chips: BookChipFilters, chip: keyof BookChipFilters): BookChipFilters {
  const turningOn = !chips[chip]
  const next: BookChipFilters = { ...chips, [chip]: turningOn }
  const opposite = AVAILABILITY_OPPOSITE[chip]
  if (turningOn && opposite) {
    next[opposite] = false
  }
  return next
}

function held(value: boolean | null | undefined): boolean {
  return value === true
}

/** Selected formats within one library match when any of them is held. */
function matchesFormatOr(
  checks: { on: boolean; value: boolean | null | undefined }[],
): boolean {
  const selected = checks.filter((check) => check.on)
  if (selected.length === 0) return true
  return selected.some((check) => held(check.value))
}

function matchesWithout(on: boolean, value: boolean | null | undefined): boolean {
  return !on || !held(value)
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

function isMissingGrokipediaUrl(value: string | null | undefined): boolean {
  const trimmed = value?.trim()
  return !trimmed || trimmed === '-'
}

/** True if trimmed length meets or exceeds PROPER_TEXT_MIN_CHARS. Null/blank/whitespace-only = false. */
export function isProperText(value: string | null | undefined): boolean {
  const trimmed = value?.trim()
  return Boolean(trimmed) && trimmed!.length >= PROPER_TEXT_MIN_CHARS
}

/**
 * Apply chip filters to a book list. Chips AND together, except Audio, Book,
 * and Ebook within one public library, which OR. A Without chip keeps titles
 * whose flag is false or unset. Status is applied separately
 * by {@code applyBookStatusFilter}.
 * "Recent Arrivals" matches dateAddedToLibrary on the most recent day UTC
 * (cutoff = UTC start of max-1 day) or a temporary date-format title.
 */
export function applyChipFilters<T extends Pick<
  BookDto,
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
  | 'aclaAudioAvailable'
  | 'aclaPaperAvailable'
  | 'aclaEbookAvailable'
  | 'wccAudioAvailable'
  | 'wccPaperAvailable'
  | 'wccEbookAvailable'
  | 'msuAudioAvailable'
  | 'msuPaperAvailable'
  | 'msuEbookAvailable'
  | 'plotSummary'
  | 'detailedDescription'
  | 'author'
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
    if (!matchesFormatOr([
      { on: chips.hasYdlAudio, value: book.ydlAudioAvailable },
      { on: chips.hasYdlBook, value: book.ydlPaperAvailable },
      { on: chips.hasYdlEbook, value: book.ydlEbookAvailable },
    ])) return false
    if (!matchesWithout(chips.withoutYdlAudio, book.ydlAudioAvailable)) return false
    if (!matchesWithout(chips.withoutYdlBook, book.ydlPaperAvailable)) return false
    if (!matchesWithout(chips.withoutYdlEbook, book.ydlEbookAvailable)) return false
    if (!matchesFormatOr([
      { on: chips.hasEmuAudio, value: book.emuAudioAvailable },
      { on: chips.hasEmuBook, value: book.emuPaperAvailable },
      { on: chips.hasEmuEbook, value: book.emuEbookAvailable },
    ])) return false
    if (!matchesWithout(chips.withoutEmuAudio, book.emuAudioAvailable)) return false
    if (!matchesWithout(chips.withoutEmuBook, book.emuPaperAvailable)) return false
    if (!matchesWithout(chips.withoutEmuEbook, book.emuEbookAvailable)) return false
    if (!matchesFormatOr([
      { on: chips.hasAclaAudio, value: book.aclaAudioAvailable },
      { on: chips.hasAclaBook, value: book.aclaPaperAvailable },
      { on: chips.hasAclaEbook, value: book.aclaEbookAvailable },
    ])) return false
    if (!matchesWithout(chips.withoutAclaAudio, book.aclaAudioAvailable)) return false
    if (!matchesWithout(chips.withoutAclaBook, book.aclaPaperAvailable)) return false
    if (!matchesWithout(chips.withoutAclaEbook, book.aclaEbookAvailable)) return false
    if (!matchesFormatOr([
      { on: chips.hasWccAudio, value: book.wccAudioAvailable },
      { on: chips.hasWccBook, value: book.wccPaperAvailable },
      { on: chips.hasWccEbook, value: book.wccEbookAvailable },
    ])) return false
    if (!matchesFormatOr([
      { on: chips.hasMsuAudio, value: book.msuAudioAvailable },
      { on: chips.hasMsuBook, value: book.msuPaperAvailable },
      { on: chips.hasMsuEbook, value: book.msuEbookAvailable },
    ])) return false
    if (!matchesWithout(chips.withoutWccAudio, book.wccAudioAvailable)) return false
    if (!matchesWithout(chips.withoutWccBook, book.wccPaperAvailable)) return false
    if (!matchesWithout(chips.withoutWccEbook, book.wccEbookAvailable)) return false
    if (!matchesWithout(chips.withoutMsuAudio, book.msuAudioAvailable)) return false
    if (!matchesWithout(chips.withoutMsuBook, book.msuPaperAvailable)) return false
    if (!matchesWithout(chips.withoutMsuEbook, book.msuEbookAvailable)) return false
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
    if (chips.withoutGrokipedia && !isMissingGrokipediaUrl(book.grokipediaUrl)) return false
    if (chips.withGrokipedia && isMissingGrokipediaUrl(book.grokipediaUrl)) return false
    if (chips.withoutGenres && book.tagsList && book.tagsList.length > 0) return false
    if (chips.withoutFreeTextUrls && !isBlank(book.freeTextUrl)) return false

    if (
      chips.withoutProperPlotOrDescription &&
      isProperText(book.plotSummary) &&
      isProperText(book.detailedDescription)
    ) {
      return false
    }

    if (chips.titleNotChicago && !titleNeedsChicagoCase(book.title)) return false
    if (chips.authorNotCanonical && !authorNeedsCanonicalName(book.author)) return false

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
