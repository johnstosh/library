// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import type { BookDto } from '@/types/dtos'
import {
  applyChipFilters,
  defaultBookChipFilters,
  isAnyChipActive,
  isOtherBookChipActive,
  type BookChipFilters,
} from '@/utils/bookChipFilters'

function book(overrides: Partial<BookDto> = {}): BookDto {
  return {
    id: 1,
    title: 'Test Book',
    status: 'ACTIVE',
    lastModified: '2026-01-01T00:00:00',
    ...overrides,
  }
}

const allOff: BookChipFilters = { ...defaultBookChipFilters }

function chips(overrides: Partial<BookChipFilters>): BookChipFilters {
  return { ...allOff, ...overrides }
}

describe('defaultBookChipFilters', () => {
  it('defaults every chip to off, including mostRecent', () => {
    // Shared defaults stay off (Search). The Books page turns mostRecent on
    // from an empty /books URL (see bookFilterParams.ts).
    expect(defaultBookChipFilters.mostRecent).toBe(false)
    expect(isAnyChipActive(defaultBookChipFilters)).toBe(false)
  })
})

describe('isOtherBookChipActive', () => {
  it('ignores mostRecent and detects any other chip', () => {
    expect(isOtherBookChipActive(chips({ mostRecent: true }))).toBe(false)
    expect(isOtherBookChipActive(chips({ withoutLoc: true }))).toBe(true)
    expect(isOtherBookChipActive(chips({ hasYdlAudio: true }))).toBe(true)
    expect(isOtherBookChipActive(chips({ mostRecent: true, electronic: true }))).toBe(true)
  })
})

describe('applyChipFilters', () => {
  describe('notActiveStatus', () => {
    const active = book({ id: 1, status: 'ACTIVE', title: 'Active' })
    const lost = book({ id: 2, status: 'LOST', title: 'Lost' })
    const withdrawn = book({ id: 3, status: 'WITHDRAWN', title: 'Withdrawn' })
    const onOrder = book({ id: 4, status: 'ON_ORDER', title: 'On order' })
    const books = [active, lost, withdrawn, onOrder]

    it('when off, hides WITHDRAWN and still shows ACTIVE, LOST, ON_ORDER', () => {
      const result = applyChipFilters(books, chips({ notActiveStatus: false }))
      expect(result.map((b) => b.status)).toEqual(['ACTIVE', 'LOST', 'ON_ORDER'])
    })

    it('when on, hides ACTIVE and shows LOST, WITHDRAWN, ON_ORDER', () => {
      const result = applyChipFilters(books, chips({ notActiveStatus: true }))
      expect(result.map((b) => b.status)).toEqual(['LOST', 'WITHDRAWN', 'ON_ORDER'])
    })
  })

  it('inLibrary keeps only books with a non-blank locNumber', () => {
    const withLoc = book({ id: 1, locNumber: 'PS3511' })
    const blank = book({ id: 2, locNumber: '  ' })
    const missing = book({ id: 3, locNumber: undefined })
    const result = applyChipFilters([withLoc, blank, missing], chips({ inLibrary: true }))
    expect(result.map((b) => b.id)).toEqual([1])
  })

  it('electronic keeps only electronicResource books', () => {
    const elec = book({ id: 1, electronicResource: true })
    const paper = book({ id: 2, electronicResource: false })
    const result = applyChipFilters([elec, paper], chips({ electronic: true }))
    expect(result.map((b) => b.id)).toEqual([1])
  })

  it('freeText keeps only books with a non-blank freeTextUrl', () => {
    const withUrl = book({ id: 1, freeTextUrl: 'https://gutenberg.org/1' })
    const blank = book({ id: 2, freeTextUrl: '' })
    const result = applyChipFilters([withUrl, blank], chips({ freeText: true }))
    expect(result.map((b) => b.id)).toEqual([1])
  })

  it('audio keeps only librivox URLs, case-insensitive', () => {
    const audio = book({ id: 1, freeTextUrl: 'https://LibriVox.org/city-of-god' })
    const gutenberg = book({ id: 2, freeTextUrl: 'https://gutenberg.org/1' })
    const result = applyChipFilters([audio, gutenberg], chips({ audio: true }))
    expect(result.map((b) => b.id)).toEqual([1])
  })

  it('withoutLoc keeps books that have no locNumber', () => {
    const withLoc = book({ id: 1, locNumber: 'PS3511' })
    const without = book({ id: 2, locNumber: '' })
    const result = applyChipFilters([withLoc, without], chips({ withoutLoc: true }))
    expect(result.map((b) => b.id)).toEqual([2])
  })

  it('withoutGrokipedia keeps books with no grokipediaUrl', () => {
    const withUrl = book({ id: 1, grokipediaUrl: 'https://grokipedia.com/x' })
    const without = book({ id: 2, grokipediaUrl: '' })
    const result = applyChipFilters([withUrl, without], chips({ withoutGrokipedia: true }))
    expect(result.map((b) => b.id)).toEqual([2])
  })

  it('withGrokipedia keeps books that have a grokipediaUrl', () => {
    const withUrl = book({ id: 1, grokipediaUrl: 'https://grokipedia.com/x' })
    const without = book({ id: 2, grokipediaUrl: '' })
    const result = applyChipFilters([withUrl, without], chips({ withGrokipedia: true }))
    expect(result.map((b) => b.id)).toEqual([1])
  })

  it('withoutGenres keeps books with no tags', () => {
    const tagged = book({ id: 1, tagsList: ['fiction'] })
    const untagged = book({ id: 2, tagsList: [] })
    const result = applyChipFilters([tagged, untagged], chips({ withoutGenres: true }))
    expect(result.map((b) => b.id)).toEqual([2])
  })

  it('withoutFreeTextUrls keeps books with no freeTextUrl', () => {
    const withUrl = book({ id: 1, freeTextUrl: 'https://gutenberg.org/1' })
    const without = book({ id: 2 })
    const result = applyChipFilters([withUrl, without], chips({ withoutFreeTextUrls: true }))
    expect(result.map((b) => b.id)).toEqual([2])
  })

  it('YDL and EMU chips keep books with that holding', () => {
    const ydlAudio = book({ id: 1, ydlAudioAvailable: true })
    const ydlPaper = book({ id: 2, ydlPaperAvailable: true })
    const emuEbook = book({ id: 3, emuEbookAvailable: true })
    const none = book({ id: 4 })
    const sample = [ydlAudio, ydlPaper, emuEbook, none]
    expect(applyChipFilters(sample, chips({ hasYdlAudio: true })).map((b) => b.id)).toEqual([1])
    expect(applyChipFilters(sample, chips({ hasYdlBook: true })).map((b) => b.id)).toEqual([2])
    expect(applyChipFilters(sample, chips({ hasEmuEbook: true })).map((b) => b.id)).toEqual([3])
  })

  it('ANDs active chips so conflicting filters yield empty', () => {
    const physical = book({ id: 1, locNumber: 'PS3511' })
    const result = applyChipFilters([physical], chips({ inLibrary: true, withoutLoc: true }))
    expect(result).toEqual([])
  })

  describe('mostRecent', () => {
    it('keeps books on the most recent UTC day and the prior UTC day', () => {
      const recent = book({
        id: 1,
        dateAddedToLibrary: '2026-08-26T15:00:00Z',
      })
      const previousDay = book({
        id: 2,
        dateAddedToLibrary: '2026-08-25T01:00:00Z',
      })
      const older = book({
        id: 3,
        dateAddedToLibrary: '2026-08-24T23:59:59Z',
      })
      const result = applyChipFilters(
        [recent, previousDay, older],
        chips({ mostRecent: true }),
      )
      expect(result.map((b) => b.id)).toEqual([1, 2])
    })

    it('keeps temporary date-format titles even without a recent date', () => {
      const temp = book({
        id: 1,
        title: '2026-8-1 untitled scan',
        dateAddedToLibrary: '2020-01-01T00:00:00Z',
      })
      const dated = book({
        id: 2,
        title: 'Ordinary title',
        dateAddedToLibrary: '2026-08-26T12:00:00Z',
      })
      const result = applyChipFilters([temp, dated], chips({ mostRecent: true }))
      expect(result.map((b) => b.id)).toEqual([1, 2])
    })
  })
})
