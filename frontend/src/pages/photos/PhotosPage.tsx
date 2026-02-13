// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/Button'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import {
  usePhotoExportStats,
  usePhotoExportList,
  useExportSinglePhoto,
  useImportSinglePhoto,
  useVerifyPhoto,
  useUnlinkPhoto,
  useDeletePhoto,
  useUploadPhotoImage,
  type PhotoExportInfoDto,
} from '@/api/data-management'
import { getThumbnailUrl } from '@/api/photos'
import { ThrottledThumbnail } from '@/components/ui/ThrottledThumbnail'
import { formatDateTime, truncate } from '@/utils/formatters'
import {
  PiDownload,
  PiUpload,
  PiImage,
  PiArrowsClockwise,
  PiEye,
  PiCheck,
  PiLink,
  PiTrash,
} from 'react-icons/pi'

export function PhotosPage() {
  const [isExportingAllPhotos, setIsExportingAllPhotos] = useState(false)
  const [isImportingAllPhotos, setIsImportingAllPhotos] = useState(false)
  const [photoExportProgress, setPhotoExportProgress] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [pasteModePhotoId, setPasteModePhotoId] = useState<number | null>(null)
  const [pasteInstructions, setPasteInstructions] = useState<string>('')
  // Photo export hooks
  const { data: photoStats, refetch: refetchPhotoStats } = usePhotoExportStats()
  const { data: photoList = [], refetch: refetchPhotoList, isLoading: isLoadingPhotos } = usePhotoExportList()
  const exportSinglePhoto = useExportSinglePhoto()
  const importSinglePhoto = useImportSinglePhoto()
  const verifyPhoto = useVerifyPhoto()
  const unlinkPhoto = useUnlinkPhoto()
  const deletePhoto = useDeletePhoto()
  const uploadPhotoImage = useUploadPhotoImage()

  // Photo Export Handlers
  const handleRefreshPhotoStatus = () => {
    refetchPhotoStats()
    refetchPhotoList()
  }

  const handleExportAllPendingPhotos = async () => {
    const pendingPhotos = photoList.filter(
      (photo: PhotoExportInfoDto) => photo.hasImage && !photo.permanentId
    )

    if (pendingPhotos.length === 0) {
      setSuccessMessage('No pending photos to export.')
      return
    }

    if (
      !confirm(
        `Are you sure you want to export ${pendingPhotos.length} pending photo(s)? This may take a while.`
      )
    ) {
      return
    }

    setIsExportingAllPhotos(true)
    setSuccessMessage('')
    setErrorMessage('')

    let successCount = 0
    let failureCount = 0
    const failedPhotos: { id: number; bookTitle?: string; error: string }[] = []

    for (let i = 0; i < pendingPhotos.length; i++) {
      const photo = pendingPhotos[i]
      setPhotoExportProgress(`Exporting photo ${i + 1} of ${pendingPhotos.length}...`)

      try {
        await exportSinglePhoto.mutateAsync(photo.id)
        successCount++
      } catch (err) {
        failureCount++
        const errorMessage =
          err instanceof Error
            ? err.message
            : typeof err === 'object' && err !== null && 'message' in err
              ? String((err as { message: unknown }).message)
              : 'Unknown error'
        console.error(
          `Failed to export photo ${photo.id} (Book: ${photo.bookTitle || 'N/A'}):`,
          errorMessage
        )
        failedPhotos.push({
          id: photo.id,
          bookTitle: photo.bookTitle,
          error: errorMessage,
        })
      }

    }

    setPhotoExportProgress('')
    setIsExportingAllPhotos(false)

    if (failureCount === 0) {
      setSuccessMessage(`Successfully exported all ${successCount} photo(s)!`)
    } else {
      console.error('Failed photo exports:', failedPhotos)
      const failedBooks = [...new Set(failedPhotos.map((p) => p.bookTitle).filter(Boolean))]
      const failedBooksSummary =
        failedBooks.length > 0
          ? ` Failed books: ${failedBooks.slice(0, 3).join(', ')}${failedBooks.length > 3 ? '...' : ''}`
          : ''
      setErrorMessage(
        `Export completed: ${successCount} succeeded, ${failureCount} failed.${failedBooksSummary} Check browser console for details.`
      )
    }

    handleRefreshPhotoStatus()
  }

  const handleImportAllPendingPhotos = async () => {
    const pendingPhotos = photoList.filter(
      (photo: PhotoExportInfoDto) => photo.permanentId && !photo.hasImage
    )

    if (pendingPhotos.length === 0) {
      setSuccessMessage('No pending photos to import.')
      return
    }

    if (
      !confirm(
        `Are you sure you want to import ${pendingPhotos.length} pending photo(s) from Google Photos? This may take a while.`
      )
    ) {
      return
    }

    setIsImportingAllPhotos(true)
    setSuccessMessage('')
    setErrorMessage('')

    let successCount = 0
    let failureCount = 0
    const failedPhotos: { id: number; bookTitle?: string; error: string }[] = []

    for (let i = 0; i < pendingPhotos.length; i++) {
      const photo = pendingPhotos[i]
      setPhotoExportProgress(`Importing photo ${i + 1} of ${pendingPhotos.length}...`)

      try {
        await importSinglePhoto.mutateAsync(photo.id)
        successCount++
      } catch (err) {
        failureCount++
        const errorMessage =
          err instanceof Error
            ? err.message
            : typeof err === 'object' && err !== null && 'message' in err
              ? String((err as { message: unknown }).message)
              : 'Unknown error'
        console.error(
          `Failed to import photo ${photo.id} (Book: ${photo.bookTitle || 'N/A'}):`,
          errorMessage
        )
        failedPhotos.push({
          id: photo.id,
          bookTitle: photo.bookTitle,
          error: errorMessage,
        })
      }

    }

    setPhotoExportProgress('')
    setIsImportingAllPhotos(false)

    if (failureCount === 0) {
      setSuccessMessage(`Successfully imported all ${successCount} photo(s)!`)
    } else {
      console.error('Failed photo imports:', failedPhotos)
      const failedBooks = [...new Set(failedPhotos.map((p) => p.bookTitle).filter(Boolean))]
      const failedBooksSummary =
        failedBooks.length > 0
          ? ` Failed books: ${failedBooks.slice(0, 3).join(', ')}${failedBooks.length > 3 ? '...' : ''}`
          : ''
      setErrorMessage(
        `Import completed: ${successCount} succeeded, ${failureCount} failed.${failedBooksSummary} Check browser console for details.`
      )
    }

    handleRefreshPhotoStatus()
  }

  const handleExportSinglePhoto = async (photoId: number) => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await exportSinglePhoto.mutateAsync(photoId)
      setSuccessMessage(result.message || 'Photo exported successfully!')
      handleRefreshPhotoStatus()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to export photo #${photoId}: ${errorMessage}`)
    }
  }

  const handleImportSinglePhoto = async (photoId: number) => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await importSinglePhoto.mutateAsync(photoId)
      setSuccessMessage(result.message || 'Photo imported successfully!')
      handleRefreshPhotoStatus()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to import photo #${photoId}: ${errorMessage}`)
    }
  }

  const handleVerifyPhoto = async (photoId: number) => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await verifyPhoto.mutateAsync(photoId)
      if (result.valid) {
        setSuccessMessage(
          `Photo #${photoId} verified: ${result.message}${result.filename ? ` (${result.filename})` : ''}`
        )
      } else {
        setErrorMessage(`Photo #${photoId} verification failed: ${result.message}`)
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to verify photo #${photoId}: ${errorMessage}`)
    }
  }

  const handleUnlinkPhoto = async (photoId: number) => {
    if (
      !confirm(
        `Are you sure you want to unlink photo #${photoId}? This will remove the permanent ID and the photo will need to be re-exported.`
      )
    ) {
      return
    }

    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await unlinkPhoto.mutateAsync(photoId)
      setSuccessMessage(result.message || 'Photo unlinked successfully!')
      handleRefreshPhotoStatus()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to unlink photo #${photoId}: ${errorMessage}`)
    }
  }

  const handleDeletePhoto = async (photoId: number) => {
    if (!confirm(`Are you sure you want to delete photo #${photoId}?`)) {
      return
    }

    setSuccessMessage('')
    setErrorMessage('')

    try {
      await deletePhoto.mutateAsync(photoId)
      setSuccessMessage(`Photo #${photoId} deleted successfully!`)
      handleRefreshPhotoStatus()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to delete photo #${photoId}: ${errorMessage}`)
    }
  }

  const handleEnterPasteMode = (photoId: number) => {
    setPasteModePhotoId(photoId)
    setPasteInstructions(`Ready to paste. Press Ctrl+V to paste an image for photo #${photoId}`)
    setSuccessMessage('')
    setErrorMessage('')
    window.focus()
  }

  const handleCancelPasteMode = () => {
    setPasteModePhotoId(null)
    setPasteInstructions('')
  }

  const handlePaste = async (event: ClipboardEvent) => {
    if (pasteModePhotoId === null) return

    const items = event.clipboardData?.items
    if (!items) return

    let imageFile: File | null = null

    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (item.type.indexOf('image') !== -1) {
        const blob = item.getAsFile()
        if (blob) {
          imageFile = new File([blob], 'pasted-image.png', { type: blob.type })
          break
        }
      }
    }

    if (!imageFile) {
      setErrorMessage('No image found in clipboard. Please copy an image and try again.')
      return
    }

    setSuccessMessage('')
    setErrorMessage('')
    setPasteInstructions('Uploading pasted image...')

    try {
      await uploadPhotoImage.mutateAsync({ photoId: pasteModePhotoId, file: imageFile })
      setSuccessMessage(`Image pasted and uploaded successfully for photo #${pasteModePhotoId}!`)
      setPasteModePhotoId(null)
      setPasteInstructions('')
      handleRefreshPhotoStatus()
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to upload pasted image for photo #${pasteModePhotoId}: ${errorMessage}`)
      setPasteInstructions(`Ready to paste. Press Ctrl+V to paste an image for photo #${pasteModePhotoId}`)
    }
  }

  // Add paste event listener when in paste mode
  useEffect(() => {
    const pasteHandler = (event: Event) => handlePaste(event as ClipboardEvent)

    if (pasteModePhotoId !== null) {
      window.addEventListener('paste', pasteHandler)
    }

    return () => {
      window.removeEventListener('paste', pasteHandler)
    }
  }, [pasteModePhotoId])

  // Get status badge color class
  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-100 text-green-800'
      case 'FAILED':
        return 'bg-red-100 text-red-800'
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800'
      case 'NO_IMAGE':
        return 'bg-gray-100 text-gray-800'
      case 'PENDING_IMPORT':
        return 'bg-purple-100 text-purple-800'
      case 'PENDING':
      default:
        return 'bg-yellow-100 text-yellow-800'
    }
  }

  // Format status text
  const formatStatusText = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'Completed'
      case 'FAILED':
        return 'Failed'
      case 'IN_PROGRESS':
        return 'In Progress'
      case 'NO_IMAGE':
        return 'No Image'
      case 'PENDING_IMPORT':
        return 'Pending Import'
      case 'PENDING':
      default:
        return 'Pending'
    }
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Floating Status Messages - Fixed at top of viewport */}
      {(successMessage || errorMessage || pasteInstructions) && (
        <div className="fixed top-16 left-0 right-0 z-50 flex justify-center px-4">
          <div className="max-w-4xl w-full">
            {successMessage && (
              <div className="mb-3">
                <SuccessMessage message={successMessage} className="shadow-lg" />
              </div>
            )}
            {errorMessage && (
              <div className="mb-3">
                <ErrorMessage message={errorMessage} className="shadow-lg" />
              </div>
            )}

            {/* Paste Mode Instructions */}
            {pasteInstructions && (
              <div className="mb-3 bg-blue-50 border border-blue-200 rounded-lg p-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <PiUpload className="w-5 h-5 text-blue-600" />
                    <p className="text-blue-900 font-medium">{pasteInstructions}</p>
                  </div>
                  <button
                    onClick={handleCancelPasteMode}
                    className="text-blue-600 hover:text-blue-800 font-medium text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Photos</h1>
        <p className="text-gray-600">
          Sync photos with Google Photos cloud storage
        </p>
      </div>

      {/* Photo Import/Export Status Section */}
      <div
        className="bg-white rounded-lg shadow overflow-hidden mb-6"
        data-test="photo-export-section"
      >
        <div className="bg-green-600 px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            <PiImage className="w-8 h-8" />
            <div>
              <h2 className="text-xl font-bold" data-test="photos-header">
                Photo Import/Export Status
              </h2>
              <p className="text-sm text-green-100">
                Sync photos with Google Photos cloud storage
              </p>
            </div>
          </div>
        </div>

        <div className="p-6">
          {/* Explanation */}
          <div className="mb-6">
            <p className="text-gray-700 mb-3">
              This section shows the import/export status of all photos in the library with
              Google Photos.
            </p>
            <div className="text-sm text-gray-600">
              <p className="font-medium mb-2">How it works:</p>
              <ul className="list-disc list-inside space-y-1 ml-2">
                <li>Photos are exported to Google Photos and assigned a permanent ID</li>
                <li>Photos can be imported from Google Photos using their permanent ID</li>
                <li>An album is created to hold the photos, named after this library</li>
                <li>
                  You can manually trigger import/export for all pending photos or individual
                  photos
                </li>
              </ul>
            </div>
          </div>

          {/* Album Info */}
          <div className="mb-4">
            <p className="text-sm">
              <strong>Google Photos Album:</strong>{' '}
              <span className="font-mono text-blue-600" id="album-name">
                {photoStats?.albumName || '(Not configured)'}
              </span>
            </p>
            <p className="text-xs text-gray-500 mt-1">
              Note: Make sure Google Photos API is configured in Settings.
            </p>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap gap-3 mb-6">
            <Button
              variant="primary"
              onClick={handleExportAllPendingPhotos}
              isLoading={isExportingAllPhotos}
              leftIcon={<PiUpload />}
              data-test="export-all-photos-btn"
            >
              {isExportingAllPhotos && photoExportProgress
                ? photoExportProgress
                : 'Export All Pending Photos'}
            </Button>
            <Button
              variant="secondary"
              onClick={handleImportAllPendingPhotos}
              isLoading={isImportingAllPhotos}
              leftIcon={<PiDownload />}
              data-test="import-all-photos-btn"
              className="bg-green-600 hover:bg-green-700 text-white"
            >
              {isImportingAllPhotos && photoExportProgress
                ? photoExportProgress
                : 'Import All Pending Photos'}
            </Button>
            <Button
              variant="outline"
              onClick={handleRefreshPhotoStatus}
              leftIcon={<PiArrowsClockwise />}
              data-test="refresh-photos-btn"
            >
              Refresh Status
            </Button>
          </div>

          {/* Statistics Cards */}
          <div className="grid grid-cols-2 md:grid-cols-6 gap-3 mb-6" data-test="export-stats">
            <div className="bg-gray-100 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-gray-900" id="stats-total">
                {photoStats?.total ?? 0}
              </div>
              <div className="text-xs text-gray-600">Total Photos</div>
            </div>
            <div className="bg-green-100 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-green-800" id="stats-exported">
                {photoStats?.exported ?? 0}
              </div>
              <div className="text-xs text-green-700">Exported</div>
            </div>
            <div className="bg-blue-100 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-blue-800" id="stats-imported">
                {photoStats?.imported ?? 0}
              </div>
              <div className="text-xs text-blue-700">Imported</div>
            </div>
            <div className="bg-yellow-100 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-yellow-800" id="stats-pending-export">
                {photoStats?.pendingExport ?? 0}
              </div>
              <div className="text-xs text-yellow-700">Pending Export</div>
            </div>
            <div className="bg-gray-200 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-gray-800" id="stats-pending-import">
                {photoStats?.pendingImport ?? 0}
              </div>
              <div className="text-xs text-gray-600">Pending Import</div>
            </div>
            <div className="bg-red-100 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-red-800" id="stats-failed">
                {photoStats?.failed ?? 0}
              </div>
              <div className="text-xs text-red-700">Failed</div>
            </div>
          </div>

          {/* Photo Details Table */}
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
              <h4 className="font-medium text-gray-900">Photo Details</h4>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200" data-test="photos-table">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Photo
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Title/Author
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Exported At
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Permanent ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody
                  className="bg-white divide-y divide-gray-200"
                  data-test="photos-table-body"
                >
                  {isLoadingPhotos ? (
                    <tr>
                      <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                        <div className="flex items-center justify-center gap-3">
                          <div className="animate-spin h-5 w-5 border-2 border-blue-600 border-t-transparent rounded-full"></div>
                          <span>Loading photos...</span>
                        </div>
                      </td>
                    </tr>
                  ) : photoList.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                        No photos in the database
                      </td>
                    </tr>
                  ) : (
                    photoList.map((photo: PhotoExportInfoDto) => (
                      <tr key={photo.id} data-photo-id={photo.id}>
                        {/* Photo Thumbnail */}
                        <td className="px-4 py-3">
                          {photo.id && photo.hasImage ? (
                            <ThrottledThumbnail
                              photoId={photo.id}
                              url={getThumbnailUrl(photo.id, 48, photo.checksum)}
                              alt={`Photo #${photo.id}`}
                              className="w-12 rounded"
                            />
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>

                        {/* Title/Author */}
                        <td className="px-4 py-3">
                          {photo.bookTitle ? (
                            <div>
                              <div className="font-medium text-gray-900">{photo.bookTitle}</div>
                              {photo.bookAuthorName && (
                                <div className="text-sm text-gray-500">{photo.bookAuthorName}</div>
                              )}
                            </div>
                          ) : photo.authorName ? (
                            <div className="font-medium text-gray-900">{photo.authorName}</div>
                          ) : (
                            <span className="text-gray-400">
                              {photo.caption || `Photo #${photo.id}`}
                            </span>
                          )}
                        </td>

                        {/* Status */}
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeClass(photo.exportStatus)}`}
                            title={photo.exportErrorMessage || undefined}
                          >
                            {formatStatusText(photo.exportStatus)}
                          </span>
                        </td>

                        {/* Exported At */}
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {photo.exportedAt ? formatDateTime(photo.exportedAt) : '-'}
                        </td>

                        {/* Permanent ID */}
                        <td className="px-4 py-3">
                          {photo.permanentId ? (
                            <span
                              className="font-mono text-xs text-gray-600"
                              title={photo.permanentId}
                            >
                              {truncate(photo.permanentId, 20)}
                            </span>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>

                        {/* Actions */}
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1">
                            {/* Paste Image button - always show */}
                            <button
                              onClick={() => handleEnterPasteMode(photo.id)}
                              className={`px-2 py-1 text-xs rounded ${
                                pasteModePhotoId === photo.id
                                  ? 'bg-blue-600 text-white'
                                  : 'border border-purple-600 text-purple-600 hover:bg-purple-50'
                              }`}
                              title="Paste image from clipboard"
                              data-test={`paste-photo-${photo.id}`}
                            >
                              <PiUpload className="w-3 h-3 inline" />
                            </button>

                            {/* Export button - show if hasImage && !permanentId */}
                            {photo.hasImage && !photo.permanentId && (
                              <button
                                onClick={() => handleExportSinglePhoto(photo.id)}
                                className="px-2 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700"
                                title="Export to Google Photos"
                              >
                                Export
                              </button>
                            )}

                            {/* Import button - show if permanentId && !hasImage */}
                            {photo.permanentId && !photo.hasImage && (
                              <button
                                onClick={() => handleImportSinglePhoto(photo.id)}
                                className="px-2 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700"
                                title="Import from Google Photos"
                              >
                                Import
                              </button>
                            )}

                            {/* View button - show if permanentId */}
                            {photo.permanentId && (
                              <a
                                href={`https://photos.google.com/lr/photo/${photo.permanentId}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="px-2 py-1 text-xs border border-blue-600 text-blue-600 rounded hover:bg-blue-50"
                                title="View in Google Photos"
                              >
                                <PiEye className="w-3 h-3 inline" />
                              </a>
                            )}

                            {/* Verify button - show if permanentId */}
                            {photo.permanentId && (
                              <button
                                onClick={() => handleVerifyPhoto(photo.id)}
                                className="px-2 py-1 text-xs border border-cyan-600 text-cyan-600 rounded hover:bg-cyan-50"
                                title="Verify permanent ID still works"
                              >
                                <PiCheck className="w-3 h-3 inline" />
                              </button>
                            )}

                            {/* Unlink button - show if permanentId */}
                            {photo.permanentId && (
                              <button
                                onClick={() => handleUnlinkPhoto(photo.id)}
                                className="px-2 py-1 text-xs border border-yellow-600 text-yellow-600 rounded hover:bg-yellow-50"
                                title="Remove permanent ID"
                              >
                                <PiLink className="w-3 h-3 inline" />
                              </button>
                            )}

                            {/* Delete button */}
                            <button
                              onClick={() => handleDeletePhoto(photo.id)}
                              className="px-2 py-1 text-xs border border-red-600 text-red-600 rounded hover:bg-red-50"
                              title="Delete photo"
                            >
                              <PiTrash className="w-3 h-3 inline" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
