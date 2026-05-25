// (c) Copyright 2025 by Muczynski
import { useEffect, useRef, useState } from 'react'

// --- Concurrent load queue ---
// Images register when they enter the viewport. Up to MAX_CONCURRENT load at a time,
// which keeps server load reasonable while allowing disk-cache hits to resolve in parallel.

type LoadCallback = () => void

const MAX_CONCURRENT = 6

const queue: LoadCallback[] = []
let activeCount = 0

function enqueue(load: LoadCallback) {
  queue.push(load)
  processNext()
}

function processNext() {
  while (activeCount < MAX_CONCURRENT && queue.length > 0) {
    const next = queue.shift()!
    activeCount++
    next()
  }
}

function advance() {
  activeCount--
  processNext()
}

// --- Component ---

interface ThumbnailProps {
  photoId: number
  url: string
  alt: string
  className?: string
  respectOrientation?: boolean
}

export function Thumbnail({ url, alt, className, respectOrientation }: ThumbnailProps) {
  const [activeSrc, setActiveSrc] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(false)
  const placeholderRef = useRef<HTMLDivElement>(null)
  const enqueuedRef = useRef(false)

  const style = respectOrientation ? { imageOrientation: 'from-image' as const } : undefined

  useEffect(() => {
    const el = placeholderRef.current
    if (!el) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !enqueuedRef.current) {
          enqueuedRef.current = true
          observer.disconnect()
          enqueue(() => setActiveSrc(url))
        }
      },
      { rootMargin: '100px' }
    )

    observer.observe(el)
    return () => observer.disconnect()
  }, [url])

  if (error) {
    return (
      <div className={`bg-gray-200 flex items-center justify-center text-gray-400 ${className || 'w-12 h-12'}`}>
        -
      </div>
    )
  }

  return (
    <>
      {isLoading && (
        <div
          ref={placeholderRef}
          className={`bg-gray-200 animate-pulse flex items-center justify-center ${className || 'w-12'}`}
        >
          {activeSrc && (
            <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin" />
          )}
        </div>
      )}
      {activeSrc && (
        <img
          src={activeSrc}
          alt={alt}
          className={isLoading ? 'hidden' : className}
          style={style}
          data-test="thumbnail-img"
          onLoad={() => {
            setIsLoading(false)
            advance()
          }}
          onError={() => {
            setError(true)
            setIsLoading(false)
            advance()
          }}
        />
      )}
    </>
  )
}

// Backward-compatible alias
export const ThrottledThumbnail = Thumbnail
