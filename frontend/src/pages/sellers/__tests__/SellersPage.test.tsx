// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AbeSellerDto } from '@/types/dtos'
import { SellersPage } from '../SellersPage'

const sellers: AbeSellerDto[] = [
  ...Array.from({ length: 130 }, (_, index) => ({
    id: index + 1,
    vendorId: 1000 + index,
    name: `Seller ${index + 1}`,
    additionalShippingDollars: 1.5,
    additionalShippingSource: 'PARSED' as const,
    listingCount: 0,
  })),
  ...Array.from({ length: 4 }, (_, index) => ({
    id: 131 + index,
    vendorId: 2000 + index,
    name: `Unpublished ${index + 1}`,
    additionalShippingDollars: null,
    additionalShippingSource: 'UNKNOWN' as const,
    listingCount: 0,
  })),
]
sellers[0].sampleStandardShipping = 4.5
sellers[1].sampleStandardShipping = 1

const reparse = vi.hoisted(() => vi.fn())

vi.mock('@/api/sellers', () => ({
  useSellers: () => ({ data: sellers, isLoading: false, error: null }),
  useReparseSellers: () => ({ mutateAsync: reparse, isPending: false }),
}))

vi.mock('@/api/branches', () => ({
  useBranches: () => ({
    data: [{ id: 1, branchName: 'St. Martin de Porres', librarySystemName: 'Sacred Heart Library System' }],
  }),
}))

describe('SellersPage', () => {
  it('shows 100 sellers at a time and the same count lines as Books', () => {
    render(
      <MemoryRouter>
        <SellersPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('table-count')).toHaveTextContent('100 sellers in this table')
    expect(screen.getByTestId('search-results-count')).toHaveTextContent('130 sellers in the search results')
    expect(screen.getByTestId('filter-not-published')).toHaveAttribute('aria-pressed', 'false')
    expect(screen.queryByText('Unpublished 1')).not.toBeInTheDocument()
    expect(screen.getByTestId('database-count')).toHaveTextContent(
      '134 sellers in the Sacred Heart Library System',
    )
    expect(screen.getByText('Showing 100 of 130 sellers')).toBeInTheDocument()
    expect(screen.getByTestId('seller-link-1')).toBeInTheDocument()
    expect(screen.queryByTestId('seller-link-101')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('sellers-load-more'))

    expect(screen.getByTestId('seller-link-101')).toBeInTheDocument()
    expect(screen.getByTestId('seller-link-130')).toBeInTheDocument()
    expect(screen.getByTestId('table-count')).toHaveTextContent('130 sellers in this table')
    expect(screen.getByText('Showing 130 sellers')).toBeInTheDocument()
    expect(screen.queryByTestId('sellers-load-more')).not.toBeInTheDocument()
    expect(screen.queryByText('Unpublished 1')).not.toBeInTheDocument()
  })

  it('includes sellers with no published rate only when Not published is on', () => {
    render(
      <MemoryRouter>
        <SellersPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('search-results-count')).toHaveTextContent('130 sellers in the search results')
    fireEvent.click(screen.getByTestId('filter-not-published'))

    expect(screen.getByTestId('filter-not-published')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByTestId('search-results-count')).toHaveTextContent('134 sellers in the search results')
    expect(screen.getByTestId('database-count')).toHaveTextContent(
      '134 sellers in the Sacred Heart Library System',
    )
    expect(screen.queryByText('Unpublished 1')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('sellers-load-more'))

    expect(screen.getByText('Unpublished 1')).toBeInTheDocument()
    expect(screen.getByTestId('seller-additional-131')).toHaveTextContent('Not published')
    expect(screen.getByTestId('table-count')).toHaveTextContent('134 sellers in this table')
    expect(screen.getByText('Showing 134 sellers')).toBeInTheDocument()
  })

  it('keeps sellers whose extra book is cheaper than the first book', () => {
    render(
      <MemoryRouter initialEntries={['/sellers?multiBook=1']}>
        <SellersPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('filter-multi-book')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('Seller 1')).toBeInTheDocument()
    expect(screen.queryByText('Seller 2')).not.toBeInTheDocument()
    expect(screen.getByTestId('search-results-count')).toHaveTextContent('1 seller in the search results')
  })

  it('re-parses stored terms and reports how many rates changed', async () => {
    reparse.mockResolvedValue({ examined: 134, updated: 12, unchanged: 120, keptLibrarian: 2 })
    render(
      <MemoryRouter>
        <SellersPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByTestId('reparse-sellers'))

    expect(await screen.findByTestId('reparse-result')).toHaveTextContent(
      'Updated 12 sellers from the stored terms. 2 rates entered here were kept.',
    )
    expect(reparse).toHaveBeenCalledOnce()
  })
})
