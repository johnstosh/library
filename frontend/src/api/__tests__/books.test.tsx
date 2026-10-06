// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { queryKeys } from '@/config/queryClient'
import type { BookDto } from '@/types/dtos'
import { useAuthors } from '../authors'
import { useTitleAuthorFromPhoto, useLookupBulkReadingDifficultyWithProgress, useBulkBookFromTitleAuthor, useBulkBookFromImage, useBooks } from '../books'
import { api } from '../client'
import { grokJobDefaults } from '../grokJobs'

afterEach(() => {
  vi.clearAllMocks()
})

vi.mock('../client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../client')>()),
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

// Poll background Grok jobs without waiting between polls.
grokJobDefaults.pollIntervalMs = 0

const originalBook: BookDto = {
  id: 1,
  title: 'Original Title',
  authorId: 1,
  status: 'ACTIVE',
  lastModified: '2026-01-01T00:00:00',
}

describe('useTitleAuthorFromPhoto', () => {
  it('does not write the extracted preview into the book detail cache', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    queryClient.setQueryData(queryKeys.books.detail(1), originalBook)

    vi.mocked(api.post).mockResolvedValue({
      jobId: 'job-1',
      status: 'SUCCEEDED',
      result: { ...originalBook, title: 'Extracted Title', authorId: 2 },
    })

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useTitleAuthorFromPhoto(), { wrapper })

    await result.current.mutateAsync(1)

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true)
    })

    expect(queryClient.getQueryData(queryKeys.books.detail(1))).toEqual(originalBook)
    expect(api.post).toHaveBeenCalledWith('/books/1/title-author-from-photo/start', {})
  })
})

describe('useLookupBulkReadingDifficultyWithProgress', () => {
  it('posts book IDs in batches of 10 and seeds the book cache', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    const posted: number[][] = []
    vi.mocked(api.post).mockImplementation(async (_url, body) => {
      const ids = body as number[]
      posted.push(ids)
      return ids.map((id) => ({
        bookId: id,
        success: true,
        suggestedDifficulty: 'children',
        updatedBook: { ...originalBook, id, readingDifficulty: 'children' },
      }))
    })

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useLookupBulkReadingDifficultyWithProgress(), { wrapper })

    const ids = Array.from({ length: 11 }, (_, i) => i + 1)
    await result.current.mutateAsync(ids)

    expect(posted).toEqual([
      [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
      [11],
    ])
    expect(api.post).toHaveBeenNthCalledWith(1, '/books/lookup-reading-difficulty-bulk', posted[0])
    expect(queryClient.getQueryData(queryKeys.books.detail(11))).toMatchObject({
      id: 11,
      readingDifficulty: 'children',
    })
  })
})

describe('catalog by-ids batches', () => {
  it('loads books and authors 100 ids at a time', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    const summaries = Array.from({ length: 101 }, (_, index) => ({
      id: index + 1,
      lastModified: '2026-01-01T00:00:00',
    }))
    vi.mocked(api.get).mockImplementation(async (path: string) => {
      if (path === '/books/summaries' || path === '/authors/summaries') return summaries
      return []
    })
    const posted: Array<{ path: string; ids: number[] }> = []
    vi.mocked(api.post).mockImplementation(async (path: string, body) => {
      const ids = body as number[]
      posted.push({ path, ids })
      if (path === '/authors/by-ids') {
        return ids.map((id) => ({ id, name: `Author ${id}`, lastModified: '2026-01-01T00:00:00' }))
      }
      return ids.map((id) => ({ ...originalBook, id, lastModified: '2026-01-01T00:00:00' }))
    })

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    const books = renderHook(() => useBooks(), { wrapper })
    const authors = renderHook(() => useAuthors(), { wrapper })

    await waitFor(() => expect(books.result.current.data).toHaveLength(101))
    await waitFor(() => expect(authors.result.current.data).toHaveLength(101))

    const bookPosts = posted.filter((call) => call.path === '/books/by-ids')
    const authorPosts = posted.filter((call) => call.path === '/authors/by-ids')
    expect(bookPosts.map((call) => call.ids.length)).toEqual([100, 1])
    expect(authorPosts.map((call) => call.ids.length)).toEqual([100, 1])
  })
})

describe('useBulkBookFromTitleAuthor', () => {
  it('fetches book detail then starts the background job with title and authorName, updates cache on success', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    const bookDetail = { ...originalBook, title: 'Test Book', author: 'Test Author' }
    const updatedBook = { ...bookDetail, plotSummary: 'AI generated summary from title/author' }

    vi.mocked(api.get).mockImplementation(async (url) => {
      if (url === '/books/1') return bookDetail
      if (url === '/grok-jobs/job-1') return { jobId: 'job-1', status: 'SUCCEEDED', result: updatedBook }
      throw new Error(`unexpected GET ${url}`)
    })
    vi.mocked(api.post).mockResolvedValue({ jobId: 'job-1', status: 'RUNNING' })

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useBulkBookFromTitleAuthor(), { wrapper })

    await result.current.mutateAsync([1])

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true)
    })

    expect(api.get).toHaveBeenCalledWith('/books/1')
    expect(api.post).toHaveBeenCalledWith('/books/1/book-from-title-author/start', {
      title: 'Test Book',
      authorName: 'Test Author',
    })
    expect(api.get).toHaveBeenCalledWith('/grok-jobs/job-1')
    expect(api.put).not.toHaveBeenCalled()
    expect(queryClient.getQueryData(queryKeys.books.detail(1))).toMatchObject(updatedBook)
  })
})

describe('useBulkBookFromImage', () => {
  it('marks rows failed with the server message and leaves the cached book unchanged', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    queryClient.setQueryData(queryKeys.books.detail(1), originalBook)
    queryClient.setQueryData(queryKeys.books.detail(2), { ...originalBook, id: 2 })

    const noPhotos = 'This book has no photos, so Book from Image has nothing to read.'
    const outOfCredits =
      'Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.'
    // Book 1: job runs, then fails with 400 (no photos). Book 2: job fails at once with 402.
    vi.mocked(api.post).mockImplementation(async (url) => {
      if (url === '/books/1/book-by-photo/start') return { jobId: 'job-1', status: 'RUNNING' }
      return { jobId: 'job-2', status: 'FAILED', httpStatus: 402, error: outOfCredits }
    })
    vi.mocked(api.get).mockResolvedValue({ jobId: 'job-1', status: 'FAILED', httpStatus: 400, error: noPhotos })

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useBulkBookFromImage(), { wrapper })

    const results = await result.current.mutateAsync([1, 2])

    expect(results).toEqual([
      { id: 1, success: false, error: noPhotos },
      { id: 2, success: false, error: outOfCredits },
    ])
    expect(queryClient.getQueryData(queryKeys.books.detail(1))).toEqual(originalBook)
    expect(queryClient.getQueryData(queryKeys.books.detail(2))).toEqual({ ...originalBook, id: 2 })
  })
})
