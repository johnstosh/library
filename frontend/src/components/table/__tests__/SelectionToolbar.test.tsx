// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { Button } from '@/components/ui/Button'
import { ActionCarousel, SelectionSummary, TableCountPlaceholder } from '../SelectionToolbar'

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
    expect(screen.queryByTestId('table-branch-name')).not.toBeInTheDocument()
  })

  it('shows the first-branch heading above the counts, matching the nav bar', () => {
    render(
      <TableCountPlaceholder
        tableCount={12}
        totalCount={1847}
        singular="book"
        plural="books"
        branchName="St. Martin de Porres"
        librarySystemName="Sacred Heart Library System"
      />
    )

    const heading = screen.getByTestId('table-branch-name')
    expect(heading).toHaveTextContent('The St. Martin de Porres Branch')
    expect(heading).toHaveTextContent('of the Sacred Heart Library System')
    expect(screen.getByTestId('table-count')).toHaveTextContent('12 books in this table')
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

describe('ActionCarousel', () => {
  it('lets action button labels wrap instead of staying on one horizontal line', () => {
    render(
      <ActionCarousel>
        <Button size="sm">Find links to free online text</Button>
      </ActionCarousel>
    )

    const carousel = screen.getByTestId('action-carousel')
    expect(carousel.className).toMatch(/overflow-x-auto/)
    expect(carousel.className).toMatch(/whitespace-normal/)
    expect(carousel.className).toMatch(/max-w-\[8\.5rem\]/)
    expect(carousel.className).toMatch(/h-auto/)

    const button = screen.getByRole('button', { name: 'Find links to free online text' })
    expect(button.className).not.toMatch(/whitespace-nowrap/)
  })
})

