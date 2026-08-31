// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FilterChip } from '../FilterChip'

describe('FilterChip', () => {
  it('toggles the filter from the label, not from the info button', () => {
    const onClick = vi.fn()
    render(
      <FilterChip
        label="In-library materials"
        active={false}
        onClick={onClick}
        tooltip="Only books with a Library of Congress call number"
        dataTest="filter-in-library"
      />,
    )

    fireEvent.click(screen.getByTestId('filter-in-library'))
    expect(onClick).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByTestId('filter-in-library-info'))
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('shows the filter explanation when the info button is clicked', () => {
    render(
      <FilterChip
        label="Without LOC"
        active={false}
        onClick={() => {}}
        tooltip="Only books without a Library of Congress call number"
        dataTest="filter-without-loc"
      />,
    )

    expect(screen.queryByTestId('filter-without-loc-tooltip')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('filter-without-loc-info'))

    const tooltip = screen.getByTestId('filter-without-loc-tooltip')
    expect(tooltip).toBeInTheDocument()
    expect(tooltip).toHaveTextContent('Only books without a Library of Congress call number')
    expect(screen.getByTestId('filter-without-loc-info')).toHaveAttribute('aria-expanded', 'true')
  })

  it('shows the filter explanation on info hover', () => {
    render(
      <FilterChip
        label="Has email"
        active={false}
        onClick={() => {}}
        tooltip="Only applications that include an email address"
        dataTest="filter-has-email"
      />,
    )

    fireEvent.mouseEnter(screen.getByTestId('filter-has-email-info').parentElement as HTMLElement)
    expect(screen.getByTestId('filter-has-email-tooltip')).toHaveTextContent(
      'Only applications that include an email address',
    )
  })
})
