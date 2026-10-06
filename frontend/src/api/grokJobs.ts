// (c) Copyright 2025 by Muczynski
import { api, ApiError } from './client'

/**
 * Background Grok jobs.
 *
 * A Grok call can take several minutes (grok-4.7 ~200s). Holding one fetch open that long
 * fails in the browser ("Failed to fetch") even when the server succeeds. Instead the
 * `.../start` endpoint returns 202 with a job id at once and we poll
 * `GET /api/grok-jobs/{jobId}` every few seconds until it is done.
 *
 * The resolved value is exactly what the old synchronous endpoint returned. A failed job
 * throws an ApiError with the same message and HTTP status the synchronous endpoint used
 * (e.g. 402 "Grok is out of credits...", 400 "This book has no photos...").
 */

export type GrokJobStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED'

export interface GrokJobDto<T = unknown> {
  jobId: string
  kind?: string
  status: GrokJobStatus
  result?: T
  error?: string
  httpStatus?: number
}

export interface GrokJobOptions {
  /** Delay between polls (ms). */
  pollIntervalMs?: number
  /** Give up after this long (ms). The server-side Grok timeout is 10 minutes. */
  timeoutMs?: number
  /** Consecutive transient poll failures (network blip, 502/503/504) tolerated before giving up. */
  maxPollErrors?: number
}

/** Shared defaults; tests may lower pollIntervalMs. */
export const grokJobDefaults: Required<GrokJobOptions> = {
  pollIntervalMs: 3000,
  timeoutMs: 15 * 60 * 1000,
  maxPollErrors: 5,
}

const TRANSIENT_POLL_STATUSES = new Set([0, 408, 429, 502, 503, 504])

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

function settle<T>(job: GrokJobDto<T>): { done: true; value: T } | { done: false } {
  if (job.status === 'SUCCEEDED') {
    return { done: true, value: job.result as T }
  }
  if (job.status === 'FAILED') {
    const message = job.error || 'Grok request failed'
    throw new ApiError(message, job.httpStatus ?? 500, '')
  }
  return { done: false }
}

/**
 * Start a background Grok job with POST `startEndpoint` and poll until it finishes.
 */
export async function runGrokJob<T>(
  startEndpoint: string,
  body?: unknown,
  options: GrokJobOptions = {}
): Promise<T> {
  const { pollIntervalMs, timeoutMs, maxPollErrors } = { ...grokJobDefaults, ...options }

  const started = await api.post<GrokJobDto<T>>(startEndpoint, body ?? {})
  if (!started || !started.jobId) {
    throw new ApiError('Grok request did not start', 500, '')
  }
  let outcome = settle(started)
  if (outcome.done) {
    return outcome.value
  }

  const deadline = Date.now() + timeoutMs
  let pollErrors = 0
  while (Date.now() < deadline) {
    await sleep(pollIntervalMs)
    let job: GrokJobDto<T>
    try {
      job = await api.get<GrokJobDto<T>>(`/grok-jobs/${encodeURIComponent(started.jobId)}`)
    } catch (error) {
      const status = error instanceof ApiError ? error.status : 0
      if (TRANSIENT_POLL_STATUSES.has(status) && ++pollErrors <= maxPollErrors) {
        continue
      }
      throw error
    }
    pollErrors = 0
    outcome = settle(job)
    if (outcome.done) {
      return outcome.value
    }
  }
  throw new ApiError('Grok is taking too long. Check the book again in a few minutes.', 504, '')
}
