// (c) Copyright 2025 by Muczynski
import {
  defaultBookChipFilters,
  isOtherBookChipActive,
  type BookChipFilters,
} from '@/utils/bookChipFilters'

/** URL query keys for chip state. inLib/elec keep existing shareable URLs. */
export const CHIP_URL_KEYS: Record<keyof BookChipFilters, string> = {
  hasYdlAudio: 'ydlAudio',
  hasYdlBook: 'ydlBook',
  hasYdlEbook: 'ydlEbook',
  hasEmuAudio: 'emuAudio',
  hasEmuBook: 'emuBook',
  hasEmuEbook: 'emuEbook',
  inLibrary: 'inLib',
  electronic: 'elec',
  freeText: 'freeText',
  audio: 'audio',
  mostRecent: 'mostRecent',
  withoutLoc: 'withoutLoc',
  withoutGrokipedia: 'withoutGrokipedia',
  withGrokipedia: 'withGrokipedia',
  withoutGenres: 'withoutGenres',
  notActiveStatus: 'notActiveStatus',
  withoutFreeTextUrls: 'withoutFreeTextUrls',
}

/** Discovery chips shown on Search. Cataloger chips stay on Books only. */
export const SEARCH_VISIBLE_CHIPS: (keyof BookChipFilters)[] = [
  'hasYdlAudio',
  'hasYdlBook',
  'hasYdlEbook',
  'hasEmuAudio',
  'hasEmuBook',
  'hasEmuEbook',
  'inLibrary',
  'electronic',
  'freeText',
  'audio',
]

export type BookFilterUrlMode = 'search' | 'books'

const ALL_CHIP_KEYS = Object.keys(CHIP_URL_KEYS) as (keyof BookChipFilters)[]

export function isSearchVisibleChip(chip: keyof BookChipFilters): boolean {
  return SEARCH_VISIBLE_CHIPS.includes(chip)
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
): boolean {
  return isOtherBookChipActive(chips) || labels.length > 0 || q.trim().length > 0
}

export function chipsFromSearchParams(
  params: URLSearchParams,
  mode: BookFilterUrlMode,
): BookChipFilters {
  const chips: BookChipFilters = { ...defaultBookChipFilters }
  const keys = mode === 'search' ? SEARCH_VISIBLE_CHIPS : ALL_CHIP_KEYS
  for (const chip of keys) {
    if (chip === 'mostRecent' && mode === 'books') continue
    chips[chip] = params.get(CHIP_URL_KEYS[chip]) === 'true'
  }
  if (mode === 'books') {
    const labels = labelsFromSearchParams(params)
    const q = (params.get('q') ?? '').trim()
    const othersOn = isBooksIntakeConstrained(chips, labels, q)
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
  q: string
  bookPage?: number
  authorPage?: number
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

  const keys = mode === 'search' ? SEARCH_VISIBLE_CHIPS : ALL_CHIP_KEYS
  for (const chip of keys) {
    if (chip === 'mostRecent' && mode === 'books') continue
    if (state.chips[chip]) params[CHIP_URL_KEYS[chip]] = 'true'
  }

  if (mode === 'books') {
    const othersOn = isBooksIntakeConstrained(state.chips, state.labels, q)
    if (!othersOn && !state.chips.mostRecent) {
      params[CHIP_URL_KEYS.mostRecent] = 'false'
    }
  }

  if (mode === 'search') {
    if ((state.bookPage ?? 0) > 0) params.bookPage = String(state.bookPage)
    if ((state.authorPage ?? 0) > 0) params.authorPage = String(state.authorPage)
  }

  return params
}

/** One-way handoff: copy Search discovery filters onto the Books inventory URL. */
export function booksPathFromFilters(state: {
  chips: BookChipFilters
  labels: string[]
  q: string
}): string {
  const discoveryChips: BookChipFilters = { ...defaultBookChipFilters, mostRecent: true }
  for (const key of SEARCH_VISIBLE_CHIPS) {
    discoveryChips[key] = state.chips[key]
  }
  const params = bookFilterParamsForUrl(
    { chips: discoveryChips, labels: state.labels, q: state.q },
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
