// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyDesireToPurchaseFilter,
  DESIRE_TO_PURCHASE_UNSET,
  desireToPurchaseFromSearchParams,
  matchesDesireToPurchaseFilter,
} from '@/utils/desireToPurchase'

describe('desireToPurchaseFromSearchParams', () => {
  it('parses known values and drops unknowns and duplicates', () => {
    expect(
      desireToPurchaseFromSearchParams(
        new URLSearchParams('desireToPurchase=0,5,unset,bogus,5,11,-1'),
      ),
    ).toEqual([0, 5, DESIRE_TO_PURCHASE_UNSET])
  })

  it('returns empty when the param is missing', () => {
    expect(desireToPurchaseFromSearchParams(new URLSearchParams('q=narnia'))).toEqual([])
  })
})

describe('matchesDesireToPurchaseFilter', () => {
  it('passes every book when nothing is selected', () => {
    expect(matchesDesireToPurchaseFilter(7, [])).toBe(true)
    expect(matchesDesireToPurchaseFilter(null, [])).toBe(true)
  })

  it('ORs selected values and treats null as Unset', () => {
    expect(matchesDesireToPurchaseFilter(0, [0, 10])).toBe(true)
    expect(matchesDesireToPurchaseFilter(5, [0, 10])).toBe(false)
    expect(matchesDesireToPurchaseFilter(null, [DESIRE_TO_PURCHASE_UNSET])).toBe(true)
    expect(matchesDesireToPurchaseFilter(undefined, [DESIRE_TO_PURCHASE_UNSET])).toBe(true)
    expect(matchesDesireToPurchaseFilter(7, [DESIRE_TO_PURCHASE_UNSET])).toBe(false)
  })
})

describe('applyDesireToPurchaseFilter', () => {
  it('keeps books matching any selected value', () => {
    const books = [
      { id: 1, desireToPurchase: 10 },
      { id: 2, desireToPurchase: 0 },
      { id: 3, desireToPurchase: null },
      { id: 4, desireToPurchase: 5 },
    ]
    expect(
      applyDesireToPurchaseFilter(books, [10, DESIRE_TO_PURCHASE_UNSET]).map((book) => book.id),
    ).toEqual([1, 3])
  })
})
