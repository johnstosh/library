// (c) Copyright 2025 by Muczynski
import type { LoanDto } from '@/types/dtos'
import { parseISODateSafe } from '@/utils/formatters'

/**
 * Independent boolean chip filters for the Loans page.
 * All active chips AND together — more buttons on = fewer results.
 * Active defaults on so the page still starts with outstanding loans only.
 */
export interface LoanChipFilters {
  active: boolean
  returned: boolean
  overdue: boolean
  hasPhoto: boolean
  mostRecent: boolean
}

export const defaultLoanChipFilters: LoanChipFilters = {
  active: true,
  returned: false,
  overdue: false,
  hasPhoto: false,
  mostRecent: false,
}

export function isLoanReturned(loan: LoanDto): boolean {
  return !!loan.returnDate
}

export function isLoanOverdue(loan: LoanDto, now = new Date()): boolean {
  if (isLoanReturned(loan)) return false
  const due = parseISODateSafe(loan.dueDate)
  return !isNaN(due.getTime()) && due < now
}

function localDayKey(dateString: string): string | null {
  const date = parseISODateSafe(dateString)
  if (isNaN(date.getTime())) return null
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function applyLoanChipFilters(loans: LoanDto[], chips: LoanChipFilters, now = new Date()): LoanDto[] {
  let maxDay: string | null = null
  if (chips.mostRecent) {
    for (const loan of loans) {
      const day = localDayKey(loan.loanDate)
      if (day && (maxDay === null || day > maxDay)) maxDay = day
    }
  }

  return loans.filter((loan) => {
    if (chips.active && isLoanReturned(loan)) return false
    if (chips.returned && !isLoanReturned(loan)) return false
    if (chips.overdue && !isLoanOverdue(loan, now)) return false
    if (chips.hasPhoto && !loan.photoId) return false
    if (chips.mostRecent) {
      const day = localDayKey(loan.loanDate)
      if (!day || day !== maxDay) return false
    }
    return true
  })
}
