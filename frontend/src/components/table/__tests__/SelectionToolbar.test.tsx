// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { Button } from '@/components/ui/Button'
import { ActionCarousel, SelectionSummary, TableCountPlaceholder } from '../SelectionToolbar'

describe('TableCountPlaceholder', () => {
  it('reports table and library-system counts', () => {
    render(
      <TableCountPlaceholder
        tableCount={12}
        totalCount={1847}
        singular="book"
        plural="books"
      />
    )

    expect(screen.getByTestId('table-count')).toHaveTextContent('12 books in this table')
    expect(screen.getByTestId('database-count')).toHaveTextContent('1,847 books in the Sacred Heart Library System')
    expect(screen.queryByTestId('search-results-count')).not.toBeInTheDocument()
    expect(screen.queryByTestId('table-branch-name')).not.toBeInTheDocument()
  })

  it('shows the search-results count between the table count and the library-system total', () => {
    render(
      <TableCountPlaceholder
        tableCount={100}
        searchResultsCount={312}
        totalCount={2433}
        singular="book"
        plural="books"
      />
    )

    expect(screen.getByTestId('table-count')).toHaveTextContent('100 books in this table')
    expect(screen.getByTestId('search-results-count')).toHaveTextContent('312 books in the search results')
    expect(screen.getByTestId('database-count')).toHaveTextContent('2,433 books in the Sacred Heart Library System')

    const table = screen.getByTestId('table-count')
    const search = screen.getByTestId('search-results-count')
    const database = screen.getByTestId('database-count')
    expect(table.compareDocumentPosition(search) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(search.compareDocumentPosition(database) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('uses the singular noun for a search-results count of one', () => {
    render(
      <TableCountPlaceholder
        tableCount={1}
        searchResultsCount={1}
        totalCount={12}
        singular="book"
        plural="books"
      />
    )

    expect(screen.getByTestId('search-results-count')).toHaveTextContent('1 book in the search results')
  })

  it('hides the search-results count when it is undefined', () => {
    render(
      <TableCountPlaceholder
        tableCount={4}
        totalCount={12}
        singular="book"
        plural="books"
      />
    )

    expect(screen.queryByTestId('search-results-count')).not.toBeInTheDocument()
  })

  it('shows the app name above the counts, matching the nav bar', () => {
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

    expect(screen.getByTestId('app-name')).toHaveTextContent('St. Martin de Porres')
    expect(screen.getByTestId('app-sub-name')).toHaveTextContent('Sacred Heart Library System')
    expect(screen.getByTestId('table-count')).toHaveTextContent('12 books in this table')
    expect(screen.getByTestId('database-count')).toHaveTextContent('1,847 books in the Sacred Heart Library System')
  })

  it('falls back to Sacred Heart Library System when the library system name is blank', () => {
    const { rerender } = render(
      <TableCountPlaceholder
        tableCount={2}
        totalCount={9}
        singular="book"
        plural="books"
        librarySystemName="   "
      />
    )

    expect(screen.getByTestId('database-count')).toHaveTextContent('9 books in the Sacred Heart Library System')

    rerender(
      <TableCountPlaceholder
        tableCount={2}
        totalCount={9}
        singular="book"
        plural="books"
      />
    )

    expect(screen.getByTestId('database-count')).toHaveTextContent('9 books in the Sacred Heart Library System')
  })

  it('uses the passed library system name in the total count', () => {
    render(
      <TableCountPlaceholder
        tableCount={2}
        totalCount={9}
        singular="book"
        plural="books"
        librarySystemName="River Library System"
      />
    )

    expect(screen.getByTestId('database-count')).toHaveTextContent('9 books in the River Library System')
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
    expect(screen.getByTestId('database-count')).toHaveTextContent('1 author in the Sacred Heart Library System')
  })

  it('appends extra table counts such as price rows', () => {
    render(
      <TableCountPlaceholder
        tableCount={3}
        totalCount={1847}
        singular="book"
        plural="books"
        extraTableCounts={[
          { count: 4, singular: 'price', plural: 'prices', dataTest: 'price-row-count' },
        ]}
      />
    )

    expect(screen.getByTestId('table-count')).toHaveTextContent('3 books in this table')
    expect(screen.getByTestId('database-count')).toHaveTextContent('1,847 books in the Sacred Heart Library System')
    expect(screen.getByTestId('price-row-count')).toHaveTextContent('4 prices in this table')
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

