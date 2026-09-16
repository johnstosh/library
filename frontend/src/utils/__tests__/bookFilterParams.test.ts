// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { defaultBookChipFilters, type BookChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  booksPathFromFilters,
  booksPathFromPriceFilters,
  pricesPathFromFilters,
  chipsFromSearchParams,
  isBooksIntakeConstrained,
  isSearchVisibleChip,
  labelsFromSearchParams,
  matchesBookQuery,
  pageFromSearchParams,
  priceOlderDaysFromSearchParams,
  SEARCH_VISIBLE_CHIPS,
} from '@/utils/bookFilterParams'

function chips(overrides: Partial<BookChipFilters> = {}): BookChipFilters {
  return { ...defaultBookChipFilters, ...overrides }
}

describe('isSearchVisibleChip', () => {
  it('keeps discovery chips and rejects cataloger chips', () => {
    expect(isSearchVisibleChip('freeText')).toBe(true)
    expect(isSearchVisibleChip('hasYdlAudio')).toBe(true)
    expect(isSearchVisibleChip('withoutLoc')).toBe(false)
    expect(isSearchVisibleChip('mostRecent')).toBe(true)
    expect(isSearchVisibleChip('withPrices')).toBe(false)
    expect(isSearchVisibleChip('noPrices')).toBe(false)
    expect(isSearchVisibleChip('priceOlder')).toBe(false)
    expect(isSearchVisibleChip('lookupErrors')).toBe(false)
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
    const params = new URLSearchParams('freeText=true&withoutLoc=true&mostRecent=true')
    const result = chipsFromSearchParams(params, 'search')
    expect(result.freeText).toBe(true)
    expect(result.withoutLoc).toBe(false)
    expect(result.mostRecent).toBe(true)
  })
})

describe('chipsFromSearchParams books mode', () => {
  it('defaults Recent Arrivals on when the URL is empty', () => {
    expect(chipsFromSearchParams(new URLSearchParams(), 'books').mostRecent).toBe(true)
  })

  it('turns Recent Arrivals off when mostRecent=false', () => {
    expect(
      chipsFromSearchParams(new URLSearchParams('mostRecent=false'), 'books').mostRecent,
    ).toBe(false)
  })

  it('turns Recent Arrivals off when another chip, labels, or q is present', () => {
    expect(chipsFromSearchParams(new URLSearchParams('withoutLoc=true'), 'books').mostRecent).toBe(
      false,
    )
    expect(chipsFromSearchParams(new URLSearchParams('labels=fiction'), 'books').mostRecent).toBe(
      false,
    )
    expect(
      chipsFromSearchParams(new URLSearchParams('readingDifficulty=children'), 'books').mostRecent,
    ).toBe(false)
    expect(chipsFromSearchParams(new URLSearchParams('q=narnia'), 'books').mostRecent).toBe(false)
    expect(chipsFromSearchParams(new URLSearchParams('noPrices=true'), 'books').mostRecent).toBe(false)
    expect(chipsFromSearchParams(new URLSearchParams('withPrices=true'), 'books').mostRecent).toBe(false)
  })
})

describe('priceOlderDaysFromSearchParams', () => {
  it('defaults to 90', () => {
    expect(priceOlderDaysFromSearchParams(new URLSearchParams())).toBe(90)
  })

  it('reads a positive integer', () => {
    expect(priceOlderDaysFromSearchParams(new URLSearchParams('priceOlderDays=30'))).toBe(30)
  })
})

describe('bookFilterParamsForUrl price chips', () => {
  it('emits noPrices and priceOlderDays when those chips are on', () => {
    const params = bookFilterParamsForUrl(
      {
        chips: chips({ noPrices: true, priceOlder: true }),
        labels: [],
        q: '',
        priceOlderDays: 45,
      },
      'books',
    )
    expect(params.noPrices).toBe('true')
    expect(params.priceOlder).toBe('true')
    expect(params.priceOlderDays).toBe('45')
  })

  it('emits desireToPurchase when those chips are on', () => {
    const params = bookFilterParamsForUrl(
      {
        chips: chips(),
        labels: [],
        q: '',
        desireToPurchase: [0, 'unset'],
      },
      'prices',
    )
    expect(params.desireToPurchase).toBe('0,unset')
  })

  it('emits withPrices when that chip is on', () => {
    const params = bookFilterParamsForUrl(
      {
        chips: chips({ withPrices: true }),
        labels: [],
        q: '',
      },
      'books',
    )
    expect(params.withPrices).toBe('true')
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
        chips: chips({ freeText: true, withoutLoc: true, mostRecent: true }),
        labels: ['fiction'],
        readingDifficulties: ['children', 'unset'],
        statuses: ['in-library'],
        q: 'Augustine',
        bookPage: 2,
        authorPage: 1,
      },
      'search',
    )
    expect(params).toEqual({
      q: 'Augustine',
      labels: 'fiction',
      readingDifficulty: 'children,unset',
      status: 'in-library',
      freeText: 'true',
      mostRecent: 'true',
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
        chips: chips({ withoutLoc: true }),
        labels: ['classic'],
        readingDifficulties: ['demanding'],
        statuses: ['in-library'],
        favoriteLists: ['Have Read'],
        q: 'Summa',
      }),
    ).toBe('/books?q=Summa&labels=classic&readingDifficulty=demanding&status=in-library&favoriteLists=Have+Read')
  })

  it('does not copy Search Recent Arrivals off-state onto Books intake', () => {
    expect(
      booksPathFromFilters({
        chips: chips({ mostRecent: false }),
        labels: [],
        q: '',
      }),
    ).toBe('/books')
  })
})

describe('pricesPathFromFilters', () => {
  it('opens /prices with no query when nothing is set', () => {
    expect(pricesPathFromFilters({ chips: chips({ mostRecent: false }), labels: [], q: '' })).toBe('/prices')
  })

  it('copies books filters including mostRecent onto /prices', () => {
    expect(
      pricesPathFromFilters({
        chips: chips({ mostRecent: true }),
        labels: ['classic'],
        statuses: ['in-library'],
        q: 'Summa',
      }),
    ).toBe('/prices?q=Summa&labels=classic&status=in-library&mostRecent=true')
  })

  it('copies Pricing chips and days onto /prices', () => {
    expect(
      pricesPathFromFilters({
        chips: chips({ noPrices: true, priceOlder: true }),
        labels: [],
        q: '',
        priceOlderDays: 45,
      }),
    ).toBe('/prices?noPrices=true&priceOlder=true&priceOlderDays=45')
  })

  it('copies Lookup Errors onto /prices', () => {
    expect(
      pricesPathFromFilters({
        chips: chips({ lookupErrors: true }),
        labels: [],
        q: '',
      }),
    ).toBe('/prices?lookupErrors=true')
  })

  it('copies Books with Pricing onto /prices', () => {
    expect(
      pricesPathFromFilters({
        chips: chips({ withPrices: true }),
        labels: [],
        q: '',
      }),
    ).toBe('/prices?withPrices=true')
  })
})

describe('booksPathFromPriceFilters', () => {
  it('opens /books with mostRecent=false when Prices has no filters', () => {
    expect(
      booksPathFromPriceFilters({ chips: chips({ mostRecent: false }), labels: [], q: '' }),
    ).toBe('/books?mostRecent=false')
  })

  it('copies Prices inventory filters onto /books', () => {
    expect(
      booksPathFromPriceFilters({
        chips: chips({ lookupErrors: true, noPrices: true }),
        labels: ['classic'],
        readingDifficulties: ['demanding'],
        statuses: ['in-library'],
        q: 'Summa',
      }),
    ).toBe(
      '/books?q=Summa&labels=classic&readingDifficulty=demanding&status=in-library&noPrices=true&lookupErrors=true',
    )
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
    expect(isBooksIntakeConstrained(chips(), [], '', ['children'])).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], 'narnia')).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], '', [], ['Have Read'])).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], '', [], [], ['in-library'])).toBe(true)
  })
})
