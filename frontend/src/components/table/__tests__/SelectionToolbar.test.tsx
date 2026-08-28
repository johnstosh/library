// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { SelectionSummary, TableCountPlaceholder } from '../SelectionToolbar'

describe('TableCountPlaceholder', () => {
  it('reports table and database counts', () => {
    render(
      <TableCountPlaceholder
        tableCount={12}
        totalCount={1847}
        singular="book"
        plural="books"
      />
    )

    expect(screen.getByTestId('table-count')).toHaveTextContent('12 books in this table')
    expect(screen.getByTestId('database-count')).toHaveTextContent('1,847 books in the database')
  })

  it('uses the singular noun for a count of one', () => {
    render(
      <TableCountPlaceholder
        tableCount={1}
        totalCount={1}
        singular="author"
        plural="authors"
      />
    )

    expect(screen.getByTestId('table-count')).toHaveTextContent('1 author in this table')
    expect(screen.getByTestId('database-count')).toHaveTextContent('1 author in the database')
  })
})

describe('SelectionSummary', () => {
  it('stacks the count text above Clear Selection', () => {
    render(
      <SelectionSummary count={3} singular="book" plural="books" onClear={() => {}} />
    )

    expect(screen.getByText('3 books selected')).toBeInTheDocument()
    expect(screen.getByTestId('clear-selection')).toHaveTextContent('Clear Selection')
  })
})

