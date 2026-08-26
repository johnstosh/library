// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyAuthorChipFilters,
  defaultAuthorChipFilters,
  type AuthorChipFilters,
} from '../authorChipFilters'
import type { AuthorDto } from '@/types/dtos'

function author(overrides: Partial<AuthorDto> & { id: number; name: string }): AuthorDto {
  return {
    lastModified: '2026-01-01T00:00:00',
    ...overrides,
  }
}

const chips = (overrides: Partial<AuthorChipFilters> = {}): AuthorChipFilters => ({
  ...defaultAuthorChipFilters,
  ...overrides,
})

const sample: AuthorDto[] = [
  author({ id: 1, name: 'Has Bio', briefBiography: 'A life.', grokipediaUrl: 'https://g', bookCount: 2, firstPhotoId: 9, dateOfBirth: '1900-01-01', dateOfDeath: '1980-01-01' }),
  author({ id: 2, name: 'Empty', bookCount: 0 }),
]

describe('applyAuthorChipFilters', () => {
  it('returns everyone when no chips are on', () => {
    expect(applyAuthorChipFilters(sample, chips()).map((a) => a.id)).toEqual([1, 2])
  })

  it('ANDs without-description and zero-books', () => {
    expect(
      applyAuthorChipFilters(sample, chips({ withoutDescription: true, zeroBooks: true })).map((a) => a.id)
    ).toEqual([2])
  })

  it('restricts to most-recent ids when that chip is on', () => {
    expect(
      applyAuthorChipFilters(sample, chips({ mostRecent: true }), new Set([1])).map((a) => a.id)
    ).toEqual([1])
  })

  it('without-photos keeps authors with no firstPhotoId', () => {
    expect(applyAuthorChipFilters(sample, chips({ withoutPhotos: true })).map((a) => a.id)).toEqual([2])
  })

  it('with-grokipedia and with-photos keep authors that have those', () => {
    expect(applyAuthorChipFilters(sample, chips({ withGrokipedia: true })).map((a) => a.id)).toEqual([1])
    expect(applyAuthorChipFilters(sample, chips({ withPhotos: true })).map((a) => a.id)).toEqual([1])
  })
})
