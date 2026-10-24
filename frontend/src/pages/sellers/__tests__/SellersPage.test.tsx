// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AbeSellerDto } from '@/types/dtos'
import { SellersPage } from '../SellersPage'

const sellers: AbeSellerDto[] = Array.from({ length: 130 }, (_, index) => ({
  id: index + 1,
  vendorId: 1000 + index,
  name: `Seller ${index + 1}`,
  additionalShippingSource: 'UNKNOWN',
  listingCount: 0,
}))

vi.mock('@/api/sellers', () => ({
  useSellers: () => ({ data: sellers, isLoading: false, error: null }),
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
    expect(screen.getByTestId('database-count')).toHaveTextContent(
      '130 sellers in the Sacred Heart Library System',
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
  })
})
