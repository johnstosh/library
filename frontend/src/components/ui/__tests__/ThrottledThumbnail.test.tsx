// (c) Copyright 2025 by Muczynski
import { render, screen, waitFor, cleanup } from '@testing-library/react'
import {
  ThrottledThumbnail,
  clearThumbnailCache,
  _resetForTesting,
  _getCacheEntryForTesting,
} from '../ThrottledThumbnail'

// --- Mock Setup ---

let blobCounter = 0
const revokedUrls: string[] = []

beforeEach(() => {
  _resetForTesting()
  blobCounter = 0
  revokedUrls.length = 0

  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(new Blob(['fake-image'], { type: 'image/png' })),
      })
    )
  )

  vi.stubGlobal(
    'URL',
    {
      ...globalThis.URL,
      createObjectURL: vi.fn(() => {
        blobCounter++
        return `blob:mock-${blobCounter}`
      }),
      revokeObjectURL: vi.fn((url: string) => {
        revokedUrls.push(url)
      }),
    }
  )
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

// --- Cache Behavior ---

describe('Checksum cache', () => {
  it('should cache blob URL by checksum after first load', async () => {
    render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const entry = _getCacheEntryForTesting('abc123')
    expect(entry).not.toBeNull()
    expect(entry!.url).toBe('blob:mock-1')
  })

  it('should reuse cached blob URL for same checksum (no extra fetch)', async () => {
    render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const fetchCountBefore = (fetch as ReturnType<typeof vi.fn>).mock.calls.length

    // Mount a second component with the same checksum
    render(
      <ThrottledThumbnail photoId={2} url="/api/photos/2/thumbnail" alt="photo2" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo2')).toBeInTheDocument()
    })

    const fetchCountAfter = (fetch as ReturnType<typeof vi.fn>).mock.calls.length
    expect(fetchCountAfter).toBe(fetchCountBefore) // no extra fetch
  })

  it('clearThumbnailCache should revoke all URLs and clear cache', async () => {
    render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    clearThumbnailCache()

    expect(revokedUrls).toContain('blob:mock-1')
    const entry = _getCacheEntryForTesting('abc123')
    expect(entry).toBeNull()
  })
})

// --- Blob URL Stability ---

describe('Blob URL stability (no premature revocation)', () => {
  it('should NOT revoke blob URL when one of two components sharing a checksum unmounts', async () => {
    // Mount first component
    render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="shared" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    // Mount second component with same checksum
    const { unmount: unmount2 } = render(
      <ThrottledThumbnail photoId={2} url="/api/photos/2/thumbnail" alt="photo2" checksum="shared" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo2')).toBeInTheDocument()
    })

    // Unmount second — blob URL should NOT be revoked
    unmount2()
    expect(revokedUrls).not.toContain('blob:mock-1')

    // First component's img should still be functional
    expect(screen.getByAltText('photo1')).toHaveAttribute('src', 'blob:mock-1')
  })

  it('should NOT revoke blob URL when single component unmounts', async () => {
    const { unmount } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    unmount()

    // Blob URL should NOT be revoked — it stays in cache for potential reuse
    expect(revokedUrls).not.toContain('blob:mock-1')
    // Cache should still have the entry
    expect(_getCacheEntryForTesting('abc')).not.toBeNull()
  })

  it('should NOT revoke uncached blob URL on unmount', async () => {
    const { unmount } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    unmount()

    // Even uncached blob URLs are not revoked — the minor memory cost
    // is acceptable to avoid the disappearing thumbnail bug
    expect(revokedUrls.length).toBe(0)
  })
})

// --- Deps Behavior ---

describe('Effect deps (url and checksum only)', () => {
  it('should not refetch when only photoId changes but url and checksum stay the same', async () => {
    const { rerender } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="stable" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const fetchCountBefore = (fetch as ReturnType<typeof vi.fn>).mock.calls.length

    // Rerender with different photoId but same url and checksum
    rerender(
      <ThrottledThumbnail photoId={999} url="/api/photos/1/thumbnail" alt="photo1" checksum="stable" />
    )

    // Wait a tick for any potential effect re-run
    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const fetchCountAfter = (fetch as ReturnType<typeof vi.fn>).mock.calls.length
    expect(fetchCountAfter).toBe(fetchCountBefore)
  })

  it('should refetch when url changes', async () => {
    const { rerender } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const fetchCountBefore = (fetch as ReturnType<typeof vi.fn>).mock.calls.length

    // Rerender with different url
    rerender(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail?v=2" alt="photo1" />
    )

    await waitFor(() => {
      const fetchCountAfter = (fetch as ReturnType<typeof vi.fn>).mock.calls.length
      expect(fetchCountAfter).toBeGreaterThan(fetchCountBefore)
    })
  })
})
