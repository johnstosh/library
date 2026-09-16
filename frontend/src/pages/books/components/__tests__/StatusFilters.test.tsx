// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { StatusFilters } from '../StatusFilters'

describe('StatusFilters', () => {
  it('starts the section with Status and lists Active variants plus the other statuses', () => {
    render(<StatusFilters selected={[]} onToggle={() => {}} onClear={() => {}} />)

    const section = screen.getByTestId('status-filters')
    expect(section).toHaveTextContent('Status')
    expect(screen.getByTestId('status-filter-in-library')).toHaveTextContent('In-library')
    expect(screen.getByTestId('status-filter-electronic-resource')).toHaveTextContent(
      'Electronic resource',
    )
    expect(screen.getByTestId('status-filter-without-loc')).toHaveTextContent('Without LOC')
    expect(screen.getByTestId('status-filter-lost')).toHaveTextContent('Lost')
    expect(screen.getByTestId('status-filter-withdrawn')).toHaveTextContent('Withdrawn')
    expect(screen.getByTestId('status-filter-on-order')).toHaveTextContent('On Order')
    expect(screen.getByTestId('status-filter-requested')).toHaveTextContent('Requested')
  })

  it('marks selected chips and shows a clear button', () => {
    render(
      <StatusFilters
        selected={['in-library', 'requested']}
        onToggle={() => {}}
        onClear={() => {}}
      />,
    )

    expect(screen.getByTestId('status-filter-in-library')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByTestId('status-filter-lost')).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByTestId('status-filter-clear')).toBeInTheDocument()
  })
})
