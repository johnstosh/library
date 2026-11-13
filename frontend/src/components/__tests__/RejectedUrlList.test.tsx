// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { RejectedUrlList } from '../RejectedUrlList'
import type { RejectedUrl } from '@/types/dtos'

function Harness({ initialLive = 'https://grokipedia.com/page/Hermann_Geiger' }: { initialLive?: string }) {
  const [rows, setRows] = useState<RejectedUrl[]>([])
  const [live, setLive] = useState(initialLive)
  return (
    <>
      <input aria-label="Grokipedia URL" value={live} onChange={(event) => setLive(event.target.value)} />
      <RejectedUrlList
        title="Rejected Grokipedia pages"
        testId="book-invalid-grokipedia"
        rows={rows}
        onChange={setRows}
        liveValue={live}
        onMove={(reason) => {
          setRows([{ url: live, reason }])
          setLive('')
        }}
      />
    </>
  )
}

describe('RejectedUrlList', () => {
  it('stays collapsed, then adds and removes a URL', () => {
    render(<Harness initialLive="" />)
    const details = screen.getByTestId('book-invalid-grokipedia')
    expect(details).not.toHaveAttribute('open')
    fireEvent.click(screen.getByText(/Rejected Grokipedia pages/))
    fireEvent.change(screen.getByTestId('book-invalid-grokipedia-url'), { target: { value: 'https://example.com/a' } })
    fireEvent.change(screen.getByTestId('book-invalid-grokipedia-reason'), { target: { value: 'dead link' } })
    fireEvent.click(screen.getByTestId('book-invalid-grokipedia-add'))
    expect(screen.getByText('https://example.com/a')).toBeInTheDocument()
    fireEvent.click(screen.getByTestId('book-invalid-grokipedia-remove'))
    expect(screen.queryByText('https://example.com/a')).not.toBeInTheDocument()
  })

  it('moves the live URL into the list and clears the field', () => {
    render(<Harness />)
    const live = screen.getByLabelText('Grokipedia URL')
    expect(live).toHaveValue('https://grokipedia.com/page/Hermann_Geiger')
    fireEvent.change(screen.getByTestId('book-invalid-grokipedia-move-reason'), { target: { value: 'different person' } })
    fireEvent.click(screen.getByTestId('book-invalid-grokipedia-move'))
    expect(live).toHaveValue('')
    expect(screen.getByText('https://grokipedia.com/page/Hermann_Geiger')).toBeInTheDocument()
  })

  it('warns when the live URL is already rejected', () => {
    render(
      <RejectedUrlList
        title="Rejected Grokipedia pages"
        testId="book-invalid-grokipedia"
        rows={[{ url: 'hermann_geiger', reason: 'different person' }]}
        onChange={() => {}}
        liveValue="https://grokipedia.com/page/Hermann_Geiger"
      />,
    )
    expect(screen.getByTestId('book-invalid-grokipedia-warning')).toHaveTextContent('already rejected')
  })
})
