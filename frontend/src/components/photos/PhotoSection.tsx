// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { PhotoGallery } from './PhotoGallery'
import { PhotoUploadModal } from './PhotoUploadModal'
import {
  useBookPhotos,
  useAuthorPhotos,
  useUploadBookPhoto,
  useUploadAuthorPhoto,
  useDeletePhoto,
  useRotatePhotoCW,
  useRotatePhotoCCW,
  useMovePhotoLeft,
  useMovePhotoRight,
} from '@/api/photos'
import { PiUpload } from 'react-icons/pi'

interface PhotoSectionProps {
  entityType: 'book' | 'author'
  entityId: number
  entityName: string
}

export function PhotoSection({ entityType, entityId, entityName }: PhotoSectionProps) {
  const [showUploadModal, setShowUploadModal] = useState(false)

  // Fetch photos based on entity type
  const { data: bookPhotos = [], isLoading: loadingBookPhotos } = useBookPhotos(
    entityType === 'book' ? entityId : 0
  )
  const { data: authorPhotos = [], isLoading: loadingAuthorPhotos } = useAuthorPhotos(
    entityType === 'author' ? entityId : 0
  )

  const photos = entityType === 'book' ? bookPhotos : authorPhotos
  const isLoading = entityType === 'book' ? loadingBookPhotos : loadingAuthorPhotos

  // Mutations
  const uploadBookPhoto = useUploadBookPhoto()
  const uploadAuthorPhoto = useUploadAuthorPhoto()
  const deletePhoto = useDeletePhoto()
  const rotateCW = useRotatePhotoCW()
  const rotateCCW = useRotatePhotoCCW()
  const moveLeft = useMovePhotoLeft()
  const moveRight = useMovePhotoRight()

  const handleUpload = async (file: File) => {
    if (entityType === 'book') {
      await uploadBookPhoto.mutateAsync({ bookId: entityId, file })
    } else {
      await uploadAuthorPhoto.mutateAsync({ authorId: entityId, file })
    }
  }

  const handleDelete = async (photoId: number) => {
    await deletePhoto.mutateAsync(photoId)
  }

  const handleRotateCW = async (photoId: number) => {
    await rotateCW.mutateAsync({
      photoId,
      bookId: entityType === 'book' ? entityId : undefined,
      authorId: entityType === 'author' ? entityId : undefined,
    })
  }

  const handleRotateCCW = async (photoId: number) => {
    await rotateCCW.mutateAsync({
      photoId,
      bookId: entityType === 'book' ? entityId : undefined,
      authorId: entityType === 'author' ? entityId : undefined,
    })
  }

  const handleMoveLeft = async (photoId: number) => {
    await moveLeft.mutateAsync({
      photoId,
      bookId: entityType === 'book' ? entityId : undefined,
      authorId: entityType === 'author' ? entityId : undefined,
    })
  }

  const handleMoveRight = async (photoId: number) => {
    await moveRight.mutateAsync({
      photoId,
      bookId: entityType === 'book' ? entityId : undefined,
      authorId: entityType === 'author' ? entityId : undefined,
    })
  }

  const isUploading = uploadBookPhoto.isPending || uploadAuthorPhoto.isPending
  const isMutating =
    deletePhoto.isPending ||
    rotateCW.isPending ||
    rotateCCW.isPending ||
    moveLeft.isPending ||
    moveRight.isPending

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Photos</h2>
          <p className="text-gray-600 mt-1">
            {photos.length} {photos.length === 1 ? 'photo' : 'photos'}
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setShowUploadModal(true)}
          leftIcon={<PiUpload />}
          data-test="upload-photo-button"
        >
          Upload Photo
        </Button>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-gray-500">
          <p>Loading photos...</p>
        </div>
      ) : (
        <PhotoGallery
          photos={photos}
          onDelete={handleDelete}
          onRotateCW={handleRotateCW}
          onRotateCCW={handleRotateCCW}
          onMoveLeft={handleMoveLeft}
          onMoveRight={handleMoveRight}
          isLoading={isMutating}
        />
      )}

      <PhotoUploadModal
        isOpen={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        onUpload={handleUpload}
        title={`Upload Photo for ${entityName}`}
        isUploading={isUploading}
      />
    </div>
  )
}
