// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { BookFilters, hideBookFilterOnMobile } from '../BookFilters'
import { defaultBookChipFilters, type BookChipFilters } from '@/utils/bookChipFilters'

const chips: BookChipFilters = { ...defaultBookChipFilters }

describe('hideBookFilterOnMobile', () => {
  it('hides chips whose names contain without, plus notActiveStatus', () => {
    expect(hideBookFilterOnMobile('withoutLoc')).toBe(true)
    expect(hideBookFilterOnMobile('withoutGrokipedia')).toBe(true)
    expect(hideBookFilterOnMobile('withoutGenres')).toBe(true)
    expect(hideBookFilterOnMobile('withoutFreeTextUrls')).toBe(true)
    expect(hideBookFilterOnMobile('notActiveStatus')).toBe(true)
  })

  it('keeps other chips visible on phone', () => {
    expect(hideBookFilterOnMobile('inLibrary')).toBe(false)
    expect(hideBookFilterOnMobile('withGrokipedia')).toBe(false)
    expect(hideBookFilterOnMobile('mostRecent')).toBe(false)
    expect(hideBookFilterOnMobile('freeText')).toBe(false)
  })
})

describe('BookFilters', () => {
  it('shows without-* and Not Active Status chips by default', () => {
    render(<BookFilters chips={chips} onToggle={() => {}} />)

    expect(screen.getByTestId('filter-without-loc')).not.toHaveClass('hidden')
    expect(screen.getByTestId('filter-without-grokipedia')).not.toHaveClass('hidden')
    expect(screen.getByTestId('filter-without-genres')).not.toHaveClass('hidden')
    expect(screen.getByTestId('filter-without-free-text-urls')).not.toHaveClass('hidden')
    expect(screen.getByTestId('filter-not-active-status')).not.toHaveClass('hidden')
  })

  it('hides without-* and Not Active Status chips on phone when asked', () => {
    render(
      <BookFilters chips={chips} onToggle={() => {}} hideWithoutAndNotActiveOnMobile />,
    )

    expect(screen.getByTestId('filter-without-loc')).toHaveClass('hidden', 'sm:inline-flex')
    expect(screen.getByTestId('filter-without-grokipedia')).toHaveClass('hidden', 'sm:inline-flex')
    expect(screen.getByTestId('filter-without-genres')).toHaveClass('hidden', 'sm:inline-flex')
    expect(screen.getByTestId('filter-without-free-text-urls')).toHaveClass('hidden', 'sm:inline-flex')
    expect(screen.getByTestId('filter-not-active-status')).toHaveClass('hidden', 'sm:inline-flex')

    expect(screen.getByTestId('filter-in-library')).not.toHaveClass('hidden')
    expect(screen.getByTestId('filter-with-grokipedia')).not.toHaveClass('hidden')
    expect(screen.getByTestId('filter-most-recent')).not.toHaveClass('hidden')
  })
})
