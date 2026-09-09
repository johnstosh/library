// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { ReadingDifficulty } from '@/types/enums'
import {
  applyReadingDifficultyFilter,
  matchesReadingDifficultyFilter,
  normalizeReadingDifficulty,
  readingDifficultiesFromSearchParams,
} from '@/utils/readingDifficulty'

describe('normalizeReadingDifficulty', () => {
  it('maps null, blank, and unknown values to Unset', () => {
    expect(normalizeReadingDifficulty(null)).toBe(ReadingDifficulty.UNSET)
    expect(normalizeReadingDifficulty(undefined)).toBe(ReadingDifficulty.UNSET)
    expect(normalizeReadingDifficulty('')).toBe(ReadingDifficulty.UNSET)
    expect(normalizeReadingDifficulty('  ')).toBe(ReadingDifficulty.UNSET)
    expect(normalizeReadingDifficulty('not-a-level')).toBe(ReadingDifficulty.UNSET)
  })

  it('keeps known enum keys, case-insensitively', () => {
    expect(normalizeReadingDifficulty('children')).toBe(ReadingDifficulty.CHILDREN)
    expect(normalizeReadingDifficulty('DEMANDING')).toBe(ReadingDifficulty.DEMANDING)
  })
})

describe('readingDifficultiesFromSearchParams', () => {
  it('parses known keys and drops unknowns and duplicates', () => {
    expect(
      readingDifficultiesFromSearchParams(
        new URLSearchParams('readingDifficulty=children,unset,bogus,children'),
      ),
    ).toEqual([ReadingDifficulty.CHILDREN, ReadingDifficulty.UNSET])
  })

  it('returns empty when the param is missing', () => {
    expect(readingDifficultiesFromSearchParams(new URLSearchParams('q=narnia'))).toEqual([])
  })
})

describe('matchesReadingDifficultyFilter', () => {
  it('passes every book when nothing is selected', () => {
    expect(matchesReadingDifficultyFilter('children', [])).toBe(true)
    expect(matchesReadingDifficultyFilter(null, [])).toBe(true)
  })

  it('ORs selected values and treats null or blank as Unset', () => {
    expect(matchesReadingDifficultyFilter('children', ['children', 'accessible'])).toBe(true)
    expect(matchesReadingDifficultyFilter('moderate', ['children', 'accessible'])).toBe(false)
    expect(matchesReadingDifficultyFilter(null, ['unset'])).toBe(true)
    expect(matchesReadingDifficultyFilter('', ['unset'])).toBe(true)
    expect(matchesReadingDifficultyFilter('children', ['unset'])).toBe(false)
  })
})

describe('applyReadingDifficultyFilter', () => {
  it('keeps books matching any selected value', () => {
    const books = [
      { id: 1, readingDifficulty: 'children' },
      { id: 2, readingDifficulty: 'demanding' },
      { id: 3, readingDifficulty: null },
      { id: 4, readingDifficulty: '' },
    ]
    expect(
      applyReadingDifficultyFilter(books, ['children', 'unset']).map((book) => book.id),
    ).toEqual([1, 3, 4])
  })
})
