// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FavoriteListFilters } from '../FavoriteListFilters'

describe('FavoriteListFilters', () => {
  it('labels chips with count then list name and hides zero counts unless selected', () => {
    const onToggle = vi.fn()
    render(
      <FavoriteListFilters
        lists={[
          { listName: 'Have Read', count: 3 },
          { listName: 'Want to Read', count: 0 },
        ]}
        selected={['Want to Read']}
        onToggle={onToggle}
        onClear={() => {}}
      />,
    )

    expect(screen.getByTestId('favorite-filter-have-read')).toHaveTextContent('3 Have Read')
    expect(screen.getByTestId('favorite-filter-want-to-read')).toHaveTextContent('0 Want to Read')
    fireEvent.click(screen.getByTestId('favorite-filter-have-read'))
    expect(onToggle).toHaveBeenCalledWith('Have Read')
  })

  it('renders nothing when there are no lists and none selected', () => {
    const { container } = render(
      <FavoriteListFilters lists={[]} selected={[]} onToggle={() => {}} onClear={() => {}} />,
    )
    expect(container).toBeEmptyDOMElement()
  })
})
