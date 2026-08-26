// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyLoanChipFilters,
  defaultLoanChipFilters,
  type LoanChipFilters,
} from '../loanChipFilters'
import type { LoanDto } from '@/types/dtos'

function loan(overrides: Partial<LoanDto> & { id: number }): LoanDto {
  return {
    bookId: 1,
    userId: 1,
    loanDate: '2026-08-01',
    dueDate: '2026-08-15',
    lastModified: '2026-08-01T00:00:00',
    ...overrides,
  }
}

const chips = (overrides: Partial<LoanChipFilters> = {}): LoanChipFilters => ({
  ...defaultLoanChipFilters,
  active: false,
  ...overrides,
})

const now = new Date(2026, 7, 20)

const sample: LoanDto[] = [
  loan({ id: 1, bookTitle: 'Active', dueDate: '2026-08-25' }),
  loan({ id: 2, bookTitle: 'Overdue', dueDate: '2026-08-01' }),
  loan({ id: 3, bookTitle: 'Returned', returnDate: '2026-08-10', photoId: 9 }),
  loan({ id: 4, bookTitle: 'Recent', loanDate: '2026-08-18', dueDate: '2026-08-25' }),
]

describe('applyLoanChipFilters', () => {
  it('defaults hide returned loans', () => {
    expect(applyLoanChipFilters(sample, defaultLoanChipFilters, now).map((l) => l.id)).toEqual([1, 2, 4])
  })

  it('returned chip keeps only returned loans', () => {
    expect(applyLoanChipFilters(sample, chips({ returned: true }), now).map((l) => l.id)).toEqual([3])
  })

  it('overdue chip keeps outstanding past-due loans', () => {
    expect(applyLoanChipFilters(sample, chips({ overdue: true }), now).map((l) => l.id)).toEqual([2])
  })

  it('has-photo chip keeps loans with a checkout photo', () => {
    expect(applyLoanChipFilters(sample, chips({ hasPhoto: true }), now).map((l) => l.id)).toEqual([3])
  })

  it('most-recent chip keeps the latest checkout day', () => {
    expect(applyLoanChipFilters(sample, chips({ mostRecent: true }), now).map((l) => l.id)).toEqual([4])
  })
})
