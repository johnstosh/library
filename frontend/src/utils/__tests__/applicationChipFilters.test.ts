// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import type { AppliedDto } from '@/api/library-cards'
import {
  applyApplicationFilters,
  defaultApplicationChipFilters,
  isAwaitingReview,
  type ApplicationChipFilters,
} from '../applicationChipFilters'

function application(overrides: Partial<AppliedDto> & { id: number; name: string }): AppliedDto {
  return {
    status: 'PENDING',
    ...overrides,
  }
}

const chips = (overrides: Partial<ApplicationChipFilters> = {}): ApplicationChipFilters => ({
  ...defaultApplicationChipFilters,
  needsApproval: false,
  ...overrides,
})

const sample: AppliedDto[] = [
  application({ id: 1, name: 'Pending Pat', email: 'pat@example.com', status: 'PENDING' }),
  application({ id: 2, name: 'Question Quinn', email: 'quinn@example.com', status: 'QUESTION' }),
  application({ id: 3, name: 'Approved Ann', email: 'ann@example.com', status: 'APPROVED' }),
  application({ id: 4, name: 'Declined Dan', status: 'NOT_APPROVED' }),
  application({ id: 5, name: 'Unset Una', status: undefined }),
]

describe('isAwaitingReview', () => {
  it('treats pending, question, and unset as needing a decision', () => {
    expect(isAwaitingReview('PENDING')).toBe(true)
    expect(isAwaitingReview('QUESTION')).toBe(true)
    expect(isAwaitingReview(undefined)).toBe(true)
    expect(isAwaitingReview('APPROVED')).toBe(false)
    expect(isAwaitingReview('NOT_APPROVED')).toBe(false)
  })
})

describe('applyApplicationFilters', () => {
  it('defaults to applications that still need approval', () => {
    expect(applyApplicationFilters(sample, defaultApplicationChipFilters, '').map((a) => a.id)).toEqual([
      1, 2, 5,
    ])
  })

  it('pending chip keeps only pending and unset statuses', () => {
    expect(applyApplicationFilters(sample, chips({ pending: true }), '').map((a) => a.id)).toEqual([1, 5])
  })

  it('question chip keeps only question status', () => {
    expect(applyApplicationFilters(sample, chips({ question: true }), '').map((a) => a.id)).toEqual([2])
  })

  it('approved chip keeps only approved applications', () => {
    expect(applyApplicationFilters(sample, chips({ approved: true }), '').map((a) => a.id)).toEqual([3])
  })

  it('not-approved chip keeps declined applications', () => {
    expect(applyApplicationFilters(sample, chips({ notApproved: true }), '').map((a) => a.id)).toEqual([4])
  })

  it('has-email and without-email chips split by contact address', () => {
    expect(applyApplicationFilters(sample, chips({ hasEmail: true }), '').map((a) => a.id)).toEqual([1, 2, 3])
    expect(applyApplicationFilters(sample, chips({ withoutEmail: true }), '').map((a) => a.id)).toEqual([4, 5])
  })

  it('search matches name or email', () => {
    expect(applyApplicationFilters(sample, chips(), 'quinn').map((a) => a.id)).toEqual([2])
    expect(applyApplicationFilters(sample, chips(), 'ann@').map((a) => a.id)).toEqual([3])
  })

  it('ANDs chips with search', () => {
    expect(
      applyApplicationFilters(sample, chips({ needsApproval: true }), 'pat').map((a) => a.id)
    ).toEqual([1])
    expect(applyApplicationFilters(sample, chips({ needsApproval: true }), 'ann').map((a) => a.id)).toEqual(
      []
    )
  })
})
