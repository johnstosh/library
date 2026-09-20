// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { isTransientApiError, getTransientErrorMessage } from '../api'

describe('isTransientApiError', () => {
  it('returns false for null/undefined/falsey', () => {
    expect(isTransientApiError(null)).toBe(false)
    expect(isTransientApiError(undefined)).toBe(false)
    expect(isTransientApiError(0)).toBe(false)
  })

  it('identifies transient HTTP statuses from ApiError', () => {
    const transientStatuses = [408, 429, 502, 503, 504]
    transientStatuses.forEach(status => {
      const err = new ApiError('transient', status, 'error')
      expect(isTransientApiError(err)).toBe(true)
    })
  })

  it('does not treat client errors as transient', () => {
    const clientStatuses = [400, 401, 403, 404, 409]
    clientStatuses.forEach(status => {
      const err = new ApiError('client error', status, 'error')
      expect(isTransientApiError(err)).toBe(false)
    })
  })

  it('treats TypeError with fetch/network keywords as transient', () => {
    const networkErrors = [
      new TypeError('Failed to fetch'),
      new TypeError('Network error'),
      new TypeError('fetch failed'),
    ]
    networkErrors.forEach(err => {
      expect(isTransientApiError(err)).toBe(true)
    })
  })

  it('treats plain objects with transient status as transient', () => {
    expect(isTransientApiError({ status: 503 })).toBe(true)
    expect(isTransientApiError({ status: 502, message: 'bad gateway' })).toBe(true)
  })

  it('treats network-like messages as transient', () => {
    expect(isTransientApiError({ message: 'network error occurred' })).toBe(true)
    expect(isTransientApiError({ message: 'Failed to fetch from API' })).toBe(true)
    expect(isTransientApiError({ message: 'timeout' })).toBe(true)
  })

  it('returns false for non-transient plain errors', () => {
    expect(isTransientApiError(new Error('Validation failed'))).toBe(false)
    expect(isTransientApiError({ status: 400 })).toBe(false)
  })
})

describe('getTransientErrorMessage', () => {
  it('returns specific messages for known transient statuses', () => {
    expect(getTransientErrorMessage(new ApiError('', 503, ''))).toContain('Server temporarily unavailable')
    expect(getTransientErrorMessage(new ApiError('', 429, ''))).toContain('Rate limited')
    expect(getTransientErrorMessage(new ApiError('', 408, ''))).toContain('Request timed out')
    expect(getTransientErrorMessage(new ApiError('', 502, ''))).toContain('Server temporarily unavailable')
  })

  it('returns generic network message for non-ApiError transients', () => {
    expect(getTransientErrorMessage(new TypeError('Failed to fetch'))).toContain('Network connection issue')
  })

  it('falls back to error message or default for non-transient errors', () => {
    const msg = 'Specific business error'
    expect(getTransientErrorMessage(new Error(msg))).toBe(msg)
    expect(getTransientErrorMessage('plain string')).toBe('An unexpected error occurred')
  })
})
