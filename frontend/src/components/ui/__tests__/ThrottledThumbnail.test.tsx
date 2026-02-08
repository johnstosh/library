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

// --- Bug 7: Reference Counting ---

describe('Bug 7: Reference counting for cached blob URLs', () => {
  it('should track reference count (count=1 after first mount)', async () => {
    render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const entry = _getCacheEntryForTesting('abc123')
    expect(entry).not.toBeNull()
    expect(entry!.url).toBe('blob:mock-1')
    expect(entry!.count).toBe(1)
  })

  it('should track reference count (count=2 after second mount with same checksum)', async () => {
    render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    // Mount a second component with the same checksum
    render(
      <ThrottledThumbnail photoId={2} url="/api/photos/2/thumbnail" alt="photo2" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo2')).toBeInTheDocument()
    })

    const entry = _getCacheEntryForTesting('abc123')
    expect(entry).not.toBeNull()
    expect(entry!.count).toBe(2)
  })

  it('should revoke blob URL only when reference count reaches zero', async () => {
    const { unmount: unmount1 } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    const { unmount: unmount2 } = render(
      <ThrottledThumbnail photoId={2} url="/api/photos/2/thumbnail" alt="photo2" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo2')).toBeInTheDocument()
    })

    // Unmount first — count should go to 1, URL NOT revoked
    unmount1()
    expect(revokedUrls).not.toContain('blob:mock-1')
    const entryAfterFirst = _getCacheEntryForTesting('abc123')
    expect(entryAfterFirst).not.toBeNull()
    expect(entryAfterFirst!.count).toBe(1)

    // Unmount second — count should go to 0, URL IS revoked
    unmount2()
    expect(revokedUrls).toContain('blob:mock-1')
    const entryAfterSecond = _getCacheEntryForTesting('abc123')
    expect(entryAfterSecond).toBeNull()
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

// --- Bug 6: Always Return Cleanup ---

describe('Bug 6: Always return cleanup from useEffect', () => {
  it('should decrement reference count on unmount even after cache hit path', async () => {
    // First render to populate cache
    const { unmount: unmount1 } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    // Second render — should hit cache immediately
    const { unmount: unmount2 } = render(
      <ThrottledThumbnail photoId={2} url="/api/photos/2/thumbnail" alt="photo2" checksum="abc123" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo2')).toBeInTheDocument()
    })

    const entryBefore = _getCacheEntryForTesting('abc123')
    expect(entryBefore!.count).toBe(2)

    // Unmount the cache-hit component — should still decrement
    unmount2()

    const entryAfter = _getCacheEntryForTesting('abc123')
    expect(entryAfter).not.toBeNull()
    expect(entryAfter!.count).toBe(1)

    // Unmount the original — should revoke
    unmount1()
    expect(revokedUrls).toContain('blob:mock-1')
    expect(_getCacheEntryForTesting('abc123')).toBeNull()
  })
})

// --- Bug 4: Stale Closure Fix ---

describe('Bug 4: Stale closure in useEffect cleanup', () => {
  it('should revoke uncached blob URL on unmount (using current value, not stale null)', async () => {
    const { unmount } = render(
      <ThrottledThumbnail photoId={1} url="/api/photos/1/thumbnail" alt="photo1" />
    )

    await waitFor(() => {
      expect(screen.getByAltText('photo1')).toBeInTheDocument()
    })

    unmount()

    // The blob URL should be revoked — without the fix, the stale closure
    // captures loadedUrl=null and never calls revokeObjectURL
    expect(revokedUrls).toContain('blob:mock-1')
  })

  it('should not revoke cached blob URL when other components still reference it', async () => {
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

    // Unmount second — should NOT revoke since first still uses it
    unmount2()
    expect(revokedUrls).not.toContain('blob:mock-1')

    // First component's img should still be functional
    expect(screen.getByAltText('photo1')).toHaveAttribute('src', 'blob:mock-1')
  })
})

// --- Bug 5: Remove photoId from Deps ---

describe('Bug 5: Remove photoId from deps array', () => {
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
