// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyAuthorChipFilters,
  defaultAuthorChipFilters,
  isAvailabilityChipActive,
  isOtherAuthorChipActive,
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

describe('defaultAuthorChipFilters', () => {
  it('starts with YDL/EMU chips off', () => {
    expect(isAvailabilityChipActive(defaultAuthorChipFilters)).toBe(false)
  })

  it('defaults every chip to off, including mostRecent', () => {
    // Shared defaults stay off. The Authors page turns mostRecent on in
    // uiStore so it can use the faster GET /authors/most-recent-day backend.
    expect(defaultAuthorChipFilters.mostRecent).toBe(false)
  })
})

describe('isOtherAuthorChipActive', () => {
  it('ignores mostRecent and detects any other chip', () => {
    expect(isOtherAuthorChipActive(chips({ mostRecent: true }))).toBe(false)
    expect(isOtherAuthorChipActive(chips({ mostRecent: true, withoutDescription: true }))).toBe(true)
  })
})

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

  it('without-grokipedia treats "-" as N/A', () => {
    const authors = [
      author({ id: 1, name: 'Has URL', grokipediaUrl: 'https://g' }),
      author({ id: 2, name: 'N/A', grokipediaUrl: '-' }),
    ]
    expect(applyAuthorChipFilters(authors, chips({ withoutGrokipedia: true })).map((a) => a.id)).toEqual([2])
    expect(applyAuthorChipFilters(authors, chips({ withGrokipedia: true })).map((a) => a.id)).toEqual([1])
  })

  it('YDL/EMU chips keep authors who have that holding on any book', () => {
    const availability = new Map([
      [1, { hasYdlBook: true, hasEmuEbook: true, hasYdlAudio: true }],
      [2, { hasYdlEbook: true }],
    ])
    expect(
      applyAuthorChipFilters(sample, chips({ hasYdlBook: true }), undefined, availability).map((a) => a.id)
    ).toEqual([1])
    expect(
      applyAuthorChipFilters(sample, chips({ hasYdlEbook: true }), undefined, availability).map((a) => a.id)
    ).toEqual([2])
    expect(
      applyAuthorChipFilters(sample, chips({ hasYdlBook: true, hasEmuEbook: true }), undefined, availability).map((a) => a.id)
    ).toEqual([1])
    expect(
      applyAuthorChipFilters(sample, chips({ hasEmuAudio: true }), undefined, availability).map((a) => a.id)
    ).toEqual([])
  })
})
