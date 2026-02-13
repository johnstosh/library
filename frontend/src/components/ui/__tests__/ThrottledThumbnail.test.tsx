// (c) Copyright 2025 by Muczynski
import { render, screen, fireEvent } from '@testing-library/react'
import { Thumbnail, ThrottledThumbnail } from '../ThrottledThumbnail'
import { getThumbnailUrl } from '@/api/photos'

describe('Thumbnail', () => {
  it('renders loading placeholder initially', () => {
    render(
      <Thumbnail photoId={1} url="/api/photos/1/thumbnail?width=70" alt="photo1" />
    )

    // Spinner should be visible
    const spinner = document.querySelector('.animate-spin')
    expect(spinner).toBeInTheDocument()

    // Image should be hidden
    const img = screen.getByAltText('photo1')
    expect(img).toHaveClass('hidden')
  })

  it('renders img with correct src/alt/data-test after onLoad fires', () => {
    render(
      <Thumbnail photoId={1} url="/api/photos/1/thumbnail?width=70" alt="photo1" className="w-14" />
    )

    const img = screen.getByAltText('photo1')
    fireEvent.load(img)

    expect(img).toHaveAttribute('src', '/api/photos/1/thumbnail?width=70')
    expect(img).toHaveAttribute('data-test', 'thumbnail-img')
    expect(img).toHaveClass('w-14')
    expect(img).not.toHaveClass('hidden')

    // Spinner should be gone
    const spinner = document.querySelector('.animate-spin')
    expect(spinner).not.toBeInTheDocument()
  })

  it('renders error state (dash) after onError fires', () => {
    render(
      <Thumbnail photoId={1} url="/api/photos/1/thumbnail?width=70" alt="photo1" />
    )

    const img = screen.getByAltText('photo1')
    fireEvent.error(img)

    // Error state should show a dash
    expect(screen.getByText('-')).toBeInTheDocument()

    // Image should be gone
    expect(screen.queryByAltText('photo1')).not.toBeInTheDocument()
  })

  it('applies imageOrientation: from-image style when respectOrientation is true', () => {
    render(
      <Thumbnail
        photoId={1}
        url="/api/photos/1/thumbnail?width=70"
        alt="photo1"
        respectOrientation
      />
    )

    const img = screen.getByAltText('photo1')
    expect(img).toHaveStyle({ imageOrientation: 'from-image' })
  })

  it('exports ThrottledThumbnail as backward-compatible alias', () => {
    expect(ThrottledThumbnail).toBeDefined()
    expect(ThrottledThumbnail).toBe(Thumbnail)
  })
})

describe('getThumbnailUrl', () => {
  it('builds URL with checksum', () => {
    expect(getThumbnailUrl(1, 70, 'abc123')).toBe('/api/photos/1/thumbnail?width=70&v=abc123')
  })

  it('builds URL without checksum', () => {
    expect(getThumbnailUrl(1, 70)).toBe('/api/photos/1/thumbnail?width=70')
  })
})
