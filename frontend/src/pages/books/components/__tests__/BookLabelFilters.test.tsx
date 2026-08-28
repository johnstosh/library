// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { BookLabelFilters } from '../BookLabelFilters'

describe('BookLabelFilters', () => {
  it('lays genres out alphabetically in columns of three, top to bottom then left to right', () => {
    render(
      <BookLabelFilters
        selectedLabels={[]}
        onToggleLabel={() => {}}
        onClearLabels={() => {}}
      />,
    )

    const columns = screen.getAllByTestId('label-filter-column')
    const labelsByColumn = columns.map((column) =>
      Array.from(column.querySelectorAll('button')).map((button) => button.textContent),
    )

    expect(labelsByColumn[0]).toEqual(['adult', 'adventure', 'biography'])
    expect(labelsByColumn[1]).toEqual(['childrens', 'classic', 'discernment'])
    expect(labelsByColumn.every((column) => column.length <= 3)).toBe(true)

    const readingOrder = labelsByColumn.flat()
    const sorted = [...readingOrder].sort((a, b) => a!.localeCompare(b!))
    expect(readingOrder).toEqual(sorted)
  })
})
