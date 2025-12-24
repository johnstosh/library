// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import type { PhotoDto } from '@/api/photos'
import { getPhotoUrl, getThumbnailUrl } from '@/api/photos'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import {
  PiTrash,
  PiArrowCounterClockwise,
  PiArrowClockwise,
  PiArrowLeft,
  PiArrowRight,
  PiMagnifyingGlassPlus,
} from 'react-icons/pi'

interface PhotoGalleryProps {
  photos: PhotoDto[]
  onDelete: (photoId: number) => void
  onRotateCW: (photoId: number) => void
  onRotateCCW: (photoId: number) => void
  onMoveLeft: (photoId: number) => void
  onMoveRight: (photoId: number) => void
  isLoading?: boolean
}

export function PhotoGallery({
  photos,
  onDelete,
  onRotateCW,
  onRotateCCW,
  onMoveLeft,
  onMoveRight,
  isLoading,
}: PhotoGalleryProps) {
  const [deletingPhotoId, setDeletingPhotoId] = useState<number | null>(null)
  const [lightboxPhoto, setLightboxPhoto] = useState<PhotoDto | null>(null)

  if (photos.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p>No photos yet</p>
      </div>
    )
  }

  const handleDeleteConfirm = () => {
    if (deletingPhotoId !== null) {
      onDelete(deletingPhotoId)
      setDeletingPhotoId(null)
    }
  }

  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {photos.map((photo, index) => (
          <div
            key={photo.id}
            className="relative group bg-white rounded-lg shadow-sm overflow-hidden border border-gray-200 hover:shadow-md transition-shadow"
            data-test={`photo-${photo.id}`}
          >
            {/* Photo */}
            <div className="aspect-square bg-gray-100">
              <img
                src={getThumbnailUrl(photo.id, 400)}
                alt={photo.caption || `Photo ${index + 1}`}
                className="w-full h-full object-cover cursor-pointer"
                onClick={() => setLightboxPhoto(photo)}
                loading="lazy"
              />
            </div>

            {/* Actions Overlay */}
            <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-40 transition-all duration-200">
              <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity p-2 flex flex-col justify-between">
                {/* Top Actions */}
                <div className="flex justify-end gap-1">
                  <button
                    onClick={() => setLightboxPhoto(photo)}
                    className="p-2 bg-white rounded-lg shadow-sm hover:bg-gray-100 transition-colors"
                    title="View Full Size"
                    data-test={`view-photo-${photo.id}`}
                  >
                    <PiMagnifyingGlassPlus className="w-4 h-4 text-gray-700" />
                  </button>
                  <button
                    onClick={() => setDeletingPhotoId(photo.id)}
                    className="p-2 bg-white rounded-lg shadow-sm hover:bg-red-50 transition-colors"
                    title="Delete"
                    data-test={`delete-photo-${photo.id}`}
                  >
                    <PiTrash className="w-4 h-4 text-red-600" />
                  </button>
                </div>

                {/* Bottom Actions */}
                <div className="flex justify-between gap-1">
                  <div className="flex gap-1">
                    <button
                      onClick={() => onRotateCCW(photo.id)}
                      className="p-2 bg-white rounded-lg shadow-sm hover:bg-gray-100 transition-colors"
                      title="Rotate Left"
                      data-test={`rotate-left-${photo.id}`}
                      disabled={isLoading}
                    >
                      <PiArrowCounterClockwise className="w-4 h-4 text-gray-700" />
                    </button>
                    <button
                      onClick={() => onRotateCW(photo.id)}
                      className="p-2 bg-white rounded-lg shadow-sm hover:bg-gray-100 transition-colors"
                      title="Rotate Right"
                      data-test={`rotate-right-${photo.id}`}
                      disabled={isLoading}
                    >
                      <PiArrowClockwise className="w-4 h-4 text-gray-700" />
                    </button>
                  </div>
                  <div className="flex gap-1">
                    <button
                      onClick={() => onMoveLeft(photo.id)}
                      className="p-2 bg-white rounded-lg shadow-sm hover:bg-gray-100 transition-colors disabled:opacity-50"
                      title="Move Left"
                      data-test={`move-left-${photo.id}`}
                      disabled={isLoading || index === 0}
                    >
                      <PiArrowLeft className="w-4 h-4 text-gray-700" />
                    </button>
                    <button
                      onClick={() => onMoveRight(photo.id)}
                      className="p-2 bg-white rounded-lg shadow-sm hover:bg-gray-100 transition-colors disabled:opacity-50"
                      title="Move Right"
                      data-test={`move-right-${photo.id}`}
                      disabled={isLoading || index === photos.length - 1}
                    >
                      <PiArrowRight className="w-4 h-4 text-gray-700" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Photo Number Badge */}
            <div className="absolute top-2 left-2 bg-black bg-opacity-60 text-white text-xs px-2 py-1 rounded">
              {index + 1}
            </div>
          </div>
        ))}
      </div>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={deletingPhotoId !== null}
        onClose={() => setDeletingPhotoId(null)}
        onConfirm={handleDeleteConfirm}
        title="Delete Photo"
        message="Are you sure you want to delete this photo? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
      />

      {/* Lightbox */}
      {lightboxPhoto && (
        <div
          className="fixed inset-0 z-50 bg-black bg-opacity-90 flex items-center justify-center p-4"
          onClick={() => setLightboxPhoto(null)}
        >
          <div className="relative max-w-7xl max-h-full" onClick={(e) => e.stopPropagation()}>
            <img
              src={getPhotoUrl(lightboxPhoto.id)}
              alt={lightboxPhoto.caption || 'Photo'}
              className="max-w-full max-h-[90vh] object-contain"
            />
            <button
              onClick={() => setLightboxPhoto(null)}
              className="absolute top-4 right-4 p-2 bg-white rounded-lg shadow-lg hover:bg-gray-100 transition-colors"
              data-test="close-lightbox"
            >
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>
      )}
    </>
  )
}
