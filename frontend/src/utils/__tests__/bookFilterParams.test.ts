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
    expect(isSearchVisibleChip('hasAclaAudio')).toBe(true)
    expect(isSearchVisibleChip('withoutGrokipedia')).toBe(false)
    expect(isSearchVisibleChip('withoutProperPlotOrDescription')).toBe(false)
    expect(isSearchVisibleChip('mostRecent')).toBe(true)
    expect(isSearchVisibleChip('withPrices')).toBe(false)
    expect(isSearchVisibleChip('noPrices')).toBe(false)
    expect(isSearchVisibleChip('priceOlder')).toBe(false)
    expect(isSearchVisibleChip('lookupErrors')).toBe(false)
    expect(SEARCH_VISIBLE_CHIPS).not.toContain('withGrokipedia')
    expect(SEARCH_VISIBLE_CHIPS).not.toContain('withoutProperPlotOrDescription')
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
    const params = new URLSearchParams('freeText=true&withoutGrokipedia=true&mostRecent=true')
    const result = chipsFromSearchParams(params, 'search')
    expect(result.freeText).toBe(true)
    expect(result.withoutGrokipedia).toBe(false)
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

  it('respects explicit mostRecent=true even with other chips, labels, or q (combinable)', () => {
    expect(
      chipsFromSearchParams(
        new URLSearchParams('status=without-loc&mostRecent=true'),
        'books',
      ).mostRecent,
    ).toBe(true)
    expect(
      chipsFromSearchParams(new URLSearchParams('labels=fiction&mostRecent=true'), 'books').mostRecent,
    ).toBe(true)
    expect(
      chipsFromSearchParams(
        new URLSearchParams('readingDifficulty=children&mostRecent=true'),
        'books',
      ).mostRecent,
    ).toBe(true)
    expect(
      chipsFromSearchParams(new URLSearchParams('binding=HARDCOVER&mostRecent=true'), 'books')
        .mostRecent,
    ).toBe(true)
    expect(
      chipsFromSearchParams(new URLSearchParams('q=narnia&mostRecent=true'), 'books').mostRecent,
    ).toBe(true)
    expect(
      chipsFromSearchParams(new URLSearchParams('noPrices=true&mostRecent=true'), 'books')
        .mostRecent,
    ).toBe(true)
  })

  it('defaults mostRecent off only when other filters present and no explicit param', () => {
    expect(chipsFromSearchParams(new URLSearchParams('status=without-loc'), 'books').mostRecent).toBe(
      false,
    )
    expect(chipsFromSearchParams(new URLSearchParams('labels=fiction'), 'books').mostRecent).toBe(
      false,
    )
    expect(
      chipsFromSearchParams(new URLSearchParams('q=narnia'), 'books').mostRecent,
    ).toBe(false)
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
  it('omits default Books intake (mostRecent=true with no others) from the URL', () => {
    expect(
      bookFilterParamsForUrl(
        { chips: chips({ mostRecent: true }), labels: [], q: '' },
        'books',
      ),
    ).toEqual({})
  })

  it('writes mostRecent param honestly when combined with other filters or explicitly off', () => {
    expect(
      bookFilterParamsForUrl(
        { chips: chips({ mostRecent: false }), labels: [], q: '' },
        'books',
      ),
    ).toEqual({ mostRecent: 'false' })

    expect(
      bookFilterParamsForUrl(
        { chips: chips({ mostRecent: true, withoutGrokipedia: true, withoutProperPlotOrDescription: true }), labels: [], q: '' },
        'books',
      ),
    ).toEqual({ mostRecent: 'true', withoutGrokipedia: 'true', withoutProperPlotOrDescription: 'true' })
  })

  it('writes binding chips', () => {
    const params = bookFilterParamsForUrl(
      {
        chips: chips({ mostRecent: false }),
        labels: [],
        bindings: ['HARDCOVER', 'UNKNOWN'],
        q: '',
      },
      'books',
    )
    expect(params.binding).toBe('HARDCOVER,UNKNOWN')
  })

  it('writes discovery chips, labels, and q for Search and omits cataloger chips', () => {
    const params = bookFilterParamsForUrl(
      {
        chips: chips({ freeText: true, withoutGrokipedia: true, mostRecent: true }),
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

  it('copies discovery filters and query onto /books (mostRecent omitted for default intake)', () => {
    expect(
      booksPathFromFilters({
        chips: chips({ withoutGrokipedia: true }),
        labels: ['classic'],
        readingDifficulties: ['demanding'],
        statuses: ['in-library'],
        favoriteLists: ['Have Read'],
        q: 'Summa',
      }),
    ).toBe('/books?q=Summa&labels=classic&readingDifficulty=demanding&status=in-library&favoriteLists=Have+Read&mostRecent=true')
  })

  it('copies mostRecent=true from Search even if other filters present (now combinable)', () => {
    expect(
      booksPathFromFilters({
        chips: chips({ mostRecent: true, freeText: true }),
        labels: [],
        q: '',
      }),
    ).toBe('/books?freeText=true&mostRecent=true')
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
  it('opens /books with mostRecent=false when Prices has no filters (default behavior)', () => {
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
      '/books?q=Summa&labels=classic&readingDifficulty=demanding&status=in-library&noPrices=true&lookupErrors=true&mostRecent=false',
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
    expect(isBooksIntakeConstrained(chips({ withoutGrokipedia: true }), [], '')).toBe(true)
    expect(isBooksIntakeConstrained(chips({ withoutProperPlotOrDescription: true }), [], '')).toBe(true)
    expect(isBooksIntakeConstrained(chips(), ['fiction'], '')).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], '', ['children'])).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], 'narnia')).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], '', [], ['Have Read'])).toBe(true)
    expect(isBooksIntakeConstrained(chips(), [], '', [], [], ['in-library'])).toBe(true)
  })
})
