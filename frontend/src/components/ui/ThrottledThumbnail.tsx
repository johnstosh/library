// (c) Copyright 2025 by Muczynski
import { useState, useEffect, useRef } from 'react'

// Global queue for throttled thumbnail loading
// Only one thumbnail loads at a time
const thumbnailQueue: Array<{
  id: number
  url: string
  checksum?: string
  resolve: (url: string) => void
  reject: (error: Error) => void
}> = []

let isProcessing = false

// Global cache for photos keyed by checksum
// Stores blob URL and reference count for proper lifecycle management
const checksumCache = new Map<string, { url: string; count: number }>()

function retainCachedUrl(checksum: string): void {
  const entry = checksumCache.get(checksum)
  if (entry) {
    entry.count++
  }
}

function releaseCachedUrl(checksum: string): void {
  const entry = checksumCache.get(checksum)
  if (entry) {
    entry.count--
    if (entry.count <= 0) {
      URL.revokeObjectURL(entry.url)
      checksumCache.delete(checksum)
    }
  }
}

async function processQueue() {
  if (isProcessing || thumbnailQueue.length === 0) {
    return
  }

  isProcessing = true

  while (thumbnailQueue.length > 0) {
    const item = thumbnailQueue.shift()
    if (!item) continue

    try {
      // Check cache first if checksum is provided
      if (item.checksum && checksumCache.has(item.checksum)) {
        const cached = checksumCache.get(item.checksum)!
        item.resolve(cached.url)
        continue
      }

      // Fetch the image to load it into browser cache
      const response = await fetch(item.url, { credentials: 'include' })
      if (!response.ok) {
        throw new Error(`Failed to load thumbnail: ${response.status}`)
      }
      // Create blob URL from response
      const blob = await response.blob()
      const blobUrl = URL.createObjectURL(blob)

      // Store in cache if checksum is provided (count starts at 0, component retains on use)
      if (item.checksum) {
        checksumCache.set(item.checksum, { url: blobUrl, count: 0 })
      }

      item.resolve(blobUrl)
    } catch (error) {
      item.reject(error instanceof Error ? error : new Error('Unknown error'))
    }
  }

  isProcessing = false
}

function queueThumbnail(id: number, url: string, checksum?: string): Promise<string> {
  // Check cache first if checksum is provided
  if (checksum && checksumCache.has(checksum)) {
    return Promise.resolve(checksumCache.get(checksum)!.url)
  }

  return new Promise((resolve, reject) => {
    thumbnailQueue.push({ id, url, checksum, resolve, reject })
    processQueue()
  })
}

/**
 * Clear the thumbnail loading queue.
 * Call this when navigating away from a page with many thumbnails
 * to prevent unnecessary network requests.
 */
export function clearThumbnailQueue(): void {
  // Reject all pending items
  while (thumbnailQueue.length > 0) {
    const item = thumbnailQueue.shift()
    if (item) {
      item.reject(new Error('Queue cleared'))
    }
  }
}

/**
 * Clear the checksum cache.
 * This revokes all cached blob URLs to free memory.
 * Use with caution - typically only for memory management.
 */
export function clearThumbnailCache(): void {
  checksumCache.forEach((entry) => {
    URL.revokeObjectURL(entry.url)
  })
  checksumCache.clear()
}

// Test-only exports for unit testing
export function _resetForTesting(): void {
  thumbnailQueue.length = 0
  checksumCache.clear()
  isProcessing = false
}

export function _getCacheEntryForTesting(checksum: string) {
  return checksumCache.get(checksum) ?? null
}

interface ThrottledThumbnailProps {
  photoId: number
  url: string
  alt: string
  className?: string
  checksum?: string
  respectOrientation?: boolean
}

export function ThrottledThumbnail({ photoId, url, alt, className, checksum, respectOrientation }: ThrottledThumbnailProps) {
  const [loadedUrl, setLoadedUrl] = useState<string | null>(() => {
    if (checksum && checksumCache.has(checksum)) {
      return checksumCache.get(checksum)!.url
    }
    return null
  })
  const [isLoading, setIsLoading] = useState(() => {
    return !(checksum && checksumCache.has(checksum))
  })
  const [error, setError] = useState(false)
  const mountedRef = useRef(true)
  // Bug 4 fix: refs to track current values for cleanup (avoids stale closures)
  const loadedUrlRef = useRef<string | null>(null)
  const checksumRef = useRef<string | undefined>(undefined)

  useEffect(() => {
    mountedRef.current = true
    checksumRef.current = checksum

    // Check cache first for immediate display (Bug 6: always return cleanup)
    if (checksum && checksumCache.has(checksum)) {
      const cachedUrl = checksumCache.get(checksum)!.url
      setLoadedUrl(cachedUrl)
      setIsLoading(false)
      loadedUrlRef.current = cachedUrl
      retainCachedUrl(checksum)

      return () => {
        mountedRef.current = false
        if (checksumRef.current) {
          releaseCachedUrl(checksumRef.current)
        }
        loadedUrlRef.current = null
      }
    }

    setIsLoading(true)
    setError(false)
    setLoadedUrl(null)

    queueThumbnail(photoId, url, checksum)
      .then((blobUrl) => {
        if (mountedRef.current) {
          setLoadedUrl(blobUrl)
          setIsLoading(false)
        }
        // Track the URL in the ref for cleanup (Bug 4 fix)
        loadedUrlRef.current = blobUrl
        if (checksum) {
          retainCachedUrl(checksum)
        }
      })
      .catch((err) => {
        // Ignore "Queue cleared" errors - they're expected during navigation
        if (mountedRef.current && err.message !== 'Queue cleared') {
          setError(true)
          setIsLoading(false)
        }
      })

    return () => {
      mountedRef.current = false
      // Bug 4 fix: read from refs instead of stale closure values
      if (checksumRef.current) {
        releaseCachedUrl(checksumRef.current)
      } else if (loadedUrlRef.current) {
        URL.revokeObjectURL(loadedUrlRef.current)
      }
      loadedUrlRef.current = null
    }
  }, [url, checksum]) // Bug 5 fix: removed photoId from deps

  if (isLoading) {
    return (
      <div className={`bg-gray-200 animate-pulse flex items-center justify-center ${className || 'w-12'}`}>
        <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin"></div>
      </div>
    )
  }

  if (error || !loadedUrl) {
    return (
      <div className={`bg-gray-200 flex items-center justify-center text-gray-400 ${className || 'w-12 h-12'}`}>
        -
      </div>
    )
  }

  // CSS image-orientation: from-image tells the browser to respect EXIF orientation data
  const style = respectOrientation ? { imageOrientation: 'from-image' as const } : undefined

  return (
    <img
      src={loadedUrl}
      alt={alt}
      className={className}
      style={style}
      data-test="thumbnail-img"
    />
  )
}
