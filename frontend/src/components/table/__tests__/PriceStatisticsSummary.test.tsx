// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PriceStatisticsSummary } from '../PriceStatisticsSummary'
import type { BookPriceStatistics } from '@/utils/priceStatistics'

const stats: BookPriceStatistics = {
  totalCost: 49.86,
  booksOver20: 1,
  booksOver40: 1,
  booksOver80: 0,
  totalBooks: 4,
  booksWithoutPrices: 2,
}

describe('PriceStatisticsSummary', () => {
  it('reports total cost, threshold counts, total books, and books without prices', () => {
    render(<PriceStatisticsSummary stats={stats} />)

    expect(screen.getByTestId('price-statistics')).toHaveTextContent('Price statistics')
    expect(screen.getByTestId('price-stats-total-cost')).toHaveTextContent('$49.86')
    expect(screen.getByTestId('price-stats-over-20')).toHaveTextContent('1')
    expect(screen.getByTestId('price-stats-over-40')).toHaveTextContent('1')
    expect(screen.getByTestId('price-stats-over-80')).toHaveTextContent('0')
    expect(screen.getByTestId('price-stats-total-books')).toHaveTextContent('4')
    expect(screen.getByTestId('price-stats-without-prices')).toHaveTextContent('2')
  })

  it('hides while loading', () => {
    const { container } = render(<PriceStatisticsSummary stats={stats} isLoading />)
    expect(container).toBeEmptyDOMElement()
  })
})
