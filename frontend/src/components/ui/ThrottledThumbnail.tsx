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
// This persists across component unmounts and page navigation
const checksumCache = new Map<string, string>()

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
        const cachedUrl = checksumCache.get(item.checksum)!
        item.resolve(cachedUrl)
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

      // Store in cache if checksum is provided
      if (item.checksum) {
        checksumCache.set(item.checksum, blobUrl)
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
    return Promise.resolve(checksumCache.get(checksum)!)
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
  checksumCache.forEach((blobUrl) => {
    URL.revokeObjectURL(blobUrl)
  })
  checksumCache.clear()
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
      return checksumCache.get(checksum)!
    }
    return null
  })
  const [isLoading, setIsLoading] = useState(() => {
    return !(checksum && checksumCache.has(checksum))
  })
  const [error, setError] = useState(false)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true

    // Check cache first for immediate display
    if (checksum && checksumCache.has(checksum)) {
      setLoadedUrl(checksumCache.get(checksum)!)
      setIsLoading(false)
      return
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
        // Don't revoke blob URL if it's cached by checksum
        // The cache manages the lifecycle
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
      // Only revoke if NOT cached (no checksum provided)
      if (loadedUrl && !checksum) {
        URL.revokeObjectURL(loadedUrl)
      }
    }
  }, [photoId, url, checksum])

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
