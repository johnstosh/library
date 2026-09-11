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
    expect(screen.getByTestId('filter-no-prices')).toBeInTheDocument()
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
})
