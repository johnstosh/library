// (c) Copyright 2025 by Muczynski
import {
  DEFAULT_PRICE_OLDER_DAYS,
  defaultBookChipFilters,
  isOtherBookChipActive,
  type BookChipFilters,
} from '@/utils/bookChipFilters'
import { bookStatusesFromSearchParams } from '@/utils/bookStatus'
import type { DesireToPurchaseFilter } from '@/utils/desireToPurchase'
import { readingDifficultiesFromSearchParams } from '@/utils/readingDifficulty'

/** URL query keys for chip state. */
export const CHIP_URL_KEYS: Record<keyof BookChipFilters, string> = {
  hasYdlAudio: 'ydlAudio',
  hasYdlBook: 'ydlBook',
  hasYdlEbook: 'ydlEbook',
  hasEmuAudio: 'emuAudio',
  hasEmuBook: 'emuBook',
  hasEmuEbook: 'emuEbook',
  hasAclaAudio: 'aclaAudio',
  hasAclaBook: 'aclaBook',
  hasAclaEbook: 'aclaEbook',
  freeText: 'freeText',
  audio: 'audio',
  mostRecent: 'mostRecent',
  withoutGrokipedia: 'withoutGrokipedia',
  withGrokipedia: 'withGrokipedia',
  withoutGenres: 'withoutGenres',
  withoutFreeTextUrls: 'withoutFreeTextUrls',
  withPrices: 'withPrices',
  noPrices: 'noPrices',
  priceOlder: 'priceOlder',
  lookupErrors: 'lookupErrors',
}

/** Discovery chips shown on Search. Cataloger chips stay on Books only. */
export const SEARCH_VISIBLE_CHIPS: (keyof BookChipFilters)[] = [
  'hasYdlAudio',
  'hasYdlBook',
  'hasYdlEbook',
  'hasEmuAudio',
  'hasEmuBook',
  'hasEmuEbook',
  'hasAclaAudio',
  'hasAclaBook',
  'hasAclaEbook',
  'freeText',
  'audio',
  'mostRecent',
]

export type BookFilterUrlMode = 'search' | 'books' | 'prices'

const ALL_CHIP_KEYS = Object.keys(CHIP_URL_KEYS) as (keyof BookChipFilters)[]

export function isSearchVisibleChip(chip: keyof BookChipFilters): boolean {
  return SEARCH_VISIBLE_CHIPS.includes(chip)
}

export function favoriteListsFromSearchParams(params: URLSearchParams): string[] {
  const raw = params.get('favoriteLists')
  if (!raw) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

export function priceOlderDaysFromSearchParams(params: URLSearchParams): number {
  const raw = params.get('priceOlderDays')
  const n = parseInt(raw ?? '', 10)
  return Number.isFinite(n) && n >= 1 ? n : DEFAULT_PRICE_OLDER_DAYS
}

export function labelsFromSearchParams(params: URLSearchParams): string[] {
  const raw = params.get('labels')
  if (!raw) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

export function pageFromSearchParams(
  params: URLSearchParams,
  key: string,
  fallbackKey = 'page',
): number {
  const raw = params.get(key) ?? params.get(fallbackKey)
  const n = parseInt(raw ?? '0', 10)
  return Number.isFinite(n) && n > 0 ? n : 0
}

export function isBooksIntakeConstrained(
  chips: BookChipFilters,
  labels: string[],
  q: string,
  readingDifficulties: string[] = [],
  favoriteLists: string[] = [],
  statuses: string[] = [],
): boolean {
  return (
    isOtherBookChipActive(chips) ||
    labels.length > 0 ||
    readingDifficulties.length > 0 ||
    favoriteLists.length > 0 ||
    statuses.length > 0 ||
    q.trim().length > 0
  )
}

export function chipsFromSearchParams(
  params: URLSearchParams,
  mode: BookFilterUrlMode,
): BookChipFilters {
  const chips: BookChipFilters = { ...defaultBookChipFilters }
  const keys = mode === 'search' ? SEARCH_VISIBLE_CHIPS : ALL_CHIP_KEYS
  for (const chip of keys) {
    if (chip === 'mostRecent' && (mode === 'books' || mode === 'prices')) continue
    chips[chip] = params.get(CHIP_URL_KEYS[chip]) === 'true'
  }
  if (mode === 'prices') {
    chips.mostRecent = params.get(CHIP_URL_KEYS.mostRecent) === 'true'
  }
  if (mode === 'books') {
    const labels = labelsFromSearchParams(params)
    const readingDifficulties = readingDifficultiesFromSearchParams(params)
    const statuses = bookStatusesFromSearchParams(params)
    const q = (params.get('q') ?? '').trim()
    const favoriteLists = favoriteListsFromSearchParams(params)
    const othersOn = isBooksIntakeConstrained(
      chips,
      labels,
      q,
      readingDifficulties,
      favoriteLists,
      statuses,
    )
    if (othersOn) {
      chips.mostRecent = false
    } else if (params.get(CHIP_URL_KEYS.mostRecent) === 'false') {
      chips.mostRecent = false
    } else {
      chips.mostRecent = true
    }
  }
  return chips
}

export interface BookFilterUrlState {
  chips: BookChipFilters
  labels: string[]
  readingDifficulties?: string[]
  statuses?: string[]
  favoriteLists?: string[]
  q: string
  bookPage?: number
  authorPage?: number
  /** Days for the Books "price older than" chip. Default 90 when the chip is on. */
  priceOlderDays?: number
  /** Desire-to-purchase chips (Prices page). */
  desireToPurchase?: DesireToPurchaseFilter[]
  /** When true, emit `q=` even if the query is blank (Search “has searched”). */
  includeBlankQuery?: boolean
}

export function bookFilterParamsForUrl(
  state: BookFilterUrlState,
  mode: BookFilterUrlMode,
): Record<string, string> {
  const params: Record<string, string> = {}
  const q = state.q.trim()
  if (q) {
    params.q = q
  } else if (state.includeBlankQuery) {
    params.q = ''
  }
  if (state.labels.length > 0) params.labels = state.labels.join(',')
  const readingDifficulties = state.readingDifficulties ?? []
  if (readingDifficulties.length > 0) {
    params.readingDifficulty = readingDifficulties.join(',')
  }
  const statuses = state.statuses ?? []
  if (statuses.length > 0) {
    params.status = statuses.join(',')
  }
  const favoriteLists = state.favoriteLists ?? []
  if (favoriteLists.length > 0) params.favoriteLists = favoriteLists.join(',')
  const desireToPurchase = state.desireToPurchase ?? []
  if (desireToPurchase.length > 0) {
    params.desireToPurchase = desireToPurchase.join(',')
  }

  const keys = mode === 'search' ? SEARCH_VISIBLE_CHIPS : ALL_CHIP_KEYS
  for (const chip of keys) {
    if (chip === 'mostRecent' && (mode === 'books' || mode === 'prices')) continue
    if (state.chips[chip]) params[CHIP_URL_KEYS[chip]] = 'true'
  }

  if (mode === 'prices' && state.chips.mostRecent) {
    params[CHIP_URL_KEYS.mostRecent] = 'true'
  }

  if (mode === 'books') {
    const othersOn = isBooksIntakeConstrained(
      state.chips,
      state.labels,
      q,
      readingDifficulties,
      favoriteLists,
      statuses,
    )
    if (!othersOn && !state.chips.mostRecent) {
      params[CHIP_URL_KEYS.mostRecent] = 'false'
    }
  }

  if (mode === 'search') {
    if ((state.bookPage ?? 0) > 0) params.bookPage = String(state.bookPage)
    if ((state.authorPage ?? 0) > 0) params.authorPage = String(state.authorPage)
  }

  if (state.chips.priceOlder) {
    const days = state.priceOlderDays != null && state.priceOlderDays >= 1
      ? state.priceOlderDays
      : DEFAULT_PRICE_OLDER_DAYS
    params.priceOlderDays = String(days)
  }

  return params
}

/** One-way handoff: copy Search discovery filters onto the Books inventory URL. */
export function booksPathFromFilters(state: {
  chips: BookChipFilters
  labels: string[]
  readingDifficulties?: string[]
  statuses?: string[]
  favoriteLists?: string[]
  q: string
}): string {
  const discoveryChips: BookChipFilters = { ...defaultBookChipFilters, mostRecent: true }
  for (const key of SEARCH_VISIBLE_CHIPS) {
    // Books keeps its own intake default; don't copy Search's mostRecent off-state.
    if (key === 'mostRecent') continue
    discoveryChips[key] = state.chips[key]
  }
  const params = bookFilterParamsForUrl(
    {
      chips: discoveryChips,
      labels: state.labels,
      readingDifficulties: state.readingDifficulties ?? [],
      statuses: state.statuses ?? [],
      favoriteLists: state.favoriteLists ?? [],
      q: state.q,
    },
    'books',
  )
  const qs = new URLSearchParams(params).toString()
  return qs ? `/books?${qs}` : '/books'
}

/** One-way handoff: copy Books inventory filters onto the Prices page URL. */
export function pricesPathFromFilters(state: {
  chips: BookChipFilters
  labels: string[]
  readingDifficulties?: string[]
  statuses?: string[]
  favoriteLists?: string[]
  q: string
  priceOlderDays?: number
  desireToPurchase?: DesireToPurchaseFilter[]
}): string {
  const params = bookFilterParamsForUrl(
    {
      chips: state.chips,
      labels: state.labels,
      readingDifficulties: state.readingDifficulties ?? [],
      statuses: state.statuses ?? [],
      favoriteLists: state.favoriteLists ?? [],
      q: state.q,
      priceOlderDays: state.priceOlderDays,
      desireToPurchase: state.desireToPurchase,
    },
    'prices',
  )
  const qs = new URLSearchParams(params).toString()
  return qs ? `/prices?${qs}` : '/prices'
}

/** One-way handoff: copy Prices inventory filters onto the Books page URL. */
export function booksPathFromPriceFilters(state: {
  chips: BookChipFilters
  labels: string[]
  readingDifficulties?: string[]
  statuses?: string[]
  favoriteLists?: string[]
  q: string
  priceOlderDays?: number
}): string {
  const params = bookFilterParamsForUrl(
    {
      chips: state.chips,
      labels: state.labels,
      readingDifficulties: state.readingDifficulties ?? [],
      statuses: state.statuses ?? [],
      favoriteLists: state.favoriteLists ?? [],
      q: state.q,
      priceOlderDays: state.priceOlderDays,
    },
    'books',
  )
  const qs = new URLSearchParams(params).toString()
  return qs ? `/books?${qs}` : '/books'
}

export function matchesBookQuery(
  book: { title?: string | null; author?: string | null },
  q: string,
): boolean {
  const needle = q.trim().toLowerCase()
  if (!needle) return true
  return (
    (book.title ?? '').toLowerCase().includes(needle) ||
    (book.author ?? '').toLowerCase().includes(needle)
  )
}
