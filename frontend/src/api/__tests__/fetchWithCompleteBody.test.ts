// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  fetchComplete,
  IncompleteBodyError,
  readCompleteBody,
} from '../fetchWithCompleteBody'

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('readCompleteBody', () => {
  it('accepts a body matching Content-Length', async () => {
    const body = '{"ok":true}'
    const response = new Response(body, {
      headers: { 'Content-Length': String(new TextEncoder().encode(body).length) },
    })
    const buf = await readCompleteBody(response)
    expect(new TextDecoder().decode(buf)).toBe(body)
  })

  it('throws when body is shorter than Content-Length', async () => {
    const response = new Response('short', {
      headers: { 'Content-Length': '100' },
    })
    await expect(readCompleteBody(response)).rejects.toBeInstanceOf(IncompleteBodyError)
  })
})

describe('fetchComplete', () => {
  it('retries on truncated body then succeeds', async () => {
    const good = '{"section":"books","items":[]}'
    const goodLen = String(new TextEncoder().encode(good).length)
    let calls = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        calls += 1
        if (calls === 1) {
          return new Response('nope', { status: 200, headers: { 'Content-Length': goodLen } })
        }
        return new Response(good, { status: 200, headers: { 'Content-Length': goodLen } })
      }),
    )

    const response = await fetchComplete('/api/import/json/chunk', {
      credentials: 'include',
      retryDelayMs: 1,
    })
    expect(calls).toBe(2)
    expect(response.ok).toBe(true)
    expect(await response.json()).toEqual({ section: 'books', items: [] })
  })

  it('retries 503 then returns success', async () => {
    let calls = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        calls += 1
        if (calls < 3) {
          return new Response('busy', { status: 503 })
        }
        const body = '{"ok":true}'
        return new Response(body, {
          status: 200,
          headers: { 'Content-Length': String(new TextEncoder().encode(body).length) },
        })
      }),
    )

    const response = await fetchComplete('/x', { retryDelayMs: 1, retries: 3 })
    expect(calls).toBe(3)
    expect(response.ok).toBe(true)
  })

  it('does not retry 400', async () => {
    const fetchMock = vi.fn(async () => new Response('bad', { status: 400 }))
    vi.stubGlobal('fetch', fetchMock)
    const response = await fetchComplete('/x', { retryDelayMs: 1 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(response.status).toBe(400)
  })
})
