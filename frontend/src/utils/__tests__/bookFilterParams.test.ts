// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { defaultBookChipFilters, type BookChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  booksPathFromFilters,
  chipsFromSearchParams,
  isBooksIntakeConstrained,
  isSearchVisibleChip,
  labelsFromSearchParams,
  matchesBookQuery,
  pageFromSearchParams,
  SEARCH_VISIBLE_CHIPS,
} from '@/utils/bookFilterParams'

function chips(overrides: Partial<BookChipFilters> = {}): BookChipFilters {
  return { ...defaultBookChipFilters, ...overrides }
}

describe('isSearchVisibleChip', () => {
  it('keeps discovery chips and rejects cataloger chips', () => {
    expect(isSearchVisibleChip('inLibrary')).toBe(true)
    expect(isSearchVisibleChip('freeText')).toBe(true)
    expect(isSearchVisibleChip('hasYdlAudio')).toBe(true)
    expect(isSearchVisibleChip('withoutLoc')).toBe(false)
    expect(isSearchVisibleChip('mostRecent')).toBe(false)
    expect(isSearchVisibleChip('notActiveStatus')).toBe(false)
    expect(SEARCH_VISIBLE_CHIPS).not.toContain('withGrokipedia')
  })
})

describe('labelsFromSearchParams', () => {
  it('parses a comma-separated labels param', () => {
    expect(labelsFromSearchParams(new URLSearchParams('labels=fiction,classic'))).toEqual([
      'fiction',
      'classic',
    ])
  })

  it('returns empty when labels are missing', () => {
    expect(labelsFromSearchParams(new URLSearchParams('q=narnia'))).toEqual([])
  })
})

describe('pageFromSearchParams', () => {
  it('reads the named page and falls back to page', () => {
    expect(pageFromSearchParams(new URLSearchParams('bookPage=2'), 'bookPage')).toBe(2)
    expect(pageFromSearchParams(new URLSearchParams('page=3'), 'bookPage')).toBe(3)
    expect(pageFromSearchParams(new URLSearchParams('bookPage=1&page=9'), 'bookPage')).toBe(1)
    expect(pageFromSearchParams(new URLSearchParams(''), 'authorPage')).toBe(0)
  })
})

describe('chipsFromSearchParams search mode', () => {
  it('reads discovery chips and ignores cataloger params', () => {
    const params = new URLSearchParams('inLib=true&withoutLoc=true&mostRecent=true')
    const result = chipsFromSearchParams(params, 'search')
    expect(result.inLibrary).toBe(true)
    expect(result.withoutLoc).toBe(false)
    expect(result.mostRecent).toBe(false)
  })
})

describe('chipsFromSearchParams books mode', () => {
  it('defaults Most Recent Day on when the URL is empty', () => {
    expect(chipsFromSearchParams(new URLSearchParams(), 'books').mostRecent).toBe(true)
  })

  it('turns Most Recent Day off when mostRecent=false', () => {
    expect(
      chipsFromSearchParams(new URLSearchParams('mostRecent=false'), 'books').mostRecent,
    ).toBe(false)
  })

  it('turns Most Recent Day off when another chip, labels, or q is present', () => {
    expect(chipsFromSearchParams(new URLSearchParams('withoutLoc=true'), 'books').mostRecent).toBe(
      false,
    )
    expect(chipsFromSearchParams(new URLSearchParams('labels=fiction'), 'books').mostRecent).toBe(
      false,
    )
    expect(chipsFromSearchParams(new URLSearchParams('q=narnia'), 'books').mostRecent).toBe(false)
  })
})

describe('bookFilterParamsForUrl', () => {
  it('omits default Books intake from the URL', () => {
    expect(
      bookFilterParamsForUrl(
        { chips: chips({ mostRecent: true }), labels: [], q: '' },
        'books',
      ),
    ).toEqual({})
  })

  it('writes mostRecent=false for an explicit full catalog', () => {
    expect(
      bookFilterParamsForUrl(
        { chips: chips({ mostRecent: false }), labels: [], q: '' },
        'books',
      ),
    ).toEqual({ mostRecent: 'false' })
  })

  it('writes discovery chips, labels, and q for Search and omits cataloger chips', () => {
    const params = bookFilterParamsForUrl(
      {
        chips: chips({ inLibrary: true, withoutLoc: true, mostRecent: true }),
        labels: ['fiction'],
        q: 'Augustine',
        bookPage: 2,
        authorPage: 1,
      },
      'search',
    )
    expect(params).toEqual({
      q: 'Augustine',
      labels: 'fiction',
      inLib: 'true',
      bookPage: '2',
      authorPage: '1',
    })
  })

  it('emits a blank q when includeBlankQuery is set', () => {
    expect(
      bookFilterParamsForUrl(
        { chips: chips(), labels: [], q: '', includeBlankQuery: true },
        'search',
      ),
    ).toEqual({ q: '' })
  })
})

describe('booksPathFromFilters', () => {
  it('opens intake when Search has nothing to copy', () => {
    expect(booksPathFromFilters({ chips: chips(), labels: [], q: '' })).toBe('/books')
  })

  it('copies discovery filters and query onto /books', () => {
    expect(
      booksPathFromFilters({
        chips: chips({ inLibrary: true, withoutLoc: true }),
        labels: ['classic'],
        q: 'Summa',
      }),
    ).toBe('/books?q=Summa&labels=classic&inLib=true')
  })
})

describe('matchesBookQuery', () => {
  it('matches title or author case-insensitively', () => {
    const book = { title: 'City of God', author: 'Augustine of Hippo' }
    expect(matchesBookQuery(book, 'city')).toBe(true)
    expect(matchesBookQuery(book, 'HIPPO')).toBe(true)
    expect(matchesBookQuery(book, 'narnia')).toBe(false)
    expect(matchesBookQuery(book, '  ')).toBe(true)
  })
})

describe('isBooksIntakeConstrained', () => {
  it('is true when any non-intake filter is on', () => {
    expect(isBooksIntakeConstrained(chips({ mostRecent: true }), [], '')).toBe(false)
    expect(isBooksIntakeConstrained(chips({ withoutLoc: true }), [], '')).toBe(true)
    expect(isBooksIntakeConstrained(chips(), ['fiction'], '')).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], 'narnia')).toBe(true)
  })
})
