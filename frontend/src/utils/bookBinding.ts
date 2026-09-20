// (c) Copyright 2025 by Muczynski
import { BookCoverType } from '@/types/enums'

/** Catalog and price bindings in form/filter order, Unknown last. */
export const BOOK_BINDING_FILTER_VALUES: BookCoverType[] = [
  BookCoverType.HARDCOVER,
  BookCoverType.SOFTCOVER,
  BookCoverType.LIBRARY_BINDING,
  BookCoverType.OTHER,
  BookCoverType.UNKNOWN,
]

export const BOOK_BINDING_FILTER_LABELS: Record<BookCoverType, string> = {
  [BookCoverType.HARDCOVER]: 'Hardcover',
  [BookCoverType.SOFTCOVER]: 'Softcover',
  [BookCoverType.LIBRARY_BINDING]: 'Library Binding',
  [BookCoverType.OTHER]: 'Other',
  [BookCoverType.UNKNOWN]: 'Unknown',
}

const KNOWN_KEYS = new Set<string>(BOOK_BINDING_FILTER_VALUES)

export function isBookCoverType(value: string): value is BookCoverType {
  return KNOWN_KEYS.has(value)
}

/** null, blank, and unknown values count as Unknown. */
export function normalizeBookBinding(
  value: string | null | undefined,
): BookCoverType {
  if (!value || typeof value !== 'string' || value.trim() === '') {
    return BookCoverType.UNKNOWN
  }
  const key = value.trim().toUpperCase()
  return isBookCoverType(key) ? key : BookCoverType.UNKNOWN
}

export function bookBindingLabel(value: string | null | undefined): string {
  return BOOK_BINDING_FILTER_LABELS[normalizeBookBinding(value)]
}

/**
 * For display in tables, view pages, modals, and search results.
 * Electronic resources show "Electronic resource" (matching status chip) instead
 * of any binding (including Unknown). Binding filters remain unchanged.
 * Accepts either binding value or full book object.
 */
export function bookBindingDisplay(
  bindingOrBook: string | null | undefined | { binding?: string | null; electronicResource?: boolean | null },
  electronicResource: boolean | null | undefined = false,
): string {
  if (
    typeof bindingOrBook === 'object' &&
    bindingOrBook !== null &&
    'electronicResource' in bindingOrBook
  ) {
    const book = bindingOrBook as { binding?: string | null; electronicResource?: boolean | null }
    if (book.electronicResource === true) {
      return 'Electronic resource'
    }
    return bookBindingLabel(book.binding)
  }

  if (electronicResource === true) {
    return 'Electronic resource'
  }
  return bookBindingLabel(bindingOrBook as string | null | undefined)
}




export function bindingsFromSearchParams(params: URLSearchParams): BookCoverType[] {
  const raw = params.get('binding')
  if (!raw) return []
  const seen = new Set<BookCoverType>()
  const selected: BookCoverType[] = []
  for (const part of raw.split(',')) {
    const key = part.trim().toUpperCase()
    if (!isBookCoverType(key) || seen.has(key)) continue
    seen.add(key)
    selected.push(key)
  }
  return selected
}

export function matchesBookBindingFilter(
  value: string | null | undefined,
  selected: string[],
): boolean {
  if (!selected.length) return true
  return selected.includes(normalizeBookBinding(value))
}

export function applyBookBindingFilter<T extends { binding?: string | null }>(
  books: T[],
  selected: string[],
): T[] {
  if (!selected.length) return books
  return books.filter((book) => matchesBookBindingFilter(book.binding, selected))
}
