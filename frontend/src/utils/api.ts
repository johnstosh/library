// (c) Copyright 2025 by Muczynski
import { ApiError } from '@/api/client'

const TRANSIENT_STATUSES = new Set([408, 429, 502, 503, 504])

/**
 * Determines if an error is a transient API error that can be retried or refreshed.
 * Covers:
 * - ApiError with HTTP statuses 408, 429, 502, 503, 504
 * - Network failures (status 0 or TypeError/Fetch errors)
 * - Does NOT treat 400, 401, 403, 404, 409 as transient.
 */
export function isTransientApiError(error: unknown): boolean {
  if (!error) return false

  if (error instanceof ApiError) {
    return TRANSIENT_STATUSES.has(error.status)
  }

  // Network errors, failed fetch, CORS, timeout, etc.
  if (error instanceof TypeError) {
    const message = error.message.toLowerCase()
    return message.includes('fetch') || 
           message.includes('network') || 
           message.includes('failed to fetch')
  }

  if (typeof error === 'object' && error !== null) {
    const err = error as { status?: number; message?: string }
    if (typeof err.status === 'number' && TRANSIENT_STATUSES.has(err.status)) {
      return true
    }
    if (typeof err.message === 'string') {
      const msg = err.message.toLowerCase()
      return msg.includes('network') || msg.includes('failed to fetch') || msg.includes('timeout')
    }
  }

  return false
}

/**
 * Returns a user-friendly message for transient errors.
 */
export function getTransientErrorMessage(error: unknown): string {
  if (!isTransientApiError(error)) {
    return error instanceof Error ? error.message : 'An unexpected error occurred'
  }

  if (error instanceof ApiError) {
    switch (error.status) {
      case 408:
        return 'Request timed out. The server is taking too long to respond.'
      case 429:
        return 'Rate limited. Too many requests — please wait a moment.'
      case 502:
      case 503:
      case 504:
        return 'Server temporarily unavailable. Please try again shortly.'
      default:
        return 'Temporary server issue. Please refresh or try again.'
    }
  }

  return 'Network connection issue. Please check your connection and refresh.'
}
