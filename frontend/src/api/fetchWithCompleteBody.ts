// (c) Copyright 2025 by Muczynski
/**
 * Fetch helpers that verify the response body is complete when Content-Length
 * is present, and retry the same request a few times on failure / truncation.
 * Safe for idempotent GETs and upsert-style import POSTs.
 */

export const DEFAULT_CHUNK_RETRIES = 3

export class IncompleteBodyError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'IncompleteBodyError'
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Read the full response body as ArrayBuffer and verify against Content-Length
 * when the header is present. Throws IncompleteBodyError on shortfall.
 */
export async function readCompleteBody(response: Response): Promise<ArrayBuffer> {
  const declared = response.headers.get('Content-Length')
  const buf = await response.arrayBuffer()
  if (declared != null && declared !== '') {
    const expected = Number(declared)
    if (Number.isFinite(expected) && expected >= 0 && buf.byteLength !== expected) {
      throw new IncompleteBodyError(
        `Incomplete response body: received ${buf.byteLength} byte(s), Content-Length ${expected}`,
      )
    }
  }
  return buf
}

export async function readCompleteJson<T>(response: Response): Promise<T> {
  const buf = await readCompleteBody(response)
  const text = new TextDecoder().decode(buf)
  return JSON.parse(text) as T
}

export interface FetchCompleteOptions extends RequestInit {
  /** Total attempts including the first (default 3). */
  retries?: number
  /** Base backoff in ms between attempts (default 400). */
  retryDelayMs?: number
}

function rebuildResponse(source: Response, buf: ArrayBuffer): Response {
  const headers = new Headers(source.headers)
  headers.set('Content-Length', String(buf.byteLength))
  return new Response(buf, {
    status: source.status,
    statusText: source.statusText,
    headers,
  })
}

/**
 * fetch() with Content-Length body verification and a few retries on network /
 * HTTP / truncated-body failures. Does not retry on 4xx except 408/429.
 * Successful responses are fully buffered so callers can re-read safely.
 */
export async function fetchComplete(
  input: RequestInfo | URL,
  options: FetchCompleteOptions = {},
): Promise<Response> {
  const retries = options.retries ?? DEFAULT_CHUNK_RETRIES
  const delayMs = options.retryDelayMs ?? 400
  const { retries: _r, retryDelayMs: _d, ...init } = options

  let lastError: unknown
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const response = await fetch(input, init)
      if (!response.ok) {
        const retryable =
          response.status >= 500 || response.status === 408 || response.status === 429
        if (!retryable || attempt === retries) {
          return response
        }
        lastError = new Error(`HTTP ${response.status}`)
        await sleep(delayMs * attempt)
        continue
      }
      const buf = await readCompleteBody(response)
      return rebuildResponse(response, buf)
    } catch (err) {
      lastError = err
      if (attempt === retries) break
      await sleep(delayMs * attempt)
    }
  }
  throw lastError instanceof Error
    ? lastError
    : new Error(`Request failed after ${retries} attempt(s)`)
}
