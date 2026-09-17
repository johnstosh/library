// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { BindingFilters } from '../BindingFilters'

describe('BindingFilters', () => {
  it('starts the section with Binding and lists enum chips including Unknown', () => {
    render(<BindingFilters selected={[]} onToggle={() => {}} onClear={() => {}} />)

    const section = screen.getByTestId('binding-filters')
    expect(section).toHaveTextContent('Binding')
    expect(screen.getByTestId('binding-filter-hardcover')).toHaveTextContent('Hardcover')
    expect(screen.getByTestId('binding-filter-softcover')).toHaveTextContent('Softcover')
    expect(screen.getByTestId('binding-filter-library-binding')).toHaveTextContent('Library Binding')
    expect(screen.getByTestId('binding-filter-other')).toHaveTextContent('Other')
    expect(screen.getByTestId('binding-filter-unknown')).toHaveTextContent('Unknown')
  })

  it('marks selected chips and shows a clear button', () => {
    render(
      <BindingFilters
        selected={['HARDCOVER', 'UNKNOWN']}
        onToggle={() => {}}
        onClear={() => {}}
      />,
    )

    expect(screen.getByTestId('binding-filter-hardcover')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByTestId('binding-filter-softcover')).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByTestId('binding-filter-clear')).toBeInTheDocument()
  })
})
