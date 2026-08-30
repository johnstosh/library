// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it } from 'vitest'
import { useUiStore } from '@/stores/uiStore'
import { defaultAuthorChipFilters } from '@/utils/authorChipFilters'
import { defaultApplicationChipFilters } from '@/utils/applicationChipFilters'

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

describe('applicationsChips defaults', () => {
  afterEach(() => {
    useUiStore.getState().clearApplicationsChips()
    useUiStore.getState().setApplicationsSearchQuery('')
  })

  it('turns Needs approval on so the page opens as a review queue', () => {
    expect(useUiStore.getState().applicationsChips.needsApproval).toBe(true)
    expect(defaultApplicationChipFilters.needsApproval).toBe(true)
  })

  it('toggles chips independently', () => {
    useUiStore.getState().toggleApplicationsChip('approved')
    expect(useUiStore.getState().applicationsChips.approved).toBe(true)
    expect(useUiStore.getState().applicationsChips.needsApproval).toBe(true)
  })
})
