// (c) Copyright 2025 by Muczynski
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ReadingDifficultyLookupResultsModal } from '../ReadingDifficultyLookupResultsModal'
import type { ReadingDifficultyLookupResultDto } from '@/types/dtos'

describe('ReadingDifficultyLookupResultsModal', () => {
  it('shows filled and skipped books with difficulty labels', async () => {
    const results: ReadingDifficultyLookupResultDto[] = [
      { bookId: 1, title: 'Little Women', success: true, suggestedDifficulty: 'children' },
      { bookId: 2, title: 'Summa', success: false, errorMessage: 'Already has a reading difficulty' },
    ]

    render(
      <MemoryRouter>
        <ReadingDifficultyLookupResultsModal isOpen onClose={() => {}} results={results} />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('1 book processed successfully, 1 skipped or failed')).toBeInTheDocument()
    })
    expect(screen.getByText('Little Women')).toBeInTheDocument()
    expect(screen.getByText('Children')).toBeInTheDocument()
    expect(screen.getByText('Filled')).toBeInTheDocument()
    expect(screen.getByText('Summa')).toBeInTheDocument()
    expect(screen.getByText('Already has a reading difficulty')).toBeInTheDocument()
    expect(screen.getByText('Skipped')).toBeInTheDocument()
  })
})
