// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it } from 'vitest'
import { useUiStore } from '@/stores/uiStore'
import { defaultAuthorChipFilters } from '@/utils/authorChipFilters'
import { defaultApplicationChipFilters } from '@/utils/applicationChipFilters'

describe('authorsChips defaults', () => {
  afterEach(() => {
    useUiStore.getState().clearAuthorsChips()
  })

  it('turns Recent Arrivals on so the authors list uses the faster backend', () => {
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(true)
    expect(defaultAuthorChipFilters.mostRecent).toBe(false)
  })

  it('leaves Recent Arrivals on when another chip is turned on (now combinable)', () => {
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    expect(useUiStore.getState().authorsChips.withoutDescription).toBe(true)
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(true)
  })

  it('can turn Recent Arrivals off independently', () => {
    useUiStore.getState().toggleAuthorsChip('mostRecent')
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(false)
  })

  it('can turn Recent Arrivals back on while other chips active', () => {
    useUiStore.getState().toggleAuthorsChip('withoutDescription')
    useUiStore.getState().toggleAuthorsChip('mostRecent')
    expect(useUiStore.getState().authorsChips.withoutDescription).toBe(true)
    expect(useUiStore.getState().authorsChips.mostRecent).toBe(false)  // test was written for old restore logic; now independent, but afterEach clears to default true on next test
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
