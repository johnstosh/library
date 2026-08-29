// (c) Copyright 2025 by Muczynski

export type StatusTone =
  | 'success'
  | 'info'
  | 'danger'
  | 'warning'
  | 'neutral'
  | 'accent'
  | 'emphasis'

export const STATUS_TONE_CLASSES: Record<StatusTone, string> = {
  success: 'bg-green-100 text-green-800',
  info: 'bg-blue-100 text-blue-800',
  danger: 'bg-red-100 text-red-800',
  warning: 'bg-amber-100 text-amber-800',
  neutral: 'bg-gray-100 text-gray-800',
  accent: 'bg-indigo-100 text-indigo-800',
  emphasis: 'bg-purple-100 text-purple-800',
}

export function bookStatusTone(status: string | null | undefined): StatusTone {
  switch (status) {
    case 'ACTIVE':
    case 'AVAILABLE':
      return 'success'
    case 'ON_ORDER':
    case 'CHECKED_OUT':
      return 'info'
    case 'LOST':
    case 'WITHDRAWN':
      return 'danger'
    case 'DAMAGED':
      return 'warning'
    default:
      return 'neutral'
  }
}

export function loanStatusTone(returned: boolean, overdue: boolean): StatusTone {
  if (returned) return 'success'
  if (overdue) return 'danger'
  return 'info'
}

export function photoStatusTone(status: string | null | undefined): StatusTone {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'IN_PROGRESS':
      return 'info'
    case 'NO_IMAGE':
      return 'neutral'
    case 'PENDING_IMPORT':
      return 'emphasis'
    case 'PENDING':
    default:
      return 'warning'
  }
}

export function applicationStatusTone(status: string | null | undefined): StatusTone {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'QUESTION':
      return 'info'
    case 'NOT_APPROVED':
    case 'REJECTED':
      return 'danger'
    case 'PENDING':
    default:
      return 'warning'
  }
}

export function applicationStatusLabel(status: string | null | undefined): string {
  switch (status) {
    case 'APPROVED':
      return 'Approved'
    case 'QUESTION':
      return 'Question'
    case 'NOT_APPROVED':
    case 'REJECTED':
      return 'Not approved'
    case 'PENDING':
    default:
      return 'Pending'
  }
}

export function jobStatusTone(status: string | null | undefined): StatusTone {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILURE':
    case 'FAILED':
      return 'danger'
    default:
      return 'neutral'
  }
}
