// (c) Copyright 2025 by Muczynski
import { useState, useRef, useEffect } from 'react'
import { Button } from '@/components/ui/Button'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import {
  exportJsonData,
  exportPhotos,
  useImportJsonData,
  useDatabaseStats,
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
import { useBranches } from '@/api/branches'
import { getThumbnailUrl, useImportPhotosFromZip, type PhotoZipImportResultDto } from '@/api/photos'
import { ThrottledThumbnail, clearThumbnailQueue } from '@/components/ui/ThrottledThumbnail'
import { formatDateTime, truncate } from '@/utils/formatters'
import {
  PiDownload,
  PiUpload,
  PiDatabase,
  PiImage,
  PiFileArrowDown,
  PiFileArrowUp,
  PiArrowsClockwise,
  PiEye,
  PiCheck,
  PiLink,
  PiTrash,
} from 'react-icons/pi'

export function DataManagementPage() {
  const [isExportingJson, setIsExportingJson] = useState(false)
  const [isExportingPhotos, setIsExportingPhotos] = useState(false)
  const [isExportingAllPhotos, setIsExportingAllPhotos] = useState(false)
  const [isImportingAllPhotos, setIsImportingAllPhotos] = useState(false)
  const [photoExportProgress, setPhotoExportProgress] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [pasteModePhotoId, setPasteModePhotoId] = useState<number | null>(null)
  const [pasteInstructions, setPasteInstructions] = useState<string>('')
  const [zipImportResult, setZipImportResult] = useState<PhotoZipImportResultDto | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)
  const zipFileInputRef = useRef<HTMLInputElement>(null)
  const importJsonData = useImportJsonData()
  const importPhotosFromZip = useImportPhotosFromZip()

  // Fetch database statistics (total counts from backend)
  const { data: dbStats } = useDatabaseStats()
  // Fetch libraries for filename generation (this is a small list)
  const { data: libraries = [] } = useBranches()

  // Photo export hooks
  const { data: photoStats, refetch: refetchPhotoStats } = usePhotoExportStats()
  const { data: photoList = [], refetch: refetchPhotoList, isLoading: isLoadingPhotos } = usePhotoExportList()
  const exportSinglePhoto = useExportSinglePhoto()
  const importSinglePhoto = useImportSinglePhoto()
  const verifyPhoto = useVerifyPhoto()
  const unlinkPhoto = useUnlinkPhoto()
  const deletePhoto = useDeletePhoto()
  const uploadPhotoImage = useUploadPhotoImage()

  // Clear thumbnail loading queue when leaving the page
  useEffect(() => {
    return () => {
      clearThumbnailQueue()
    }
  }, [])

  const handleExportJson = async () => {
    setIsExportingJson(true)
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const blob = await exportJsonData()

      // Generate filename with statistics from database
      let libraryName = 'library'
      if (libraries.length > 0) {
        // Sanitize library branch name for use as filename
        libraryName = libraries[0].branchName
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-+|-+$/g, '')
      }

      const bookCount = dbStats?.bookCount ?? 0
      const authorCount = dbStats?.authorCount ?? 0
      const userCount = dbStats?.userCount ?? 0
      const loanCount = dbStats?.loanCount ?? 0
      const photoCount = photoStats?.total ?? 0
      const date = new Date().toISOString().split('T')[0]

      const filename = `${libraryName}-${bookCount}-books-${authorCount}-authors-${userCount}-users-${loanCount}-loans-${photoCount}-photos-${date}.json`

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)

      setSuccessMessage('JSON export downloaded successfully')
    } catch (error) {
      console.error('Failed to export JSON:', error)
      setErrorMessage('Failed to export JSON data. Please try again.')
    } finally {
      setIsExportingJson(false)
    }
  }

  const handleExportPhotos = async () => {
    setIsExportingPhotos(true)
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const blob = await exportPhotos()

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      // Generate filename: 2026-01-25-library-photos-<branchname>.zip
      let branchName = 'branch'
      if (libraries.length > 0) {
        branchName = libraries[0].branchName
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-+|-+$/g, '')
      }
      const date = new Date().toISOString().split('T')[0]
      a.download = `${date}-library-photos-${branchName}.zip`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)

      setSuccessMessage('Photo export downloaded successfully')
    } catch (error) {
      console.error('Failed to export photos:', error)
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Photo export failed: ${errorMessage}`)
    } finally {
      setIsExportingPhotos(false)
    }
  }

  const handleImportClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setSuccessMessage('')
    setErrorMessage('')

    try {
      await importJsonData.mutateAsync(file)
      setSuccessMessage('JSON import completed successfully')

      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (error) {
      console.error('Failed to import JSON:', error)
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to import JSON data: ${errorMessage}`)
    }
  }

  const handleZipImportClick = () => {
    zipFileInputRef.current?.click()
  }

  const handleZipFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setSuccessMessage('')
    setErrorMessage('')
    setZipImportResult(null)

    try {
      const result = await importPhotosFromZip.mutateAsync(file)
      setZipImportResult(result)

      if (result.failureCount === 0 && result.successCount > 0) {
        setSuccessMessage(
          `Successfully imported ${result.successCount} photo(s)${result.skippedCount > 0 ? `, ${result.skippedCount} skipped` : ''}`
        )
      } else if (result.failureCount > 0) {
        setErrorMessage(
          `Import completed: ${result.successCount} succeeded, ${result.failureCount} failed, ${result.skippedCount} skipped`
        )
      } else {
        setSuccessMessage(`Import completed: ${result.skippedCount} files skipped (no photos imported)`)
      }

      // Reset file input
      if (zipFileInputRef.current) {
        zipFileInputRef.current.value = ''
      }

      // Refresh photo status
      handleRefreshPhotoStatus()
    } catch (error) {
      console.error('Failed to import photos from ZIP:', error)
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to import photos from ZIP: ${errorMessage}`)
    }
  }

  // Photo Export Handlers
  const handleRefreshPhotoStatus = () => {
    refetchPhotoStats()
    refetchPhotoList()
  }

  const handleExportAllPendingPhotos = async () => {
    // Filter to get only pending export photos (hasImage && !permanentId)
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
        // Capture error details for debugging
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
      // Log all failed photos for debugging
      console.error('Failed photo exports:', failedPhotos)
      // Show summary of failures
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
    // Filter to get only pending import photos (permanentId && !hasImage)
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
        // Capture error details for debugging
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
      // Log all failed photos for debugging
      console.error('Failed photo imports:', failedPhotos)
      // Show summary of failures
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
      // Refresh data to update the row status
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
      // Refresh data to update the row status
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
      // Refresh data to update the row status
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
      // Refresh data to update the table
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

    // Focus on window to ensure paste event is captured
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
            {/* Success/Error Messages */}
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
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Data Management</h1>
        <p className="text-gray-600">
          Export and import library data for backup or migration
        </p>
      </div>

      {/* JSON Export/Import Section */}
      <div className="bg-white rounded-lg shadow overflow-hidden mb-6">
        <div className="bg-blue-600 px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            <PiDatabase className="w-8 h-8" />
            <div>
              <h2 className="text-xl font-bold">Database Export/Import</h2>
              <p className="text-sm text-blue-100">
                Export all library data to JSON or import from a backup file
              </p>
            </div>
          </div>
        </div>

        {/* Database Statistics */}
        <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
          <div className="grid grid-cols-5 gap-4 text-center">
            <div data-test="stat-libraries">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.libraryCount ?? 0}</div>
              <div className="text-sm text-gray-600">Libraries</div>
            </div>
            <div data-test="stat-books">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.bookCount ?? 0}</div>
              <div className="text-sm text-gray-600">Books</div>
            </div>
            <div data-test="stat-authors">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.authorCount ?? 0}</div>
              <div className="text-sm text-gray-600">Authors</div>
            </div>
            <div data-test="stat-users">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.userCount ?? 0}</div>
              <div className="text-sm text-gray-600">Users</div>
            </div>
            <div data-test="stat-loans">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.loanCount ?? 0}</div>
              <div className="text-sm text-gray-600">Loans</div>
            </div>
          </div>
        </div>

        <div className="p-6 grid md:grid-cols-2 gap-6">
          {/* Export JSON */}
          <div className="border border-gray-200 rounded-lg p-6">
            <div className="flex items-start gap-3 mb-4">
              <PiFileArrowDown className="w-6 h-6 text-blue-600 mt-1" />
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Export Data
                </h3>
                <p className="text-sm text-gray-600 mb-4">
                  Download all library data as a JSON file. Includes libraries, authors,
                  books, users, and loans. Photos are excluded.
                </p>
                <Button
                  variant="primary"
                  onClick={handleExportJson}
                  isLoading={isExportingJson}
                  leftIcon={<PiDownload />}
                  data-test="export-json"
                >
                  Export JSON
                </Button>
              </div>
            </div>
          </div>

          {/* Import JSON */}
          <div className="border border-gray-200 rounded-lg p-6">
            <div className="flex items-start gap-3 mb-4">
              <PiFileArrowUp className="w-6 h-6 text-green-600 mt-1" />
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Import Data
                </h3>
                <p className="text-sm text-gray-600 mb-4">
                  Upload a JSON export file to import data. This will merge with existing
                  data (does not delete existing records).
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".json"
                  onChange={handleFileChange}
                  className="hidden"
                  data-test="import-json-input"
                />
                <Button
                  variant="secondary"
                  onClick={handleImportClick}
                  isLoading={importJsonData.isPending}
                  leftIcon={<PiUpload />}
                  data-test="import-json"
                >
                  Import JSON
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Photo Export Section */}
      <div className="bg-white rounded-lg shadow overflow-hidden mb-6">
        <div className="bg-purple-600 px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            <PiImage className="w-8 h-8" />
            <div>
              <h2 className="text-xl font-bold">Photo Export/Import</h2>
              <p className="text-sm text-purple-100">
                Export or import book, author, and loan photos as a ZIP file
              </p>
            </div>
          </div>
        </div>

        <div className="p-6 grid md:grid-cols-2 gap-6">
          {/* Export Photos */}
          <div className="border border-gray-200 rounded-lg p-6">
            <div className="flex items-start gap-3">
              <PiFileArrowDown className="w-6 h-6 text-purple-600 mt-1" />
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Export Photos
                </h3>
                <p className="text-sm text-gray-600 mb-4">
                  Download all photos stored in Google Photos as a ZIP archive. This is
                  separate from the JSON export and can be used for complete backup.
                </p>
                <Button
                  variant="primary"
                  onClick={handleExportPhotos}
                  isLoading={isExportingPhotos}
                  leftIcon={<PiDownload />}
                  data-test="export-photos"
                >
                  Export Photos
                </Button>
              </div>
            </div>
          </div>

          {/* Import Photos from ZIP */}
          <div className="border border-gray-200 rounded-lg p-6">
            <div className="flex items-start gap-3">
              <PiFileArrowUp className="w-6 h-6 text-green-600 mt-1" />
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Import Photos from ZIP
                </h3>
                <p className="text-sm text-gray-600 mb-4">
                  Upload a ZIP file with photos to import. Filenames must follow the format:
                </p>
                <ul className="text-xs text-gray-500 mb-4 space-y-1">
                  <li><code className="bg-gray-100 px-1 rounded">book-{'{title}'}.jpg</code> - Book photo</li>
                  <li><code className="bg-gray-100 px-1 rounded">author-{'{name}'}.jpg</code> - Author photo</li>
                  <li><code className="bg-gray-100 px-1 rounded">loan-{'{title}'}-{'{username}'}.jpg</code> - Loan photo</li>
                </ul>
                <input
                  ref={zipFileInputRef}
                  type="file"
                  accept=".zip"
                  onChange={handleZipFileChange}
                  className="hidden"
                  data-test="import-zip-input"
                />
                <Button
                  variant="secondary"
                  onClick={handleZipImportClick}
                  isLoading={importPhotosFromZip.isPending}
                  leftIcon={<PiUpload />}
                  data-test="import-photos-zip"
                >
                  Import Photos ZIP
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* ZIP Import Results */}
        {zipImportResult && (
          <div className="px-6 pb-6">
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
                <h4 className="font-medium text-gray-900">
                  Import Results: {zipImportResult.totalFiles} file(s) processed
                </h4>
              </div>
              <div className="max-h-64 overflow-y-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50 sticky top-0">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Filename</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Entity</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {zipImportResult.items.map((item, index) => (
                      <tr key={index}>
                        <td className="px-4 py-2 text-sm font-mono text-gray-900">{item.filename}</td>
                        <td className="px-4 py-2">
                          <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                            item.status === 'SUCCESS' ? 'bg-green-100 text-green-800' :
                            item.status === 'FAILURE' ? 'bg-red-100 text-red-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {item.status}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-sm text-gray-600">
                          {item.entityType && `${item.entityType}: ${item.entityName || '-'}`}
                        </td>
                        <td className="px-4 py-2 text-sm text-gray-500">
                          {item.errorMessage || (item.photoId ? `Photo #${item.photoId}` : '-')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
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
                              url={getThumbnailUrl(photo.id, 48)}
                              alt={`Photo #${photo.id}`}
                              className="w-12 rounded"
                              checksum={photo.checksum}
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

      {/* Important Notes */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
        <h3 className="text-sm font-medium text-yellow-900 mb-3">Important Notes:</h3>
        <ul className="text-sm text-yellow-800 space-y-2 list-disc list-inside">
          <li>
            <strong>JSON Export does NOT include photos</strong> - Use Photo Export
            separately for a complete backup
          </li>
          <li>
            <strong>Import merges data</strong> - It does not delete existing records,
            only adds or updates
          </li>
          <li>
            <strong>Photo Export downloads from Google Photos</strong> - This may take
            some time depending on the number of photos
          </li>
          <li>
            <strong>Regular backups recommended</strong> - Export data periodically to
            prevent data loss
          </li>
        </ul>
      </div>
    </div>
  )
}
