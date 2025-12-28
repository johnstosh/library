// (c) Copyright 2025 by Muczynski
import { format, parseISO } from 'date-fns'

/**
 * Format an ISO date string to a readable date format
 */
export function formatDate(dateString: string | undefined, formatStr = 'MMM d, yyyy'): string {
  if (!dateString) return ''
  try {
    const date = parseISO(dateString)
    return format(date, formatStr)
  } catch {
    return dateString
  }
}

/**
 * Format an ISO datetime string to a readable datetime format
 */
export function formatDateTime(
  dateString: string | undefined,
  formatStr = 'MMM d, yyyy h:mm a'
): string {
  if (!dateString) return ''
  try {
    const date = parseISO(dateString)
    return format(date, formatStr)
  } catch {
    return dateString
  }
}

/**
 * Format Library of Congress call number for book pocket label display
 *
 * Note: Function name references "spine" for historical reasons, but
 * this formatting is actually used for book pocket labels.
 */
export function formatLocForSpine(locNumber: string | undefined): string {
  if (!locNumber) return ''
  return locNumber.replace(/\s/g, '\n')
}

/**
 * Format book status for display
 */
export function formatBookStatus(status: string | null | undefined): string {
  if (!status) return '—'
  return status
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ')
}

/**
 * Format author name (Last, First)
 */
export function formatAuthorName(firstName: string, lastName: string): string {
  return `${lastName}, ${firstName}`
}

/**
 * Truncate text to a maximum length with ellipsis
 */
export function truncate(text: string | undefined, maxLength: number): string {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}
