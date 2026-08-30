// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { BookFilters } from '../BookFilters'
import { defaultBookChipFilters, type BookChipFilters } from '@/utils/bookChipFilters'

const chips: BookChipFilters = { ...defaultBookChipFilters }

describe('BookFilters', () => {
  it('shows cataloger chips by default', () => {
    render(<BookFilters chips={chips} onToggle={() => {}} />)

    expect(screen.getByTestId('filter-without-loc')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-grokipedia')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-genres')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-free-text-urls')).toBeInTheDocument()
    expect(screen.getByTestId('filter-not-active-status')).toBeInTheDocument()
    expect(screen.getByTestId('filter-most-recent')).toBeInTheDocument()
    expect(screen.getByTestId('filter-with-grokipedia')).toBeInTheDocument()
  })

  it('hides cataloger chips when showCatalogerFilters is false', () => {
    render(<BookFilters chips={chips} onToggle={() => {}} showCatalogerFilters={false} />)

    expect(screen.queryByTestId('filter-without-loc')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-grokipedia')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-genres')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-free-text-urls')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-not-active-status')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-most-recent')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-with-grokipedia')).not.toBeInTheDocument()
    expect(screen.queryByTestId('book-source-filter-chips')).not.toBeInTheDocument()

    expect(screen.getByTestId('filter-in-library')).toBeInTheDocument()
    expect(screen.getByTestId('filter-electronic')).toBeInTheDocument()
    expect(screen.getByTestId('filter-free-text')).toBeInTheDocument()
    expect(screen.getByTestId('filter-audio')).toBeInTheDocument()
  })
})
