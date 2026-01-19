// (c) Copyright 2025 by Muczynski
import { useState, useRef } from 'react'
import Cropper, { type ReactCropperElement } from 'react-cropper'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { PiUpload, PiArrowCounterClockwise, PiArrowClockwise } from 'react-icons/pi'

interface PhotoUploadModalProps {
  isOpen: boolean
  onClose: () => void
  onUpload: (file: File) => Promise<void>
  title: string
  isUploading: boolean
}

export function PhotoUploadModal({
  isOpen,
  onClose,
  onUpload,
  title,
  isUploading,
}: PhotoUploadModalProps) {
  const [image, setImage] = useState<string | null>(null)
  const [error, setError] = useState('')
  const cropperRef = useRef<ReactCropperElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setError('Please select an image file')
      return
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError('Image size must be less than 10MB')
      return
    }

    setError('')

    // Read file as data URL for cropper
    const reader = new FileReader()
    reader.onload = () => {
      setImage(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  const handleRotateLeft = () => {
    const cropper = cropperRef.current?.cropper
    if (cropper) {
      cropper.rotate(-90)
    }
  }

  const handleRotateRight = () => {
    const cropper = cropperRef.current?.cropper
    if (cropper) {
      cropper.rotate(90)
    }
  }

  const handleUpload = async () => {
    const cropper = cropperRef.current?.cropper
    if (!cropper) {
      setError('No image selected')
      return
    }

    setError('')

    try {
      // Get cropped canvas
      const canvas = cropper.getCroppedCanvas()

      // Convert canvas to blob
      const blob = await new Promise<Blob | null>((resolve) => {
        canvas.toBlob(resolve, 'image/jpeg', 0.95)
      })

      if (!blob) {
        setError('Failed to process image')
        return
      }

      // Create file from blob
      const file = new File([blob], 'photo.jpg', { type: 'image/jpeg' })

      await onUpload(file)
      handleClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to upload photo')
    }
  }

  const handleClose = () => {
    setImage(null)
    setError('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
    onClose()
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={title}
      size="xl"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={handleClose} disabled={isUploading} data-test="upload-photo-cancel">
            Cancel
          </Button>
          {image && (
            <Button
              variant="primary"
              onClick={handleUpload}
              isLoading={isUploading}
              data-test="upload-photo"
            >
              Upload Photo
            </Button>
          )}
        </div>
      }
    >
      <div className="space-y-4">
        {error && <ErrorMessage message={error} />}

        {!image ? (
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="hidden"
              data-test="photo-file-input"
            />
            <PiUpload className="w-12 h-12 mx-auto mb-4 text-gray-400" />
            <p className="text-gray-600 mb-4">
              Select an image to upload
            </p>
            <Button
              variant="primary"
              onClick={() => fileInputRef.current?.click()}
              data-test="select-photo"
            >
              Select Image
            </Button>
            <p className="text-sm text-gray-500 mt-4">
              Max file size: 10MB • Supported formats: JPEG, PNG, GIF, WebP
            </p>
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between mb-2">
              <p className="text-sm font-medium text-gray-700">
                Crop and adjust your image
              </p>
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleRotateLeft}
                  leftIcon={<PiArrowCounterClockwise />}
                  data-test="rotate-left"
                >
                  Rotate Left
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleRotateRight}
                  leftIcon={<PiArrowClockwise />}
                  data-test="rotate-right"
                >
                  Rotate Right
                </Button>
              </div>
            </div>

            <div className="border border-gray-300 rounded-lg overflow-hidden bg-gray-100">
              <Cropper
                ref={cropperRef}
                src={image}
                style={{ height: 400, width: '100%' }}
                aspectRatio={NaN}
                guides={true}
                viewMode={1}
                dragMode="move"
                cropBoxMovable={true}
                cropBoxResizable={true}
                toggleDragModeOnDblclick={false}
                background={false}
                responsive={true}
                autoCropArea={1}
                checkOrientation={true}
              />
            </div>

            <div className="flex items-center justify-between">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setImage(null)
                  if (fileInputRef.current) {
                    fileInputRef.current.value = ''
                  }
                }}
                data-test="change-photo"
              >
                Change Image
              </Button>
            </div>
          </>
        )}
      </div>
    </Modal>
  )
}
