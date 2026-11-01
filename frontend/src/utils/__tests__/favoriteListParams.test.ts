// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { joinFavoriteLists, splitFavoriteLists } from '../favoriteListParams'
import { bookListFilterQuery } from '@/api/books'
import { authorListFilterQuery } from '@/api/authors'
import { defaultBookChipFilters } from '@/utils/bookChipFilters'
import { defaultAuthorChipFilters } from '@/utils/authorChipFilters'
import { booksPathFromFilters } from '@/utils/bookFilterParams'

const commaName = 'ACLA, read in Pittsburgh'

describe('favorite list query values', () => {
  it('keeps a comma inside one list name through the page URL', () => {
    const params = new URLSearchParams(
      booksPathFromFilters({
        chips: defaultBookChipFilters,
        labels: [],
        favoriteLists: [commaName, 'Have Read'],
        q: '',
      }).split('?')[1],
    )
    expect(params.get('favoriteLists')).toBe(`~${encodeURIComponent(commaName)},Have Read`)
    expect(splitFavoriteLists(params.get('favoriteLists'))).toEqual([commaName, 'Have Read'])
  })

  it('leaves names without commas as a plain comma-separated list', () => {
    expect(joinFavoriteLists(['Have Read', 'Saints'])).toBe('Have Read,Saints')
    expect(splitFavoriteLists('Have Read, Saints')).toEqual(['Have Read', 'Saints'])
  })

  it('round-trips a name that starts with the encoding mark', () => {
    expect(splitFavoriteLists(joinFavoriteLists(['~Mine']))).toEqual(['~Mine'])
  })

  it('sends the encoded name on the books and authors catalog requests', () => {
    const bookQuery = bookListFilterQuery({
      chips: defaultBookChipFilters,
      favoriteLists: [commaName],
    })
    const authorQuery = authorListFilterQuery({
      chips: defaultAuthorChipFilters,
      favoriteLists: [commaName, 'Saints'],
    })
    expect(splitFavoriteLists(new URLSearchParams(bookQuery).get('favoriteLists'))).toEqual([commaName])
    expect(splitFavoriteLists(new URLSearchParams(authorQuery).get('favoriteLists'))).toEqual([
      commaName,
      'Saints',
    ])
  })

  it('survives the search request encoding', () => {
    const onceDecoded = decodeURIComponent(encodeURIComponent(joinFavoriteLists([commaName, 'Have Read'])))
    expect(splitFavoriteLists(onceDecoded)).toEqual([commaName, 'Have Read'])
  })
})
