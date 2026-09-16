// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import type { BookPriceDto } from '@/types/dtos'
import { cheapestCoverTotal, summarizeBookPrices } from '@/utils/priceStatistics'

function price(overrides: Partial<BookPriceDto>): BookPriceDto {
  return {
    id: 1,
    bookId: 1,
    bookTitle: 'Pride and Prejudice',
    author: 'Jane Austen',
    cover: 'HARDCOVER',
    priceDollars: 4.86,
    shippingDollars: 0,
    totalDollars: 4.86,
    condition: 'Used - Good',
    lookedUpAt: '2026-09-10T12:00:00',
    ...overrides,
  }
}

describe('cheapestCoverTotal', () => {
  it('picks the less expensive of hardcover and softcover totals', () => {
    const cheapest = cheapestCoverTotal([
      price({ cover: 'HARDCOVER', totalDollars: 50, priceDollars: 50 }),
      price({ id: 2, cover: 'SOFTCOVER', totalDollars: 12, priceDollars: 8, shippingDollars: 4 }),
    ])
    expect(cheapest).toBe(12)
  })

  it('uses the only typed cover when the other is missing', () => {
    expect(cheapestCoverTotal([price({ cover: 'HARDCOVER', totalDollars: 25 })])).toBe(25)
    expect(
      cheapestCoverTotal([price({ cover: 'SOFTCOVER', totalDollars: 9, priceDollars: 9 })]),
    ).toBe(9)
  })

  it('includes library binding in the cheapest typed total', () => {
    expect(
      cheapestCoverTotal([
        price({ cover: 'HARDCOVER', totalDollars: 20, priceDollars: 20 }),
        price({ id: 2, cover: 'LIBRARY_BINDING', totalDollars: 8, priceDollars: 8 }),
      ]),
    ).toBe(8)
  })

  it('ignores failed lookups when a real listing exists', () => {
    const cheapest = cheapestCoverTotal([
      price({
        cover: 'HARDCOVER',
        priceDollars: null,
        shippingDollars: null,
        totalDollars: null,
        lookupError: 'No matching listing',
      }),
      price({ id: 2, cover: 'SOFTCOVER', totalDollars: 7, priceDollars: 7 }),
    ])
    expect(cheapest).toBe(7)
  })

  it('falls back to Other/Unknown only when neither typed cover has a price', () => {
    expect(
      cheapestCoverTotal([
        price({ cover: 'UNKNOWN', totalDollars: 3, priceDollars: 3 }),
        price({ id: 2, cover: 'HARDCOVER', totalDollars: 20, priceDollars: 20 }),
      ]),
    ).toBe(20)
    expect(cheapestCoverTotal([price({ cover: 'UNKNOWN', totalDollars: 6, priceDollars: 6 })])).toBe(
      6,
    )
  })

  it('returns null when there is no usable listing', () => {
    expect(cheapestCoverTotal([])).toBeNull()
    expect(
      cheapestCoverTotal([
        price({
          priceDollars: null,
          shippingDollars: null,
          totalDollars: null,
          lookupError: 'AbeBooks rate limited',
        }),
      ]),
    ).toBeNull()
  })
})

describe('summarizeBookPrices', () => {
  const books = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }, { id: 5 }, { id: 6 }]

  it('sums cheapest covers and counts cumulative over-$20 / $40 / $80 buckets', () => {
    const stats = summarizeBookPrices(books, [
      price({ id: 1, bookId: 1, cover: 'HARDCOVER', totalDollars: 10, priceDollars: 10 }),
      price({
        id: 2,
        bookId: 1,
        cover: 'SOFTCOVER',
        totalDollars: 8,
        priceDollars: 8,
      }),
      price({ id: 3, bookId: 2, cover: 'HARDCOVER', totalDollars: 25, priceDollars: 25 }),
      price({ id: 4, bookId: 2, cover: 'SOFTCOVER', totalDollars: 30, priceDollars: 30 }),
      price({ id: 5, bookId: 3, cover: 'HARDCOVER', totalDollars: 50, priceDollars: 50 }),
      price({ id: 6, bookId: 3, cover: 'SOFTCOVER', totalDollars: 45, priceDollars: 45 }),
      price({ id: 7, bookId: 4, cover: 'HARDCOVER', totalDollars: 100, priceDollars: 100 }),
      price({ id: 8, bookId: 4, cover: 'SOFTCOVER', totalDollars: 90, priceDollars: 90 }),
      price({ id: 9, bookId: 5, cover: 'HARDCOVER', totalDollars: 20, priceDollars: 20 }),
    ])

    expect(stats).toEqual({
      totalCost: 188,
      booksOver20: 3,
      booksOver40: 2,
      booksOver80: 1,
      totalBooks: 6,
      booksWithoutPrices: 1,
    })
  })

  it('treats failed lookups and missing rows as books without prices', () => {
    const stats = summarizeBookPrices([{ id: 1 }, { id: 2 }, { id: 3 }], [
      price({
        bookId: 1,
        priceDollars: null,
        shippingDollars: null,
        totalDollars: null,
        lookupError: 'No matching listing',
      }),
      price({
        id: 2,
        bookId: 2,
        priceDollars: null,
        shippingDollars: null,
        totalDollars: null,
        lookupError: 'AbeBooks HTTP 500',
      }),
    ])
    expect(stats.totalCost).toBe(0)
    expect(stats.booksWithoutPrices).toBe(3)
    expect(stats.totalBooks).toBe(3)
    expect(stats.booksOver20).toBe(0)
  })

  it('does not count a book at exactly the threshold as over that amount', () => {
    const stats = summarizeBookPrices([{ id: 1 }, { id: 2 }], [
      price({ bookId: 1, totalDollars: 20, priceDollars: 20 }),
      price({ id: 2, bookId: 2, totalDollars: 40, priceDollars: 40 }),
    ])
    expect(stats.booksOver20).toBe(1)
    expect(stats.booksOver40).toBe(0)
    expect(stats.booksOver80).toBe(0)
    expect(stats.totalCost).toBe(60)
  })

  it('returns empty stats for an empty book list', () => {
    expect(summarizeBookPrices([], [price({})])).toEqual({
      totalCost: 0,
      booksOver20: 0,
      booksOver40: 0,
      booksOver80: 0,
      totalBooks: 0,
      booksWithoutPrices: 0,
    })
  })
})
