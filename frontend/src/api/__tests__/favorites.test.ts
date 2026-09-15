// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  itemListsFromSummary,
  type FavoriteSummaryDto,
} from '../favorites'

const summary: FavoriteSummaryDto = {
  favoriteBookIds: [7, 9],
  favoriteAuthorIds: [4],
  lists: [
    { listName: 'Have Read', bookIds: [7], authorIds: [4] },
    { listName: 'Nightstand', bookIds: [9], authorIds: [] },
    { listName: 'Zebra Shelf', bookIds: [7], authorIds: [] },
  ],
  availableLists: ['Have Read', 'Want to Read', 'Want to Recommend', 'Nightstand', 'Zebra Shelf'],
}

describe('itemListsFromSummary', () => {
  it('uses summary availableLists and selects lists that contain the item', () => {
    const result = itemListsFromSummary(summary, 'BOOK', 7, true)
    expect(result.availableLists).toEqual(summary.availableLists)
    expect(result.selectedLists).toEqual(['Have Read', 'Zebra Shelf'])
  })

  it('falls back to built-in plus custom names when availableLists is missing', () => {
    const withoutAvailable: FavoriteSummaryDto = {
      ...summary,
      availableLists: undefined,
    }
    const patron = itemListsFromSummary(withoutAvailable, 'BOOK', 7, false)
    expect(patron.availableLists).toEqual([
      'Have Read',
      'Want to Read',
      'Want to Recommend',
      'Nightstand',
      'Zebra Shelf',
    ])
    expect(patron.selectedLists).toEqual(['Have Read', 'Zebra Shelf'])

    const librarian = itemListsFromSummary(withoutAvailable, 'AUTHOR', 4, true)
    expect(librarian.availableLists).toContain('Needs Review')
    expect(librarian.availableLists).toContain('Need to Locate')
    expect(librarian.selectedLists).toEqual(['Have Read'])
  })

  it('shows built-in lists immediately when summary has not loaded', () => {
    const patron = itemListsFromSummary(undefined, 'BOOK', 7, false)
    expect(patron.availableLists).toEqual(['Have Read', 'Want to Read', 'Want to Recommend'])
    expect(patron.selectedLists).toEqual([])
  })
})
