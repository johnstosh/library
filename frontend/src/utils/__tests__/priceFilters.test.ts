// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import type { BookPriceDto } from '@/types/dtos'
import {
  applyPriceFilters,
  coverLabel,
  defaultPriceChipFilters,
  parseMaxTotal,
} from '@/utils/priceFilters'
import { formatUsd } from '@/utils/formatters'

function price(overrides: Partial<BookPriceDto>): BookPriceDto {
  return {
    id: 1,
    bookId: 10,
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

describe('applyPriceFilters', () => {
  const rows = [
    price({ id: 1, cover: 'HARDCOVER', totalDollars: 4.86 }),
    price({ id: 2, cover: 'SOFTCOVER', totalDollars: 12, priceDollars: 8, shippingDollars: 4 }),
    price({
      id: 3,
      cover: 'HARDCOVER',
      priceDollars: null,
      shippingDollars: null,
      totalDollars: null,
      lookupError: 'No matching listing',
    }),
    price({ id: 4, cover: 'UNKNOWN', totalDollars: 15, priceDollars: 15, shippingDollars: 0 }),
  ]

  it('keeps totals strictly less than maxTotal', () => {
    const filtered = applyPriceFilters(rows, defaultPriceChipFilters, '10')
    expect(filtered.map((row) => row.id)).toEqual([1])
  })

  it('filters by cover chip', () => {
    const filtered = applyPriceFilters(rows, { ...defaultPriceChipFilters, softcover: true }, '')
    expect(filtered.map((row) => row.id)).toEqual([2])
  })

  it('filters Other/Unknown covers', () => {
    const filtered = applyPriceFilters(rows, { ...defaultPriceChipFilters, otherUnknown: true }, '')
    expect(filtered.map((row) => row.id)).toEqual([4])
  })

  it('ORs hardcover with Other/Unknown', () => {
    const filtered = applyPriceFilters(
      rows,
      { ...defaultPriceChipFilters, hardcover: true, otherUnknown: true },
      '',
    )
    expect(filtered.map((row) => row.id)).toEqual([1, 3, 4])
  })

  it('filters lookup failures', () => {
    const filtered = applyPriceFilters(rows, { ...defaultPriceChipFilters, lookupFailed: true }, '')
    expect(filtered.map((row) => row.id)).toEqual([3])
  })

  it('filters looked up recently by hours', () => {
    const now = Date.parse('2026-09-10T12:00:00Z')
    const recentRows = [
      price({ id: 1, lookedUpAt: '2026-09-10T11:00:00Z' }),
      price({ id: 2, lookedUpAt: '2026-09-09T12:00:00Z' }),
    ]
    const filtered = applyPriceFilters(
      recentRows,
      { ...defaultPriceChipFilters, recent: true },
      '',
      now,
      6,
    )
    expect(filtered.map((row) => row.id)).toEqual([1])
  })
})

describe('parseMaxTotal', () => {
  it('parses a non-negative number', () => {
    expect(parseMaxTotal('12.50')).toBe(12.5)
    expect(parseMaxTotal('')).toBeNull()
    expect(parseMaxTotal('nope')).toBeNull()
  })
})

describe('coverLabel', () => {
  it('labels hardcover, softcover, and other/unknown', () => {
    expect(coverLabel('HARDCOVER')).toBe('Hardcover')
    expect(coverLabel('SOFTCOVER')).toBe('Softcover')
    expect(coverLabel('UNKNOWN')).toBe('Other/Unknown')
  })
})

describe('formatUsd', () => {
  it('formats dollars and missing values', () => {
    expect(formatUsd(4.86)).toBe('$4.86')
    expect(formatUsd(null)).toBe('—')
  })
})
