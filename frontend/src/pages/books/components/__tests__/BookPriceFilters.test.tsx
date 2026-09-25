// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { BookPriceFilters } from '../BookPriceFilters'
import { defaultBookChipFilters } from '@/utils/bookChipFilters'

describe('BookPriceFilters', () => {
  it('renders a Pricing section with both chips and the days input', () => {
    render(
      <BookPriceFilters
        chips={{ ...defaultBookChipFilters, priceOlder: true }}
        onToggle={() => {}}
        priceOlderDays={90}
        onPriceOlderDaysChange={() => {}}
      />,
    )

    expect(screen.getByTestId('book-price-filters')).toHaveTextContent('Pricing')
    expect(screen.getByTestId('filter-with-prices')).toHaveTextContent('Books with Pricing')
    expect(screen.getByTestId('filter-no-prices')).toHaveTextContent('Books without Pricing')
    expect(screen.getByTestId('filter-lookup-errors')).toHaveTextContent('Lookup Errors')
    expect(screen.getByTestId('filter-price-older')).toBeInTheDocument()
    expect(screen.getByTestId('filter-price-older-days')).toHaveValue(90)
  })

  it('notifies when days change', () => {
    const onDays = vi.fn()
    render(
      <BookPriceFilters
        chips={{ ...defaultBookChipFilters, priceOlder: true }}
        onToggle={() => {}}
        priceOlderDays={90}
        onPriceOlderDaysChange={onDays}
      />,
    )

    fireEvent.change(screen.getByTestId('filter-price-older-days'), { target: { value: '30' } })
    expect(onDays).toHaveBeenCalledWith(30)
  })

  it('renders plain chips without FilterChip info/funnel chrome', () => {
    render(
      <BookPriceFilters
        chips={{ ...defaultBookChipFilters, withPrices: true }}
        onToggle={() => {}}
        priceOlderDays={90}
        onPriceOlderDaysChange={() => {}}
      />,
    )

    expect(screen.queryByTestId('filter-with-prices-info')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-no-prices-info')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-lookup-errors-info')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-price-older-info')).not.toBeInTheDocument()
    const withPrices = screen.getByTestId('filter-with-prices')
    expect(withPrices).toHaveClass('bg-primary-600')
    expect(withPrices).toHaveClass('border-primary-600')
    expect(withPrices.querySelector('svg')).toBeNull()
  })
})
