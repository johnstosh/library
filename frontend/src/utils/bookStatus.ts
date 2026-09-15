// (c) Copyright 2025 by Muczynski
import { BookStatus } from '@/types/enums'

/** Status-filter keys. Active is split into in-library and electronic-resource. */
export const BookStatusFilter = {
  IN_LIBRARY: 'in-library',
  ELECTRONIC_RESOURCE: 'electronic-resource',
  LOST: 'lost',
  WITHDRAWN: 'withdrawn',
  ON_ORDER: 'on-order',
  REQUESTED: 'requested',
} as const

export type BookStatusFilter = (typeof BookStatusFilter)[keyof typeof BookStatusFilter]

/** Chip order: Active variants first, then the remaining BookStatus values. */
export const BOOK_STATUS_FILTER_VALUES: BookStatusFilter[] = [
  BookStatusFilter.IN_LIBRARY,
  BookStatusFilter.ELECTRONIC_RESOURCE,
  BookStatusFilter.LOST,
  BookStatusFilter.WITHDRAWN,
  BookStatusFilter.ON_ORDER,
  BookStatusFilter.REQUESTED,
]

export const BOOK_STATUS_FILTER_LABELS: Record<BookStatusFilter, string> = {
  [BookStatusFilter.IN_LIBRARY]: 'In-library',
  [BookStatusFilter.ELECTRONIC_RESOURCE]: 'Electronic resource',
  [BookStatusFilter.LOST]: 'Lost',
  [BookStatusFilter.WITHDRAWN]: 'Withdrawn',
  [BookStatusFilter.ON_ORDER]: 'On Order',
  [BookStatusFilter.REQUESTED]: 'Requested',
}

const KNOWN_KEYS = new Set<string>(BOOK_STATUS_FILTER_VALUES)

export function isBookStatusFilterKey(value: string): value is BookStatusFilter {
  return KNOWN_KEYS.has(value)
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

function hasLocNumber(locNumber: string | null | undefined): boolean {
  return !isBlank(locNumber)
}

function matchesOne(
  book: {
    status?: string | null
    locNumber?: string | null
    electronicResource?: boolean | null
  },
  key: BookStatusFilter,
): boolean {
  const status = book.status ?? ''
  switch (key) {
    case BookStatusFilter.IN_LIBRARY:
      return status === BookStatus.ACTIVE && hasLocNumber(book.locNumber)
    case BookStatusFilter.ELECTRONIC_RESOURCE:
      return status === BookStatus.ACTIVE && book.electronicResource === true
    case BookStatusFilter.LOST:
      return status === BookStatus.LOST
    case BookStatusFilter.WITHDRAWN:
      return status === BookStatus.WITHDRAWN
    case BookStatusFilter.ON_ORDER:
      return status === BookStatus.ON_ORDER
    case BookStatusFilter.REQUESTED:
      return status === BookStatus.REQUESTED
    default:
      return false
  }
}

/**
 * Parse `status=` plus legacy chip params (`inLib`, `elec`, `requestedStatus`,
 * `notActiveStatus`) so existing shareable URLs still work.
 */
export function bookStatusesFromSearchParams(params: URLSearchParams): BookStatusFilter[] {
  const raw = params.get('status')
  if (raw) {
    const seen = new Set<BookStatusFilter>()
    const selected: BookStatusFilter[] = []
    for (const part of raw.split(',')) {
      const key = part.trim().toLowerCase()
      if (!isBookStatusFilterKey(key) || seen.has(key)) continue
      seen.add(key)
      selected.push(key)
    }
    return selected
  }

  const selected: BookStatusFilter[] = []
  const add = (key: BookStatusFilter) => {
    if (!selected.includes(key)) selected.push(key)
  }
  if (params.get('inLib') === 'true') add(BookStatusFilter.IN_LIBRARY)
  if (params.get('elec') === 'true') add(BookStatusFilter.ELECTRONIC_RESOURCE)
  if (params.get('requestedStatus') === 'true') add(BookStatusFilter.REQUESTED)
  if (params.get('notActiveStatus') === 'true') {
    add(BookStatusFilter.LOST)
    add(BookStatusFilter.WITHDRAWN)
    add(BookStatusFilter.ON_ORDER)
    add(BookStatusFilter.REQUESTED)
  }
  return selected
}

/**
 * Empty selection keeps the catalog default: hide WITHDRAWN and REQUESTED.
 * Any selected values OR together.
 */
export function matchesBookStatusFilter(
  book: {
    status?: string | null
    locNumber?: string | null
    electronicResource?: boolean | null
  },
  selected: string[],
): boolean {
  if (!selected.length) {
    return book.status !== BookStatus.WITHDRAWN && book.status !== BookStatus.REQUESTED
  }
  return selected.some(
    (key) => isBookStatusFilterKey(key) && matchesOne(book, key),
  )
}

export function applyBookStatusFilter<
  T extends {
    status?: string | null
    locNumber?: string | null
    electronicResource?: boolean | null
  },
>(books: T[], selected: string[]): T[] {
  return books.filter((book) => matchesBookStatusFilter(book, selected))
}
