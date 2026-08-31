// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { CoverThumbnail } from '../CoverThumbnail'

describe('CoverThumbnail', () => {
  it('renders a placeholder when there is no photo', () => {
    render(<CoverThumbnail alt="Cover of Untitled" dataTest="book-result-cover-1" />)

    const placeholder = screen.getByTestId('book-result-cover-1')
    expect(placeholder).toHaveTextContent('-')
    expect(screen.queryByAltText('Cover of Untitled')).not.toBeInTheDocument()
  })

  it('renders a thumbnail that links to the full-size photo', () => {
    render(
      <CoverThumbnail
        photoId={42}
        checksum="abc"
        alt="Cover of Summa"
        dataTest="book-result-cover-9"
      />,
    )

    const link = screen.getByTestId('book-result-cover-9')
    expect(link).toHaveAttribute('href', '/photos/42')
    expect(link).toHaveAttribute('target', '_blank')

    const img = screen.getByAltText('Cover of Summa')
    expect(img).toHaveAttribute('src', '/api/photos/42/thumbnail?width=70&v=abc')
    fireEvent.load(img)
    expect(img).toHaveAttribute('data-test', 'thumbnail-img')
  })

  it('stops click bubbling when stopPropagation is set', () => {
    const onParentClick = vi.fn()
    render(
      <div onClick={onParentClick}>
        <CoverThumbnail photoId={7} alt="Cover of City of God" stopPropagation />
      </div>,
    )

    fireEvent.click(screen.getByAltText('Cover of City of God').closest('a')!)
    expect(onParentClick).not.toHaveBeenCalled()
  })
})
