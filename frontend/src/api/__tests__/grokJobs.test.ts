// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api } from '../client'
import { runGrokJob } from '../grokJobs'

vi.mock('../client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../client')>()),
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

afterEach(() => {
  vi.clearAllMocks()
})

const fast = { pollIntervalMs: 0 }
const OUT_OF_CREDITS =
  'Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.'

describe('runGrokJob', () => {
  it('starts the job, polls while RUNNING, and resolves with the result', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })
    vi.mocked(api.get)
      .mockResolvedValueOnce({ jobId: 'abc', status: 'RUNNING' })
      .mockResolvedValueOnce({ jobId: 'abc', status: 'RUNNING' })
      .mockResolvedValueOnce({ jobId: 'abc', status: 'SUCCEEDED', result: { id: 7, title: 'Done' } })

    const result = await runGrokJob<{ id: number; title: string }>(
      '/books/7/book-from-title-author/start',
      { title: 'T', authorName: 'A' },
      fast
    )

    expect(result).toEqual({ id: 7, title: 'Done' })
    expect(api.post).toHaveBeenCalledWith('/books/7/book-from-title-author/start', { title: 'T', authorName: 'A' })
    expect(api.get).toHaveBeenCalledTimes(3)
    expect(api.get).toHaveBeenCalledWith('/grok-jobs/abc')
  })

  it('throws an ApiError with the server message and status when the job fails (402 out of credits)', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })
    vi.mocked(api.get).mockResolvedValue({ jobId: 'abc', status: 'FAILED', httpStatus: 402, error: OUT_OF_CREDITS })

    const error = await runGrokJob('/books/1/book-by-photo/start', undefined, fast).catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.message).toBe(OUT_OF_CREDITS)
    expect(error.status).toBe(402)
  })

  it('resolves without polling when the start response is already finished', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'SUCCEEDED', result: 'ok' })

    await expect(runGrokJob('/x/start', undefined, fast)).resolves.toBe('ok')
    expect(api.get).not.toHaveBeenCalled()
  })

  it('rides out a transient poll failure', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })
    vi.mocked(api.get)
      .mockRejectedValueOnce(new ApiError('Failed to fetch', 0, 'Network Error'))
      .mockRejectedValueOnce(new ApiError('Bad gateway', 502, 'Bad Gateway'))
      .mockResolvedValueOnce({ jobId: 'abc', status: 'SUCCEEDED', result: 42 })

    await expect(runGrokJob('/x/start', undefined, fast)).resolves.toBe(42)
  })

  it('gives up after too many consecutive transient poll failures', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })
    vi.mocked(api.get).mockRejectedValue(new ApiError('Failed to fetch', 0, 'Network Error'))

    await expect(runGrokJob('/x/start', undefined, { ...fast, maxPollErrors: 2 })).rejects.toThrow('Failed to fetch')
    expect(api.get).toHaveBeenCalledTimes(3)
  })

  it('surfaces a non-transient poll error such as an expired job (404)', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })
    vi.mocked(api.get).mockRejectedValue(new ApiError('This Grok request is no longer available', 404, 'Not Found'))

    await expect(runGrokJob('/x/start', undefined, fast)).rejects.toThrow('This Grok request is no longer available')
    expect(api.get).toHaveBeenCalledTimes(1)
  })

  it('times out with a plain message when the job never finishes', async () => {
    vi.mocked(api.post).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })
    vi.mocked(api.get).mockResolvedValue({ jobId: 'abc', status: 'RUNNING' })

    const error = await runGrokJob('/x/start', undefined, { pollIntervalMs: 5, timeoutMs: 30 }).catch((e) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(504)
  })
})
