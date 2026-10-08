// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import { BulkActionsToolbar } from '../BulkActionsToolbar'

const branchState = vi.hoisted(() => ({
  current: [{ id: 1, branchName: 'St. Martin de Porres', librarySystemName: 'Sacred Heart Library System' }] as
    | { id: number; branchName: string; librarySystemName: string }[]
    | [],
}))

vi.mock('@/api/branches', () => ({
  useBranches: () => ({ data: branchState.current }),
}))

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn() }),
}))

vi.mock('@/api/books', () => ({
  useDeleteBooks: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useBulkBookFromImage: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useBulkBookFromTitleAuthor: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useLookupBulkGenresWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useLookupBulkReadingDifficultyWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useNormalizeTitlesBulk: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useNormalizeAuthorsBulk: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/loc-lookup', () => ({
  useLookupBulkBooksWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/grokipedia-lookup', () => ({
  useLookupBulkBooksGrokipediaWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/free-text-lookup', () => ({
  useLookupBulkFreeTextWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/ydl-lookup', () => ({
  useLookupBulkYdlWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/emu-lookup', () => ({
  useLookupBulkEmuWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/acla-lookup', () => ({
  useLookupBulkAclaWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/prices', () => ({
  useLookupBulkPricesWithProgress: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

function renderToolbar(props?: Partial<{
  tableCount: number
  searchResultsCount: number | undefined
  totalCount: number
}>) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <BulkActionsToolbar
        selectedIds={new Set()}
        onClearSelection={() => undefined}
        tableCount={props?.tableCount ?? 100}
        searchResultsCount={props && 'searchResultsCount' in props ? props.searchResultsCount : 312}
        totalCount={props?.totalCount ?? 2433}
      />
    </QueryClientProvider>,
  )
}

describe('BulkActionsToolbar stats', () => {
  it('shows the search-results count between the table count and the library-system total', () => {
    branchState.current = [
      { id: 1, branchName: 'St. Martin de Porres', librarySystemName: 'Sacred Heart Library System' },
    ]
    renderToolbar()

    expect(screen.getByTestId('table-count')).toHaveTextContent('100 books in this table')
    expect(screen.getByTestId('search-results-count')).toHaveTextContent('312 books in the search results')
    expect(screen.getByTestId('database-count')).toHaveTextContent(
      '2,433 books in the Sacred Heart Library System',
    )
  })

  it('uses the singular book noun when the search-results count is one', () => {
    renderToolbar({ tableCount: 1, searchResultsCount: 1, totalCount: 1 })

    expect(screen.getByTestId('search-results-count')).toHaveTextContent('1 book in the search results')
    expect(screen.getByTestId('database-count')).toHaveTextContent('1 book in the Sacred Heart Library System')
  })

  it('hides the search-results count when it is undefined', () => {
    renderToolbar({ searchResultsCount: undefined })

    expect(screen.queryByTestId('search-results-count')).not.toBeInTheDocument()
    expect(screen.getByTestId('database-count')).toBeInTheDocument()
  })

  it('shows the naming tools when a book is selected', () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <BulkActionsToolbar
          selectedIds={new Set([1])}
          onClearSelection={() => undefined}
          tableCount={1}
        />
      </QueryClientProvider>,
    )

    expect(screen.getByTestId('bulk-chicago-title')).toHaveTextContent('Chicago Title Case')
    expect(screen.getByTestId('bulk-canonical-author')).toHaveTextContent('Canonical Author Names')
  })

  it('falls back to Sacred Heart Library System when the first branch has no library system name', () => {
    branchState.current = []
    renderToolbar({ tableCount: 2, searchResultsCount: 2, totalCount: 9 })

    expect(screen.getByTestId('database-count')).toHaveTextContent('9 books in the Sacred Heart Library System')
    expect(screen.queryByTestId('table-branch-name')).not.toBeInTheDocument()
  })
})
