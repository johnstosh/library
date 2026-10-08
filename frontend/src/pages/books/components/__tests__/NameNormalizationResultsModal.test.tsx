// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { NameNormalizationResultsModal } from '../NameNormalizationResultsModal'

describe('NameNormalizationResultsModal', () => {
  it('links a merged author to the kept row', () => {
    const onClose = vi.fn()
    render(
      <MemoryRouter>
        <NameNormalizationResultsModal
          isOpen
          onClose={onClose}
          title="Canonical Author Names"
          subject="author"
          results={[
            {
              authorId: 9,
              name: 'St. Alphonsus Liguori',
              before: 'St. Alphonsus Liguori (ed. Frederick M. Jones, C.Ss.R.)',
              after: 'St. Alphonsus Liguori',
              changed: true,
              success: true,
              mergedIntoAuthorId: 4,
            },
          ]}
        />
      </MemoryRouter>
    )

    expect(screen.getByTestId('name-normalization-results')).toHaveTextContent(
      '1 author merged into an existing author'
    )
    expect(screen.getByTestId('name-normalization-merged')).toHaveTextContent(
      'this duplicate was removed'
    )
    expect(screen.getByRole('link', { name: 'St. Alphonsus Liguori' })).toHaveAttribute(
      'href',
      '/authors/4'
    )
  })
})
