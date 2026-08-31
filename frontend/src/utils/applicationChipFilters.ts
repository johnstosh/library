// (c) Copyright 2025 by Muczynski
import type { AppliedDto } from '@/api/library-cards'

/**
 * Independent boolean chip filters for the Applications page.
 * All active chips AND together with the search box — more constraints = fewer results.
 * Needs approval defaults on so the page opens as a review queue.
 */
export interface ApplicationChipFilters {
  needsApproval: boolean
  pending: boolean
  question: boolean
  approved: boolean
  notApproved: boolean
  hasEmail: boolean
  withoutEmail: boolean
}

export const defaultApplicationChipFilters: ApplicationChipFilters = {
  needsApproval: true,
  pending: false,
  question: false,
  approved: false,
  notApproved: false,
  hasEmail: false,
  withoutEmail: false,
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

export function isAwaitingReview(status: AppliedDto['status']): boolean {
  return status == null || status === 'PENDING' || status === 'QUESTION'
}

export function isPendingStatus(status: AppliedDto['status']): boolean {
  return status == null || status === 'PENDING'
}

export function isNotApprovedStatus(status: AppliedDto['status']): boolean {
  return status === 'NOT_APPROVED' || status === 'REJECTED'
}

export function isApprovedStatus(status: AppliedDto['status']): boolean {
  return status === 'APPROVED'
}

export function applyApplicationFilters(
  applications: AppliedDto[],
  chips: ApplicationChipFilters,
  query: string
): AppliedDto[] {
  const needle = query.trim().toLowerCase()

  return applications.filter((application) => {
    if (needle) {
      const haystack = [application.name, application.email, application.phone]
        .filter((value) => !isBlank(value))
        .join(' ')
        .toLowerCase()
      if (!haystack.includes(needle)) return false
    }
    if (chips.needsApproval && !isAwaitingReview(application.status)) return false
    if (chips.pending && !isPendingStatus(application.status)) return false
    if (chips.question && application.status !== 'QUESTION') return false
    if (chips.approved && !isApprovedStatus(application.status)) return false
    if (chips.notApproved && !isNotApprovedStatus(application.status)) return false
    if (chips.hasEmail && isBlank(application.email)) return false
    if (chips.withoutEmail && !isBlank(application.email)) return false
    return true
  })
}
