// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  computeShippingSavings,
  UNPUBLISHED_RATE_NOTE,
  type SavingsBook,
  type SavingsListing,
} from '@/utils/shippingSavings'

function book(id: number, title: string): SavingsBook {
  return { id, title, author: null }
}

function listing(partial: Partial<SavingsListing> & Pick<SavingsListing, 'bookId' | 'sellerId' | 'priceDollars' | 'shippingDollars'>): SavingsListing {
  return {
    listingId: partial.listingId ?? partial.bookId * 100 + partial.sellerId,
    listingTitle: partial.listingTitle ?? `Listing ${partial.bookId}`,
    sellerName: partial.sellerName ?? `Seller ${partial.sellerId}`,
    storefrontUrl: partial.storefrontUrl ?? null,
    detailsUrl: partial.detailsUrl ?? `https://www.abebooks.com/${partial.bookId}`,
    additionalShippingDollars: partial.additionalShippingDollars ?? 0,
    additionalShippingSource: partial.additionalShippingSource ?? 'PARSED',
    condition: partial.condition ?? 'Used - Good',
    binding: partial.binding ?? 'HARDCOVER',
    ...partial,
  }
}

describe('computeShippingSavings', () => {
  it('does not give a single book a false savings from the extra-book rate', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'Only')],
      listings: [listing({
        bookId: 1,
        sellerId: 1,
        sellerName: 'Solo',
        priceDollars: 10,
        shippingDollars: 5,
        additionalShippingDollars: 1,
      })],
    })
    expect(report.sellers).toEqual([])
    expect(report.totalSavings).toBe(0)
    expect(report.individual).toEqual([
      expect.objectContaining({ bookId: 1, baselineDollars: 15 }),
    ])
  })

  it('saves the shared first-book premium on a two-book bundle', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'A'), book(2, 'B')],
      listings: [1, 2].map((bookId) => listing({
        bookId,
        sellerId: 1,
        sellerName: 'Bundle',
        priceDollars: 10,
        shippingDollars: 5,
        additionalShippingDollars: 1,
      })),
    })
    expect(report.totalSavings).toBe(4)
    expect(report.sellers).toHaveLength(1)
    expect(report.sellers[0]).toMatchObject({
      sellerName: 'Bundle',
      basketDollars: 26,
      baselineDollars: 30,
      savingsDollars: 4,
      shippingEstimated: false,
      additionalShippingDollars: 1,
    })
    expect(report.individual).toEqual([])
  })

  it('leaves books individual when the bundle premium is not covered', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'A'), book(2, 'B')],
      listings: [
        listing({ bookId: 1, sellerId: 2, sellerName: 'Cheap', priceDollars: 5, shippingDollars: 0, additionalShippingDollars: 0, additionalShippingSource: 'FREE' }),
        listing({ bookId: 2, sellerId: 3, sellerName: 'Cheap B', priceDollars: 5, shippingDollars: 0, additionalShippingDollars: 0, additionalShippingSource: 'FREE' }),
        listing({ bookId: 1, sellerId: 1, sellerName: 'Dear', priceDollars: 4, shippingDollars: 6, additionalShippingDollars: 0 }),
        listing({ bookId: 2, sellerId: 1, sellerName: 'Dear', priceDollars: 4, shippingDollars: 6, additionalShippingDollars: 0 }),
      ],
    })
    expect(report.sellers).toEqual([])
    expect(report.shared).toEqual([])
    expect(report.totalSavings).toBe(0)
    expect(report.individual.map((row) => row.bookId).sort()).toEqual([1, 2])
  })

  it('reports an unknown multi-book seller without a savings number', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'A'), book(2, 'B')],
      listings: [1, 2].map((bookId) => listing({
        bookId,
        sellerId: 8,
        sellerName: 'Mystery',
        priceDollars: 10,
        shippingDollars: 4,
        additionalShippingDollars: null,
        additionalShippingSource: 'UNKNOWN',
      })),
    })
    expect(report.totalSavings).toBe(0)
    expect(report.sellers).toEqual([])
    expect(report.shared).toEqual([
      expect.objectContaining({
        sellerName: 'Mystery',
        note: UNPUBLISHED_RATE_NOTE,
        bookIds: [1, 2],
      }),
    ])
    expect(report.individual).toHaveLength(2)
  })

  it('prices an unknown seller when every chosen copy already ships free', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'A'), book(2, 'B')],
      listings: [
        listing({ bookId: 1, sellerId: 1, sellerName: 'Free Ship', priceDollars: 12, shippingDollars: 0, additionalShippingDollars: null, additionalShippingSource: 'UNKNOWN' }),
        listing({ bookId: 2, sellerId: 1, sellerName: 'Free Ship', priceDollars: 12, shippingDollars: 0, additionalShippingDollars: null, additionalShippingSource: 'UNKNOWN' }),
        listing({ bookId: 1, sellerId: 2, sellerName: 'Baseline A', priceDollars: 20, shippingDollars: 0, additionalShippingDollars: 0, additionalShippingSource: 'FREE' }),
        listing({ bookId: 2, sellerId: 3, sellerName: 'Baseline B', priceDollars: 20, shippingDollars: 0, additionalShippingDollars: 0, additionalShippingSource: 'FREE' }),
      ],
    })
    expect(report.shared).toEqual([])
    expect(report.sellers).toEqual([])
    expect(report.totalSavings).toBe(0)
    expect(report.individual).toHaveLength(2)
  })

  it('replaces one large seller with two smaller bundles when that saves more', async () => {
    const books = [book(1, 'A'), book(2, 'B'), book(3, 'C'), book(4, 'D')]
    const singles = books.map((row) => listing({
      bookId: row.id,
      sellerId: 100 + row.id,
      sellerName: `Single ${row.title}`,
      priceDollars: 1000,
      shippingDollars: 0,
      additionalShippingDollars: null,
      additionalShippingSource: 'UNKNOWN',
    }))
    const big = books.map((row) => listing({
      bookId: row.id,
      sellerId: 1,
      sellerName: 'Big',
      priceDollars: 775,
      shippingDollars: 400,
      additionalShippingDollars: 0,
    }))
    const pair = (sellerId: number, name: string, ids: number[]) => ids.map((bookId) => listing({
      bookId,
      sellerId,
      sellerName: name,
      priceDollars: 800,
      shippingDollars: 100,
      additionalShippingDollars: 0,
    }))
    const report = await computeShippingSavings({
      books,
      listings: [
        ...singles,
        ...big,
        ...pair(2, 'Pair One', [1, 2]),
        ...pair(3, 'Pair Two', [3, 4]),
      ],
    })
    expect(report.totalSavings).toBe(200)
    expect(report.sellers.map((seller) => seller.sellerName).sort()).toEqual(['Pair One', 'Pair Two'])
    expect(report.shared).toEqual([])
  })

  it('marks shipping as an estimate when first-book rates differ', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'A'), book(2, 'B')],
      listings: [
        listing({ bookId: 1, sellerId: 9, sellerName: 'Base', priceDollars: 20, shippingDollars: 0, additionalShippingSource: 'FREE' }),
        listing({ bookId: 2, sellerId: 10, sellerName: 'Base B', priceDollars: 20, shippingDollars: 0, additionalShippingSource: 'FREE' }),
        listing({ bookId: 1, sellerId: 1, sellerName: 'Mixed', priceDollars: 10, shippingDollars: 2, additionalShippingDollars: 1 }),
        listing({ bookId: 2, sellerId: 1, sellerName: 'Mixed', priceDollars: 10, shippingDollars: 5, additionalShippingDollars: 1 }),
      ],
    })
    expect(report.sellers[0]).toMatchObject({
      sellerName: 'Mixed',
      shippingEstimated: true,
      savingsDollars: 1,
      basketDollars: 26,
      baselineDollars: 27,
    })
  })

  it('excludes books that have not been looked up', async () => {
    const report = await computeShippingSavings({
      books: [book(1, 'Missing'), book(2, 'Present')],
      listings: [listing({ bookId: 2, sellerId: 1, priceDollars: 8, shippingDollars: 0 })],
    })
    expect(report.booksWithoutListings).toEqual([{ bookId: 1, catalogTitle: 'Missing' }])
    expect(report.individual.map((row) => row.bookId)).toEqual([2])
  })

  it('yields so the timer can paint', async () => {
    let yields = 0
    await computeShippingSavings(
      { books: [book(1, 'A')], listings: [] },
      async () => {
        yields += 1
      },
    )
    expect(yields).toBeGreaterThan(0)
  })
})
