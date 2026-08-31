// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { queryKeys } from '@/config/queryClient'
import type { BookDto } from '@/types/dtos'
import { useTitleAuthorFromPhoto } from '../books'
import { api } from '../client'

vi.mock('../client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

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

    vi.mocked(api.put).mockResolvedValue({
      ...originalBook,
      title: 'Extracted Title',
      authorId: 2,
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
    expect(api.put).toHaveBeenCalledWith('/books/1/title-author-from-photo')
  })
})
