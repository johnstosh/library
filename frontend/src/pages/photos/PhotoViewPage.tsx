// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getImageUrl } from '@/api/photos'

interface ErrorDetails {
  error: string
  message: string
}

export function PhotoViewPage() {
  const { id } = useParams<{ id: string }>()
  const photoId = parseInt(id || '0', 10)
  const [hasError, setHasError] = useState(false)
  const [errorDetails, setErrorDetails] = useState<ErrorDetails | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const imageUrl = getImageUrl(photoId)

  // Fetch error details when image fails to load
  useEffect(() => {
    if (hasError && !errorDetails) {
      fetch(imageUrl)
        .then(async (response) => {
          if (!response.ok) {
            const contentType = response.headers.get('content-type')
            if (contentType && contentType.includes('application/json')) {
              const errorData = await response.json()
              setErrorDetails(errorData)
            } else {
              setErrorDetails({
                error: `HTTP ${response.status}`,
                message: response.statusText || 'Failed to load photo',
              })
            }
          }
        })
        .catch(() => {
          // Network error - keep generic message
        })
    }
  }, [hasError, errorDetails, imageUrl])

  if (!photoId) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <p className="text-white text-lg">Photo not found</p>
      </div>
    )
  }

  if (hasError) {
    return (
      <div className="min-h-screen bg-black flex flex-col items-center justify-center gap-4">
        <p className="text-white text-lg">Photo not found or failed to load</p>
        <p className="text-gray-400 text-sm">Photo ID: {photoId}</p>
        {errorDetails && (
          <p className="text-red-400 text-sm max-w-md text-center" data-test="photo-error-message">
            Error: {errorDetails.message}
          </p>
        )}
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
