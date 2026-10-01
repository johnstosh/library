// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { defaultAuthorChipFilters } from '@/utils/authorChipFilters'
import { defaultBookChipFilters } from '@/utils/bookChipFilters'
import { useFilteredAuthors } from '../authors'
import { useBooks } from '../books'
import { api } from '../client'

afterEach(() => {
  vi.clearAllMocks()
})

vi.mock('../client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

function wrapperFor() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return wrapper
}

describe('catalog windows', () => {
  it('loads the first 100 matching books, then the next page', async () => {
    const summaries = Array.from({ length: 250 }, (_, index) => ({
      id: index + 1,
      lastModified: '2026-01-01T00:00:00',
      dateAddedToLibrary: new Date(Date.UTC(2020, 0, 1) + index * 86_400_000).toISOString(),
    }))
    vi.mocked(api.get).mockResolvedValue(summaries)
    const posted: number[][] = []
    vi.mocked(api.post).mockImplementation(async (_path: string, body) => {
      const ids = body as number[]
      posted.push(ids)
      return ids.map((id) => ({
        id,
        title: `Book ${id}`,
        status: 'ACTIVE',
        lastModified: '2026-01-01T00:00:00',
        dateAddedToLibrary: summaries[id - 1].dateAddedToLibrary,
      }))
    })

    const { result } = renderHook(
      () => useBooks({ chips: defaultBookChipFilters }, { pageSize: 100 }),
      { wrapper: wrapperFor() },
    )

    await waitFor(() => expect(result.current.data).toHaveLength(100))
    expect(result.current.total).toBe(250)
    expect(result.current.hasMore).toBe(true)
    expect(posted).toHaveLength(1)
    expect(posted[0]).toHaveLength(100)
    expect(posted[0][0]).toBe(250)
    expect(posted[0][99]).toBe(151)
    expect(result.current.data[0].id).toBe(250)

    act(() => result.current.loadMore())
    await waitFor(() => expect(posted).toHaveLength(2))
    expect(posted[1]).toHaveLength(100)
    expect(posted[1][0]).toBe(150)
    await waitFor(() => expect(result.current.data).toHaveLength(200))

    act(() => result.current.loadMore())
    await waitFor(() => expect(posted).toHaveLength(3))
    expect(posted[2]).toHaveLength(50)
    await waitFor(() => expect(result.current.hasMore).toBe(false))
    expect(result.current.data).toHaveLength(250)
  })

  it('loads the first 100 matching authors, then the next page', async () => {
    const summaries = Array.from({ length: 130 }, (_, index) => ({
      id: index + 1,
      name: `Person ${String(index + 1).padStart(3, '0')}`,
      lastModified: '2026-01-01T00:00:00',
    }))
    vi.mocked(api.get).mockResolvedValue(summaries)
    const posted: number[][] = []
    vi.mocked(api.post).mockImplementation(async (_path: string, body) => {
      const ids = body as number[]
      posted.push(ids)
      return ids.map((id) => ({
        id,
        name: summaries[id - 1].name,
        lastModified: '2026-01-01T00:00:00',
      }))
    })

    const { result } = renderHook(
      () => useFilteredAuthors({ chips: defaultAuthorChipFilters }, { pageSize: 100 }),
      { wrapper: wrapperFor() },
    )

    await waitFor(() => expect(result.current.data).toHaveLength(100))
    expect(result.current.total).toBe(130)
    expect(posted).toHaveLength(1)
    expect(posted[0][0]).toBe(1)
    expect(posted[0][99]).toBe(100)

    act(() => result.current.loadMore())
    await waitFor(() => expect(posted).toHaveLength(2))
    expect(posted[1]).toHaveLength(30)
    await waitFor(() => expect(result.current.data).toHaveLength(130))
    expect(result.current.hasMore).toBe(false)
  })
})
