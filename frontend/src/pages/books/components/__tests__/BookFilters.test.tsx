// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import { BookFilters } from '../BookFilters'
import { defaultBookChipFilters, type BookChipFilters } from '@/utils/bookChipFilters'

const chips: BookChipFilters = { ...defaultBookChipFilters }

describe('BookFilters', () => {
  it('shows cataloger chips by default', () => {
    render(<BookFilters chips={chips} onToggle={() => {}} />)

    expect(screen.queryByTestId('filter-without-loc')).not.toBeInTheDocument()
    expect(screen.getByTestId('filter-without-grokipedia')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-genres')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-free-text-urls')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-proper-plot-or-description')).toBeInTheDocument()
    expect(screen.getByTestId('filter-title-not-chicago')).toHaveTextContent('Title not Chicago case')
    expect(screen.getByTestId('filter-author-not-canonical')).toHaveTextContent('Author not canonical')
    expect(screen.queryByTestId('filter-not-active-status')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-requested-status')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-in-library')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-electronic')).not.toBeInTheDocument()
    expect(screen.getByTestId('filter-most-recent')).toBeInTheDocument()
    expect(screen.getByText('Recent Arrivals')).toBeInTheDocument()
    expect(screen.getByTestId('filter-with-grokipedia')).toBeInTheDocument()
  })

  it('shows Without chips in each public library box', () => {
    render(<BookFilters chips={chips} onToggle={() => {}} showAvailabilityFilters />)

    expect(screen.getByTestId('filter-without-ydl-audio')).toHaveTextContent('Without Audio')
    expect(screen.getByTestId('filter-without-ydl-book')).toHaveTextContent('Without Book')
    expect(screen.getByTestId('filter-without-ydl-ebook')).toHaveTextContent('Without Ebook')
    expect(screen.getByTestId('filter-without-emu-audio')).toBeInTheDocument()
    expect(screen.getByTestId('filter-without-acla-ebook')).toBeInTheDocument()
    expect(screen.queryByTestId('filter-library-checked-older')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-library-lookup-error')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-free-text-searched-older')).not.toBeInTheDocument()
  })

  it('shows the library data age filter when the page opts in', () => {
    const onLibraryCheckedOlderChange = vi.fn()
    const props = {
      chips,
      onToggle: () => {},
      showAvailabilityFilters: true,
      libraryCheckedOlderDays: 90,
      onLibraryCheckedOlderChange,
    }
    const view = render(<BookFilters {...props} libraryCheckedOlder="" />)

    const select = screen.getByTestId('filter-library-checked-older')
    expect(select).toHaveValue('')
    expect(screen.getByTestId('filter-library-checked-older-days')).toBeDisabled()

    fireEvent.change(select, { target: { value: 'MSU' } })
    expect(onLibraryCheckedOlderChange).toHaveBeenCalledWith('MSU', 90)

    view.rerender(<BookFilters {...props} libraryCheckedOlder="MSU" />)
    expect(screen.getByTestId('filter-library-checked-older-days')).toBeEnabled()
    fireEvent.change(screen.getByTestId('filter-library-checked-older-days'), { target: { value: '14' } })
    expect(onLibraryCheckedOlderChange).toHaveBeenCalledWith('MSU', 14)

    const onLibraryLookupErrorChange = vi.fn()
    view.rerender(<BookFilters {...props} libraryCheckedOlder="MSU" onLibraryLookupErrorChange={onLibraryLookupErrorChange} />)
    const errorSelect = screen.getByTestId('filter-library-lookup-error')
    expect(errorSelect).toHaveValue('')
    fireEvent.change(errorSelect, { target: { value: 'WCC' } })
    expect(onLibraryLookupErrorChange).toHaveBeenCalledWith('WCC')
    expect(screen.queryByTestId('filter-free-text-searched-older')).not.toBeInTheDocument()
  })

  it('shows the free-text age chip only when the books page opts in', () => {
    const onFreeTextSearchedOlderChange = vi.fn()
    const props = {
      chips,
      onToggle: () => {},
      showFreeTextAge: true,
      onFreeTextSearchedOlderChange,
    }
    const view = render(<BookFilters {...props} freeTextSearchedOlderDays={null} />)

    const box = screen.getByTestId('filter-free-text-searched-older')
    expect(box).not.toBeChecked()
    expect(screen.getByTestId('filter-free-text-searched-older-days')).toBeDisabled()
    expect(screen.getByTestId('filter-free-text-searched-older-days')).toHaveValue(90)

    fireEvent.click(box)
    expect(onFreeTextSearchedOlderChange).toHaveBeenCalledWith(true, 90)

    view.rerender(<BookFilters {...props} freeTextSearchedOlderDays={90} />)
    expect(screen.getByTestId('filter-free-text-searched-older-days')).toBeEnabled()
    fireEvent.change(screen.getByTestId('filter-free-text-searched-older-days'), { target: { value: '30' } })
    expect(onFreeTextSearchedOlderChange).toHaveBeenCalledWith(true, 30)
  })

  it('hides cataloger chips when showCatalogerFilters is false', () => {
    render(<BookFilters chips={chips} onToggle={() => {}} showCatalogerFilters={false} />)

    expect(screen.queryByTestId('filter-without-loc')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-grokipedia')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-genres')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-free-text-urls')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-without-proper-plot-or-description')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-title-not-chicago')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-author-not-canonical')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-not-active-status')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-with-grokipedia')).not.toBeInTheDocument()
    expect(screen.queryByTestId('book-source-filter-chips')).not.toBeInTheDocument()
    expect(screen.queryByTestId('book-price-filters')).not.toBeInTheDocument()

    expect(screen.getByTestId('filter-free-text')).toBeInTheDocument()
    expect(screen.getByTestId('filter-audio')).toBeInTheDocument()
    expect(screen.getByTestId('filter-most-recent')).toBeInTheDocument()
    expect(screen.getByText('Recent Arrivals')).toBeInTheDocument()
  })
})
