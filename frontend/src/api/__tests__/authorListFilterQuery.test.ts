// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { authorListFilterQuery } from '../authors'
import { defaultAuthorChipFilters } from '@/utils/authorChipFilters'

describe('authorListFilterQuery', () => {
  it('sends a trimmed name query with active chips and favorite lists', () => {
    const query = authorListFilterQuery({
      chips: { ...defaultAuthorChipFilters, zeroBooks: true },
      q: '  Augustine  ',
      favoriteLists: ['Saints'],
    })
    const params = new URLSearchParams(query)
    expect(params.get('q')).toBe('Augustine')
    expect(params.get('zeroBooks')).toBe('true')
    expect(params.get('favoriteLists')).toBe('Saints')
    expect(params.get('mostRecent')).toBeNull()
  })

  it('omits a blank query', () => {
    const query = authorListFilterQuery({
      chips: defaultAuthorChipFilters,
      q: '   ',
    })
    expect(new URLSearchParams(query).has('q')).toBe(false)
    expect(query).toBe('')
  })
})
