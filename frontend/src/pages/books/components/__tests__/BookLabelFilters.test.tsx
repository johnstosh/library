// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import {
  BookLabelFilters,
  formatBookLabel,
  GenreChips,
  standardGenresFrom,
} from '../BookLabelFilters'

describe('formatBookLabel', () => {
  it('uses title case and special names instead of slugs', () => {
    expect(formatBookLabel('childrens')).toBe("Children's")
    expect(formatBookLabel('slice-of-life')).toBe('Slice of Life')
    expect(formatBookLabel('talking-animals')).toBe('Talking Animals')
    expect(formatBookLabel('hagiography')).toBe('Hagiography')
    expect(formatBookLabel('fiction')).toBe('Fiction')
  })
})

describe('BookLabelFilters', () => {
  it('lists genres alphabetically so phones wrap in A–Z order', () => {
    render(
      <BookLabelFilters
        selectedLabels={[]}
        onToggleLabel={() => {}}
        onClearLabels={() => {}}
      />,
    )

    const wrap = screen.getByTestId('label-filter-wrap')
    const labels = Array.from(wrap.querySelectorAll('button')).map((button) => button.textContent)
    const sorted = [...labels].sort((a, b) => a!.localeCompare(b!))
    expect(labels).toEqual(sorted)
    expect(labels.slice(0, 3)).toEqual(['Adult', 'Adventure', 'Biography'])
  })

  it('shows user-facing names on chips while keeping slug test ids', () => {
    render(
      <BookLabelFilters
        selectedLabels={[]}
        onToggleLabel={() => {}}
        onClearLabels={() => {}}
      />,
    )

    expect(screen.getByTestId('label-filter-childrens')).toHaveTextContent("Children's")
    expect(screen.getByTestId('label-filter-slice-of-life')).toHaveTextContent('Slice of Life')
    expect(screen.getByTestId('label-filter-talking-animals')).toHaveTextContent('Talking Animals')
    expect(screen.getByTestId('label-filter-hagiography')).toHaveTextContent('Hagiography')
  })
})

describe('standardGenresFrom', () => {
  it('keeps only the standard list and drops invented names', () => {
    expect(standardGenresFrom(['Fiction', 'made-up', 'theology', 'theology'])).toEqual([
      'fiction',
      'theology',
    ])
  })
})

describe('GenreChips', () => {
  it('renders only the standard genres', () => {
    render(
      <GenreChips selected={['fiction']} onToggle={() => {}} dataTest="book-tags" />,
    )
    expect(screen.getByTestId('book-tags').querySelectorAll('button')).toHaveLength(23)
    expect(screen.queryByText('made-up')).not.toBeInTheDocument()
  })
})

