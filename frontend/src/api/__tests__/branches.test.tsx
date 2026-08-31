// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { useFirstBranch } from '../branches'
import { api } from '../client'

vi.mock('../client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

function renderUseFirstBranch() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return renderHook(() => useFirstBranch(), { wrapper })
}

describe('useFirstBranch', () => {
  it('uses the first branch in the table for branding names', async () => {
    vi.mocked(api.get).mockResolvedValue([
      { id: 2, branchName: 'Northside', librarySystemName: 'River Library System' },
      { id: 9, branchName: 'Southside', librarySystemName: 'Other System' },
    ])

    const { result } = renderUseFirstBranch()

    await waitFor(() => {
      expect(result.current.hasBranch).toBe(true)
    })
    expect(result.current.branchName).toBe('Northside')
    expect(result.current.librarySystemName).toBe('River Library System')
  })

  it('falls back when the table is empty', async () => {
    vi.mocked(api.get).mockResolvedValue([])

    const { result } = renderUseFirstBranch()

    await waitFor(() => {
      expect(result.current.hasBranch).toBe(false)
    })
    expect(result.current.branchName).toBe('Branch')
    expect(result.current.librarySystemName).toBe('Library System')
  })
})
