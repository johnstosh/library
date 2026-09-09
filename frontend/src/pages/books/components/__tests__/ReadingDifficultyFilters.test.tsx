// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { ReadingDifficultyFilters } from '../ReadingDifficultyFilters'

describe('ReadingDifficultyFilters', () => {
  it('starts the section with Reading Difficulty and lists enum chips including Unset', () => {
    render(
      <ReadingDifficultyFilters selected={[]} onToggle={() => {}} onClear={() => {}} />,
    )

    const section = screen.getByTestId('reading-difficulty-filters')
    expect(section).toHaveTextContent('Reading Difficulty')
    expect(screen.getByTestId('reading-difficulty-filter-children')).toHaveTextContent('Children')
    expect(screen.getByTestId('reading-difficulty-filter-accessible')).toHaveTextContent('Accessible')
    expect(screen.getByTestId('reading-difficulty-filter-moderate')).toHaveTextContent('Moderate')
    expect(screen.getByTestId('reading-difficulty-filter-demanding')).toHaveTextContent('Demanding')
    expect(screen.getByTestId('reading-difficulty-filter-advanced')).toHaveTextContent('Advanced')
    expect(screen.getByTestId('reading-difficulty-filter-unset')).toHaveTextContent('Unset')
  })

  it('marks selected chips and shows a clear button', () => {
    render(
      <ReadingDifficultyFilters
        selected={['children', 'unset']}
        onToggle={() => {}}
        onClear={() => {}}
      />,
    )

    expect(screen.getByTestId('reading-difficulty-filter-children')).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(screen.getByTestId('reading-difficulty-filter-moderate')).toHaveAttribute(
      'aria-pressed',
      'false',
    )
    expect(screen.getByTestId('reading-difficulty-filter-clear')).toBeInTheDocument()
  })
})
