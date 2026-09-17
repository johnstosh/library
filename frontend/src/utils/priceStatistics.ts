// (c) Copyright 2025 by Muczynski
import type { BookPriceDto } from '@/types/dtos'
import { isSavedPriceListing } from '@/utils/bookChipFilters'

export interface BookPriceStatistics {
  /** Sum of the cheaper hardcover/softcover/library-binding total for each book that has a usable listing. */
  totalCost: number
  booksOver20: number
  booksOver40: number
  booksOver80: number
  totalBooks: number
  booksWithoutPrices: number
}

const EMPTY_STATS: BookPriceStatistics = {
  totalCost: 0,
  booksOver20: 0,
  booksOver40: 0,
  booksOver80: 0,
  totalBooks: 0,
  booksWithoutPrices: 0,
}

function listingTotal(price: BookPriceDto): number | null {
  if (!isSavedPriceListing(price)) return null
  if (price.totalDollars != null && Number.isFinite(price.totalDollars)) {
    return price.totalDollars
  }
  if (price.priceDollars == null || !Number.isFinite(price.priceDollars)) return null
  const shipping =
    price.shippingDollars != null && Number.isFinite(price.shippingDollars)
      ? price.shippingDollars
      : 0
  return price.priceDollars + shipping
}

function minAmount(current: number | null, candidate: number): number {
  return current == null || candidate < current ? candidate : current
}

/**
 * Cheapest usable total for a book: min of hardcover, softcover, and
 * library binding. Other/Unknown listings fill in only when none of those
 * typed covers has a price.
 */
export function cheapestCoverTotal(prices: BookPriceDto[]): number | null {
  let typed: number | null = null
  let other: number | null = null
  for (const price of prices) {
    const total = listingTotal(price)
    if (total == null) continue
    if (
      price.cover === 'HARDCOVER' ||
      price.cover === 'SOFTCOVER' ||
      price.cover === 'LIBRARY_BINDING'
    ) {
      typed = minAmount(typed, total)
    } else {
      other = minAmount(other, total)
    }
  }
  return typed != null ? typed : other
}

/**
 * Catalog price summary for the given books. Each book contributes its cheaper
 * hardcover/softcover/library-binding total (item + shipping). Threshold counts are strictly
 * greater than $20 / $40 / $80 and are cumulative. Books with no usable listing
 * are omitted from the total and counted in {@code booksWithoutPrices}.
 */
export function summarizeBookPrices(
  books: ReadonlyArray<{ id: number }>,
  prices: BookPriceDto[],
): BookPriceStatistics {
  if (books.length === 0) {
    return { ...EMPTY_STATS }
  }

  const pricesByBook = new Map<number, BookPriceDto[]>()
  for (const price of prices) {
    const list = pricesByBook.get(price.bookId)
    if (list) {
      list.push(price)
    } else {
      pricesByBook.set(price.bookId, [price])
    }
  }

  let totalCost = 0
  let booksOver20 = 0
  let booksOver40 = 0
  let booksOver80 = 0
  let booksWithoutPrices = 0

  for (const book of books) {
    const cheapest = cheapestCoverTotal(pricesByBook.get(book.id) ?? [])
    if (cheapest == null) {
      booksWithoutPrices += 1
      continue
    }
    totalCost += cheapest
    if (cheapest > 80) booksOver80 += 1
    if (cheapest > 40) booksOver40 += 1
    if (cheapest > 20) booksOver20 += 1
  }

  return {
    totalCost: Math.round(totalCost * 100) / 100,
    booksOver20,
    booksOver40,
    booksOver80,
    totalBooks: books.length,
    booksWithoutPrices,
  }
}
