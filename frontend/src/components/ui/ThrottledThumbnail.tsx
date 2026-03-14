// (c) Copyright 2025 by Muczynski
import { useEffect, useRef, useState } from 'react'

// --- Sequential load queue ---
// Images register when they enter the viewport. Only one loads at a time.

type LoadCallback = () => void

const queue: LoadCallback[] = []
let active = false

function enqueue(load: LoadCallback) {
  queue.push(load)
  if (!active) processNext()
}

function processNext() {
  const next = queue.shift()
  if (next) {
    active = true
    next()
  } else {
    active = false
  }
}

function advance() {
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
