// (c) Copyright 2025 by Muczynski
import type { BookDto } from '@/types/dtos'

/**
 * Independent boolean chip filters shared by the Books and Search pages.
 * All active chips AND together with genre labels — more buttons on = fewer results.
 *
 * Row 1: inLibrary, electronic, freeText, audio
 * Row 2: mostRecent, withoutLoc, threeLetterLoc, withoutGrokipedia,
 *   withoutGenres, notActiveStatus, withoutFreeTextUrls
 *
 * notActiveStatus is special and always constrains:
 *   off → hide WITHDRAWN; on → only non-ACTIVE statuses.
 */
export interface BookChipFilters {
  inLibrary: boolean
  electronic: boolean
  freeText: boolean
  audio: boolean
  mostRecent: boolean
  withoutLoc: boolean
  threeLetterLoc: boolean
  withoutGrokipedia: boolean
  withoutGenres: boolean
  notActiveStatus: boolean
  withoutFreeTextUrls: boolean
}

export const defaultBookChipFilters: BookChipFilters = {
  inLibrary: false,
  electronic: false,
  freeText: false,
  audio: false,
  mostRecent: false,
  withoutLoc: false,
  threeLetterLoc: false,
  withoutGrokipedia: false,
  withoutGenres: false,
  notActiveStatus: false,
  withoutFreeTextUrls: false,
}

const TEMP_TITLE_RE = /^\d{4}-\d{1,2}-\d{1,2}/

/** True if locNumber starts with three uppercase letters. */
export function is3LetterLoc(locNumber: string | null | undefined): boolean {
  if (!locNumber) return false
  return /^[A-Z]{3}/.test(locNumber)
}

export function isAnyChipActive(chips: BookChipFilters): boolean {
  return (Object.keys(chips) as (keyof BookChipFilters)[]).some((key) => chips[key])
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

/**
 * Apply all chip filters to a book list (AND logic).
 * A book must satisfy every active chip, plus the always-on notActiveStatus constraint.
 * "Most Recent Day" matches dateAddedToLibrary on the most recent day UTC
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
  | 'status'
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
    if (chips.inLibrary && isBlank(book.locNumber)) return false
    if (chips.electronic && !book.electronicResource) return false
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
    if (chips.withoutLoc && !isBlank(book.locNumber)) return false
    if (chips.threeLetterLoc && !is3LetterLoc(book.locNumber)) return false
    if (chips.withoutGrokipedia && !isBlank(book.grokipediaUrl)) return false
    if (chips.withoutGenres && book.tagsList && book.tagsList.length > 0) return false
    if (chips.notActiveStatus) {
      if (book.status === 'ACTIVE') return false
    } else if (book.status === 'WITHDRAWN') {
      return false
    }
    if (chips.withoutFreeTextUrls && !isBlank(book.freeTextUrl)) return false

    return true
  })
}
