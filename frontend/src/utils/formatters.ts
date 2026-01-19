// (c) Copyright 2025 by Muczynski
import { format, parseISO } from 'date-fns'

/**
 * Parse an ISO date string to a Date object, handling timezone issues correctly.
 *
 * CRITICAL: Date-only strings (e.g., "2025-01-19") are parsed as UTC midnight by
 * JavaScript's Date constructor, which causes off-by-1-day errors in local timezones.
 *
 * This function detects date-only strings and parses them as local dates instead.
 * For datetime strings (with "T"), it uses standard ISO parsing.
 *
 * @param dateString - ISO date string (either "YYYY-MM-DD" or full ISO datetime)
 * @returns Date object in local timezone
 */
export function parseISODateSafe(dateString: string): Date {
  if (!dateString) {
    return new Date(NaN)
  }

  // Check if this is a date-only string (no time component)
  // Date-only format: "YYYY-MM-DD" (exactly 10 chars, no "T")
  if (dateString.length === 10 && !dateString.includes('T')) {
    // Parse as local date by extracting components directly
    const [year, month, day] = dateString.split('-').map(Number)
    // Create date at local midnight (month is 0-indexed in JS)
    return new Date(year, month - 1, day)
  }

  // For datetime strings, use standard parseISO which handles timezone correctly
  return parseISO(dateString)
}

/**
 * Format an ISO date string to a readable date format
 *
 * Uses parseISODateSafe to correctly handle date-only strings without
 * timezone conversion issues.
 */
export function formatDate(dateString: string | undefined, formatStr = 'MMM d, yyyy'): string {
  if (!dateString) return ''
  try {
    const date = parseISODateSafe(dateString)
    return format(date, formatStr)
  } catch {
    return dateString
  }
}

/**
 * Format an ISO datetime string to a readable datetime format
 *
 * Uses parseISODateSafe to correctly handle both date-only and datetime strings.
 */
export function formatDateTime(
  dateString: string | undefined,
  formatStr = 'MMM d, yyyy h:mm a'
): string {
  if (!dateString) return ''
  try {
    const date = parseISODateSafe(dateString)
    return format(date, formatStr)
  } catch {
    return dateString
  }
}

/**
 * Format a date as relative time (e.g., "5 minutes ago", "2 hours ago")
 * Falls back to absolute date for dates older than a week
 *
 * Uses parseISODateSafe to correctly handle both date-only and datetime strings.
 */
export function formatRelativeTime(dateString: string | undefined): string {
  if (!dateString) return ''
  try {
    const date = parseISODateSafe(dateString)
    const now = new Date()
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000)

    if (seconds < 60) return 'Just now'
    if (seconds < 120) return '1 minute ago'
    if (seconds < 3600) return `${Math.floor(seconds / 60)} minutes ago`
    if (seconds < 7200) return '1 hour ago'
    if (seconds < 86400) return `${Math.floor(seconds / 3600)} hours ago`
    if (seconds < 172800) return '1 day ago'
    if (seconds < 604800) return `${Math.floor(seconds / 86400)} days ago`

    // For dates older than a week, show absolute date
    return formatDate(dateString)
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

/**
 * Check if a string is a valid URL
 * Returns true only for properly formatted http/https URLs
 * Rejects empty strings, single characters like "-", and malformed URLs
 */
export function isValidUrl(url: string | undefined | null): boolean {
  if (!url || url.trim().length < 5) return false
  try {
    const parsed = new URL(url)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch {
    return false
  }
}
