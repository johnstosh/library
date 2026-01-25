// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getImageUrl } from '@/api/photos'

export function PhotoViewPage() {
  const { id } = useParams<{ id: string }>()
  const photoId = parseInt(id || '0', 10)
  const [hasError, setHasError] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  if (!photoId) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <p className="text-white text-lg">Photo not found</p>
      </div>
    )
  }

  const imageUrl = getImageUrl(photoId)

  if (hasError) {
    return (
      <div className="min-h-screen bg-black flex flex-col items-center justify-center gap-4">
        <p className="text-white text-lg">Photo not found or failed to load</p>
        <p className="text-gray-400 text-sm">Photo ID: {photoId}</p>
        <Link
          to="/"
          className="text-blue-400 hover:text-blue-300 underline"
          data-test="photo-error-home-link"
        >
          Return to Home
        </Link>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-black flex items-center justify-center p-4">
      {isLoading && (
        <div className="absolute text-white text-lg">Loading...</div>
      )}
      <img
        src={imageUrl}
        alt={`Photo ${photoId}`}
        className="max-w-full max-h-screen object-contain"
        data-test="photo-view-image"
        onLoad={() => setIsLoading(false)}
        onError={() => {
          setIsLoading(false)
          setHasError(true)
        }}
      />
    </div>
  )
}
