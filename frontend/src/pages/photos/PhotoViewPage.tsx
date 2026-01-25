// (c) Copyright 2025 by Muczynski
import { useParams } from 'react-router-dom'
import { getImageUrl } from '@/api/photos'

export function PhotoViewPage() {
  const { id } = useParams<{ id: string }>()
  const photoId = parseInt(id || '0', 10)

  if (!photoId) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <p className="text-white text-lg">Photo not found</p>
      </div>
    )
  }

  const imageUrl = getImageUrl(photoId)

  return (
    <div className="min-h-screen bg-black flex items-center justify-center p-4">
      <img
        src={imageUrl}
        alt={`Photo ${photoId}`}
        className="max-w-full max-h-screen object-contain"
        data-test="photo-view-image"
      />
    </div>
  )
}
