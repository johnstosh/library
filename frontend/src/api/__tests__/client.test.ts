// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { ApiError, apiClient, describeTransportFailure } from '../client'

describe('describeTransportFailure', () => {
  it('names the wait when fetch dies with no status', () => {
    const message = describeTransportFailure(
      new TypeError('Failed to fetch'),
      'POST',
      '/wcc-lookup/lookup/42',
      47_000,
    )
    expect(message).toBe(
      'No response (after 47s). The connection closed before a status code arrived.',
    )
  })

  it('says the failure was immediate when the connection dies at once', () => {
    const message = describeTransportFailure(
      new TypeError('Failed to fetch'),
      'post',
      '/wcc-lookup/lookup/7',
      80,
    )
    expect(message).toBe(
      'No response (immediately). The connection closed before a status code arrived.',
    )
  })

  it('keeps a non-browser message and appends the cause', () => {
    const error = new TypeError('socket hang up', { cause: new Error('ECONNRESET') })
    const message = describeTransportFailure(error, 'POST', '/msu-lookup/lookup/3', 12_400)
    expect(message).toContain('No response (after 12s). socket hang up: the connection closed')
    expect(message).toContain('Cause: ECONNRESET.')
  })

  it('says a cancelled request was cancelled', () => {
    const error = new DOMException('The operation was aborted.', 'AbortError')
    expect(describeTransportFailure(error, 'POST', '/wcc-lookup/lookup/1', 3_200)).toBe(
      'Request cancelled for POST /wcc-lookup/lookup/1 after 3s.',
    )
  })
})

describe('apiClient transport failures', () => {
  it('reports a fetch rejection as a no-status failure for that endpoint', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    try {
      await expect(apiClient('/wcc-lookup/lookup/42', { method: 'POST', requireAuth: false })).rejects.toSatisfy(
        (error: unknown) => {
          expect(error).toBeInstanceOf(ApiError)
          const apiError = error as ApiError
          expect(apiError.status).toBe(0)
          expect(apiError.statusText).toBe('Network Error')
          expect(apiError.message).toBe(
            'No response (immediately). The connection closed before a status code arrived.',
          )
          return true
        },
      )
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
