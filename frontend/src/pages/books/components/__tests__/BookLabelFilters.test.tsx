// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { BookLabelFilters, GenreChips, standardGenresFrom } from '../BookLabelFilters'

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
    expect(labels.slice(0, 3)).toEqual(['adult', 'adventure', 'biography'])
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

