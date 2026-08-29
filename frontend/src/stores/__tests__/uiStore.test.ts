// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it } from 'vitest'
import { useUiStore } from '@/stores/uiStore'
import { defaultBookChipFilters } from '@/utils/bookChipFilters'
import { defaultAuthorChipFilters } from '@/utils/authorChipFilters'

describe('booksChips defaults', () => {
  afterEach(() => {
    useUiStore.getState().clearBooksChips()
    useUiStore.getState().clearBooksLabels()
  })

  it('turns Most Recent Day on so the books list uses the faster backend', () => {
    expect(useUiStore.getState().booksChips.mostRecent).toBe(true)
    expect(defaultBookChipFilters.mostRecent).toBe(false)
  })

  it('turns Most Recent Day off when another chip is turned on', () => {
    useUiStore.getState().toggleBooksChip('withoutLoc')
    expect(useUiStore.getState().booksChips.withoutLoc).toBe(true)
    expect(useUiStore.getState().booksChips.mostRecent).toBe(false)
  })

  it('restores Most Recent Day when the last other chip is turned off', () => {
    useUiStore.getState().toggleBooksChip('withoutLoc')
    useUiStore.getState().toggleBooksChip('withoutLoc')
    expect(useUiStore.getState().booksChips.withoutLoc).toBe(false)
    expect(useUiStore.getState().booksChips.mostRecent).toBe(true)
  })

  it('keeps Most Recent Day off while any other chip remains on', () => {
    useUiStore.getState().toggleBooksChip('withoutLoc')
    useUiStore.getState().toggleBooksChip('electronic')
    useUiStore.getState().toggleBooksChip('withoutLoc')
    expect(useUiStore.getState().booksChips.electronic).toBe(true)
    expect(useUiStore.getState().booksChips.mostRecent).toBe(false)
  })

  it('turns Most Recent Day off when a genre label is selected', () => {
    useUiStore.getState().toggleBooksLabel('fiction')
    expect(useUiStore.getState().booksLabelFilter).toEqual(['fiction'])
    expect(useUiStore.getState().booksChips.mostRecent).toBe(false)
  })

  it('does not turn Most Recent Day on while other filters are active', () => {
    useUiStore.getState().toggleBooksChip('withoutLoc')
    useUiStore.getState().toggleBooksChip('mostRecent')
    expect(useUiStore.getState().booksChips.mostRecent).toBe(false)
  })

  it('restores Most Recent Day when the last genre label is cleared', () => {
    useUiStore.getState().toggleBooksLabel('fiction')
    useUiStore.getState().clearBooksLabels()
    expect(useUiStore.getState().booksLabelFilter).toEqual([])
    expect(useUiStore.getState().booksChips.mostRecent).toBe(true)
  })
})

describe('authorsChips defaults', () => {
  afterEach(() => {
    useUiStore.getState().clearAuthorsChips()
  })

  it('turns Most Recent Day on so the authors list uses the faster backend', () => {
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(true)
    expect(defaultAuthorChipFilters.mostRecent).toBe(false)
  })

  it('turns Most Recent Day off when another chip is turned on', () => {
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    expect(useUiStore.getState().authorsChips.withoutDescription).toBe(true)
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(false)
  })

  it('restores Most Recent Day when the last other chip is turned off', () => {
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    expect(useUiStore.getState().authorsChips.withoutDescription).toBe(false)
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(true)
  })

  it('keeps Most Recent Day off while any other chip remains on', () => {
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    useUiStore.getState().toggleAuthorsChip('zeroBooks')
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    expect(useUiStore.getState().authorsChips.zeroBooks).toBe(true)
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(false)
  })

  it('does not turn Most Recent Day on while other filters are active', () => {
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    useUiStore.getState().toggleAuthorsChip('mostRecent')
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(false)
  })
})
