// (c) Copyright 2025 by Muczynski
import { useState } from 'react'

interface ThumbnailProps {
  photoId: number
  url: string
  alt: string
  className?: string
  respectOrientation?: boolean
}

export function Thumbnail({ url, alt, className, respectOrientation }: ThumbnailProps) {
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(false)

  const style = respectOrientation ? { imageOrientation: 'from-image' as const } : undefined

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
        <div className={`bg-gray-200 animate-pulse flex items-center justify-center ${className || 'w-12'}`}>
          <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin"></div>
        </div>
      )}
      <img
        src={url}
        alt={alt}
        className={isLoading ? 'hidden' : className}
        style={style}
        data-test="thumbnail-img"
        onLoad={() => setIsLoading(false)}
        onError={() => {
          setError(true)
          setIsLoading(false)
        }}
      />
    </>
  )
}

// Backward-compatible alias
export const ThrottledThumbnail = Thumbnail
