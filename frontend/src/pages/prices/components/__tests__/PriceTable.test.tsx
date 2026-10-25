// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { BookPriceDto } from '@/types/dtos'
import { formatDateTime } from '@/utils/formatters'
import { PriceTable } from '../PriceTable'

const lookedUpAt = '2026-09-10T12:00:00'

const price: BookPriceDto = {
  id: 11,
  bookId: 1,
  bookTitle: 'Pride and Prejudice',
  author: 'Jane Austen',
  sellerId: 7,
  sellerName: 'ThriftBooks',
  cover: 'LIBRARY_BINDING',
  priceDollars: 4.86,
  shippingDollars: 0,
  totalDollars: 4.86,
  condition: 'Used - Good',
  lookedUpAt,
  detailsUrl: 'https://www.abebooks.com/pride/bd',
}

function renderTable(prices: BookPriceDto[] = [price]) {
  return render(
    <MemoryRouter>
      <PriceTable prices={prices} isLoading={false} />
    </MemoryRouter>,
  )
}

describe('PriceTable column widths', () => {
  it('sets Book to half its previous share and Seller to 60% of Book', () => {
    renderTable()

    const headers = screen.getAllByRole('columnheader')
    const header = (name: string) => headers.find((cell) => cell.textContent === name)

    expect(header('Book')).toHaveStyle({ width: '32.75%' })
    expect(header('Seller')).toHaveStyle({ width: '19.65%' })
    expect(header('Cover')).toHaveStyle({ width: '8.8%' })
    expect(header('Condition')).toHaveStyle({ width: '6.2%' })
    expect(header('Price')).toHaveStyle({ width: '11.3%' })
    expect(header('Looked up')).toHaveStyle({ width: '6.9%' })
    expect(header('Status')).toHaveStyle({ width: '8.9%' })
    expect(header('Listing')).toHaveStyle({ width: '5.5%' })
    expect(header('Shipping')).toBeUndefined()
    expect(header('Total')).toBeUndefined()

    expect(header('Price')).toHaveAttribute('title', 'Item price, shipping, and total')
    expect(screen.getByTestId('price-cover-11')).toHaveTextContent('LB')
    expect(screen.getByTestId('price-cover-11')).toHaveAttribute('title', 'Library Binding')
    expect(screen.getByTestId('price-total-11')).toHaveTextContent('$4.86')
    expect(screen.getByText('+ $0.00')).toBeInTheDocument()
    expect(screen.getByText('9/10/26')).toHaveAttribute('title', formatDateTime(lookedUpAt))
  })
})
