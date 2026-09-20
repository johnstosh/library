// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyBookStatusFilter,
  areAllBookStatusFiltersSelected,
  BOOK_STATUS_FILTER_VALUES,
  bookStatusesFromSearchParams,
  BookStatusFilter,
  matchesBookStatusFilter,
} from '@/utils/bookStatus'

describe('bookStatusesFromSearchParams', () => {
  it('parses unique known keys and ignores unknown tokens', () => {
    expect(
      bookStatusesFromSearchParams(
        new URLSearchParams('status=in-library,requested,bogus,in-library'),
      ),
    ).toEqual([BookStatusFilter.IN_LIBRARY, BookStatusFilter.REQUESTED])
  })

  it('returns empty when status is missing', () => {
    expect(bookStatusesFromSearchParams(new URLSearchParams('q=narnia'))).toEqual([])
  })

  it('maps legacy chip params when status is absent', () => {
    expect(bookStatusesFromSearchParams(new URLSearchParams('inLib=true'))).toEqual([
      BookStatusFilter.IN_LIBRARY,
    ])
    expect(bookStatusesFromSearchParams(new URLSearchParams('elec=true'))).toEqual([
      BookStatusFilter.ELECTRONIC_RESOURCE,
    ])
    expect(bookStatusesFromSearchParams(new URLSearchParams('withoutLoc=true'))).toEqual([
      BookStatusFilter.WITHOUT_LOC,
    ])
    expect(bookStatusesFromSearchParams(new URLSearchParams('requestedStatus=true'))).toEqual([
      BookStatusFilter.REQUESTED,
    ])
    expect(bookStatusesFromSearchParams(new URLSearchParams('notActiveStatus=true'))).toEqual([
      BookStatusFilter.LOST,
      BookStatusFilter.WITHDRAWN,
      BookStatusFilter.ON_ORDER,
      BookStatusFilter.REQUESTED,
    ])
  })

  it('prefers status= over legacy chip params', () => {
    expect(
      bookStatusesFromSearchParams(new URLSearchParams('status=lost&inLib=true')),
    ).toEqual([BookStatusFilter.LOST])
  })
})

describe('matchesBookStatusFilter', () => {
  const activeInLib = { status: 'ACTIVE', locNumber: 'PS3511' }
  const activeElectronic = { status: 'ACTIVE', electronicResource: true }
  const activeOther = { status: 'ACTIVE' }
  const withoutLoc = { status: 'ACTIVE', locNumber: null, electronicResource: false }
  const withoutLocLost = { status: 'LOST', locNumber: null, electronicResource: false }
  const withoutLocWithdrawn = { status: 'WITHDRAWN', locNumber: null }
  const withoutLocOnOrder = { status: 'ON_ORDER', locNumber: null }
  const withoutLocRequested = { status: 'REQUESTED', locNumber: null }
  const lost = { status: 'LOST', locNumber: 'PS3511' }
  const withdrawn = { status: 'WITHDRAWN', locNumber: 'PS3511' }
  const requested = { status: 'REQUESTED' }
  const onOrder = { status: 'ON_ORDER' }
  const electronicWithoutLoc = { status: 'ACTIVE', locNumber: null, electronicResource: true }

  it('when empty, hides WITHDRAWN and REQUESTED and keeps the rest', () => {
    expect(matchesBookStatusFilter(activeInLib, [])).toBe(true)
    expect(matchesBookStatusFilter(lost, [])).toBe(true)
    expect(matchesBookStatusFilter(onOrder, [])).toBe(true)
    expect(matchesBookStatusFilter(withdrawn, [])).toBe(false)
    expect(matchesBookStatusFilter(requested, [])).toBe(false)
  })

  it('ORs selected values; in-library, electronic-resource, and without-loc are Active-only', () => {
    const selected = ['in-library', 'electronic-resource']
    expect(matchesBookStatusFilter(activeInLib, selected)).toBe(true)
    expect(matchesBookStatusFilter(activeElectronic, selected)).toBe(true)
    expect(matchesBookStatusFilter(activeOther, selected)).toBe(false)
    expect(matchesBookStatusFilter(withoutLoc, ['without-loc'])).toBe(true)
    expect(matchesBookStatusFilter(withoutLocLost, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(withoutLocWithdrawn, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(withoutLocOnOrder, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(withoutLocRequested, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(electronicWithoutLoc, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(activeElectronic, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(activeInLib, ['without-loc'])).toBe(false)
    expect(matchesBookStatusFilter(lost, selected)).toBe(false)
    expect(matchesBookStatusFilter(requested, ['requested'])).toBe(true)
    expect(matchesBookStatusFilter(activeInLib, ['requested'])).toBe(false)
  })

  it('when every status chip is selected, includes books that match no individual chip', () => {
    const all = [...BOOK_STATUS_FILTER_VALUES]
    expect(areAllBookStatusFiltersSelected(['in-library', 'lost'])).toBe(false)
    expect(areAllBookStatusFiltersSelected(all)).toBe(true)
    expect(areAllBookStatusFiltersSelected([...all, 'bogus'])).toBe(true)
    expect(matchesBookStatusFilter(activeOther, all)).toBe(true)
    expect(matchesBookStatusFilter(withdrawn, all)).toBe(true)
    expect(matchesBookStatusFilter(requested, all)).toBe(true)
    expect(matchesBookStatusFilter({ status: null }, all)).toBe(true)
  })
})

describe('applyBookStatusFilter', () => {
  it('ORs selected statuses and keeps default hiding when none are selected', () => {
    const books = [
      { id: 1, status: 'ACTIVE', locNumber: 'PS3511' },
      { id: 2, status: 'ACTIVE', electronicResource: true },
      { id: 3, status: 'LOST' },
      { id: 4, status: 'REQUESTED' },
      { id: 5, status: 'WITHDRAWN' },
    ]
    expect(applyBookStatusFilter(books, []).map((book) => book.id)).toEqual([1, 2, 3])
    expect(
      applyBookStatusFilter(books, ['in-library', 'requested']).map((book) => book.id),
    ).toEqual([1, 4])
    // without-loc now requires ACTIVE (excludes non-active without LOC)
    expect(
      applyBookStatusFilter(
        [...books, { id: 6, status: 'ACTIVE', locNumber: null }],
        ['without-loc'],
      ).map((book) => book.id),
    ).toEqual([6])
    expect(
      applyBookStatusFilter(
        [...books, { id: 6, status: 'ACTIVE', locNumber: null }],
        [...BOOK_STATUS_FILTER_VALUES],
      ).map((book) => book.id),
    ).toEqual([1, 2, 3, 4, 5, 6])
  })
})
