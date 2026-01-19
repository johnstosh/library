// (c) Copyright 2025 by Muczynski
import { useState, useEffect, useRef } from 'react'

// Global queue for throttled thumbnail loading
// Only one thumbnail loads at a time
const thumbnailQueue: Array<{
  id: number
  url: string
  resolve: (url: string) => void
  reject: (error: Error) => void
}> = []

let isProcessing = false

async function processQueue() {
  if (isProcessing || thumbnailQueue.length === 0) {
    return
  }

  isProcessing = true

  while (thumbnailQueue.length > 0) {
    const item = thumbnailQueue.shift()
    if (!item) continue

    try {
      // Fetch the image to load it into browser cache
      const response = await fetch(item.url, { credentials: 'include' })
      if (!response.ok) {
        throw new Error(`Failed to load thumbnail: ${response.status}`)
      }
      // Create blob URL from response
      const blob = await response.blob()
      const blobUrl = URL.createObjectURL(blob)
      item.resolve(blobUrl)
    } catch (error) {
      item.reject(error instanceof Error ? error : new Error('Unknown error'))
    }
  }

  isProcessing = false
}

function queueThumbnail(id: number, url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    thumbnailQueue.push({ id, url, resolve, reject })
    processQueue()
  })
}

interface ThrottledThumbnailProps {
  photoId: number
  url: string
  alt: string
  className?: string
}

export function ThrottledThumbnail({ photoId, url, alt, className }: ThrottledThumbnailProps) {
  const [loadedUrl, setLoadedUrl] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(false)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true
    setIsLoading(true)
    setError(false)
    setLoadedUrl(null)

    queueThumbnail(photoId, url)
      .then((blobUrl) => {
        if (mountedRef.current) {
          setLoadedUrl(blobUrl)
          setIsLoading(false)
        } else {
          // Clean up blob URL if component unmounted
          URL.revokeObjectURL(blobUrl)
        }
      })
      .catch(() => {
        if (mountedRef.current) {
          setError(true)
          setIsLoading(false)
        }
      })

    return () => {
      mountedRef.current = false
      // Clean up blob URL on unmount
      if (loadedUrl) {
        URL.revokeObjectURL(loadedUrl)
      }
    }
  }, [photoId, url])

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

  return (
    <img
      src={loadedUrl}
      alt={alt}
      className={className}
    />
  )
}
