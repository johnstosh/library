// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { DesireToPurchaseFilters } from '../DesireToPurchaseFilters'

describe('DesireToPurchaseFilters', () => {
  it('starts the section with Desire to Purchase and lists 0–10 plus Unset', () => {
    render(
      <DesireToPurchaseFilters selected={[]} onToggle={() => {}} onClear={() => {}} />,
    )

    const section = screen.getByTestId('desire-to-purchase-filters')
    expect(section).toHaveTextContent('Desire to Purchase')
    expect(screen.getByTestId('desire-to-purchase-filter-0')).toHaveTextContent('0 — Already own enough')
    expect(screen.getByTestId('desire-to-purchase-filter-10')).toHaveTextContent('10 — First priority')
    expect(screen.getByTestId('desire-to-purchase-filter-unset')).toHaveTextContent('Unset')
  })

  it('marks selected chips and shows a clear button', () => {
    render(
      <DesireToPurchaseFilters
        selected={[7, 'unset']}
        onToggle={() => {}}
        onClear={() => {}}
      />,
    )

    expect(screen.getByTestId('desire-to-purchase-filter-7')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByTestId('desire-to-purchase-filter-0')).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByTestId('desire-to-purchase-filter-clear')).toBeInTheDocument()
  })
})
