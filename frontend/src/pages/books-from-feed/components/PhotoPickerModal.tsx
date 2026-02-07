// (c) Copyright 2025 by Muczynski
import { useState, useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { Spinner } from '@/components/progress/Spinner'
import { ProgressBar } from '@/components/progress/ProgressBar'
import {
  useCreatePickerSession,
  usePickerSessionStatus,
  usePickerMediaItems,
  saveSinglePhotoFromPickerApi,
  type SaveSingleFromPickerResult,
} from '@/api/books-from-feed'
import { ApiError } from '@/api/client'
import { PiCheckCircle, PiWarningCircle } from 'react-icons/pi'

interface PhotoPickerModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (count: number) => void
}

interface FailedPhoto {
  name: string
  error: string
}

type Step =
  | 'idle'
  | 'creating-session'
  | 'waiting-for-selection'
  | 'fetching-photos'
  | 'saving'
  | 'complete'
  | 'complete-with-errors'

function isRetryableError(error: string | undefined): boolean {
  if (!error) return true
  const lower = error.toLowerCase()
  // Don't retry on client errors that won't self-resolve
  if (lower.includes('not found') || lower.includes('unauthorized') || lower.includes('forbidden')) {
    return false
  }
  return true
}

async function saveWithRetry(
  photo: Record<string, unknown>,
  maxRetries: number,
  onRetry?: (attempt: number) => void
): Promise<SaveSingleFromPickerResult> {
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const result = await saveSinglePhotoFromPickerApi(photo)
      // Success or skipped — return immediately
      if (result.success || result.skipped) return result
      // Backend returned failure — retry only if transient
      if (attempt === maxRetries || !isRetryableError(result.error)) return result
      onRetry?.(attempt + 1)
    } catch (err) {
      // Don't retry on 4xx client errors
      if (err instanceof ApiError && err.status >= 400 && err.status < 500) {
        return {
          success: false,
          skipped: false,
          photoName: photo.name as string,
          error: err.message,
        }
      }
      if (attempt === maxRetries) {
        return {
          success: false,
          skipped: false,
          photoName: photo.name as string,
          error: err instanceof Error ? err.message : 'Unknown error',
        }
      }
      onRetry?.(attempt + 1)
    }
    // Exponential backoff: 1s, 2s, 4s, 8s, 16s, then capped at 30s (~3 min total for 10 retries)
    const delay = Math.min(1000 * Math.pow(2, attempt), 30000)
    await new Promise(resolve => setTimeout(resolve, delay))
  }
  // Should not reach here, but TypeScript needs it
  return { success: false, skipped: false, error: 'Max retries exceeded' }
}

export function PhotoPickerModal({ isOpen, onClose, onSuccess }: PhotoPickerModalProps) {
  const [step, setStep] = useState<Step>('idle')
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [pickerWindow, setPickerWindow] = useState<Window | null>(null)

  // Per-photo progress state
  const [saveProgress, setSaveProgress] = useState({ current: 0, total: 0 })
  const [currentPhotoName, setCurrentPhotoName] = useState('')
  const [retryAttempt, setRetryAttempt] = useState(0)
  const [savedCount, setSavedCount] = useState(0)
  const [failedPhotos, setFailedPhotos] = useState<FailedPhoto[]>([])
  const cancelRef = useRef(false)

  const queryClient = useQueryClient()
  const createSession = useCreatePickerSession()
  const { data: sessionStatus } = usePickerSessionStatus(sessionId)
  const mediaItemsReady = sessionStatus?.mediaItemsSet === true
  const { data: mediaItemsData } = usePickerMediaItems(sessionId, mediaItemsReady)

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      setStep('idle')
      setSessionId(null)
      setError('')
      setPickerWindow(null)
      setSaveProgress({ current: 0, total: 0 })
      setCurrentPhotoName('')
      setRetryAttempt(0)
      setSavedCount(0)
      setFailedPhotos([])
      cancelRef.current = false
    }
  }, [isOpen])

  // Handle session status updates - when user finishes selecting photos
  useEffect(() => {
    if (sessionStatus?.mediaItemsSet && step === 'waiting-for-selection') {
      if (pickerWindow && !pickerWindow.closed) {
        pickerWindow.close()
      }
      setStep('fetching-photos')
    }
  }, [sessionStatus, step, pickerWindow])

  // Handle fetched media items - transform and start per-photo saving
  useEffect(() => {
    if (mediaItemsData && step === 'fetching-photos') {
      const transformedPhotos = mediaItemsData.mediaItems.map((item: any) => ({
        id: item.id,
        name: item.mediaFile?.filename || item.id,
        url: item.mediaFile?.baseUrl,
        thumbnailUrl: item.mediaFile?.baseUrl,
        description: '',
        mimeType: item.mediaFile?.mimeType,
        lastEditedUtc: item.createTime || new Date().toISOString(),
      }))
      handleSavePhotos(transformedPhotos)
    }
  }, [mediaItemsData, step])

  const handleOpenPicker = async () => {
    setError('')
    setStep('creating-session')

    try {
      const session = await createSession.mutateAsync()
      setSessionId(session.id)

      const width = 800
      const height = 600
      const left = (window.screen.width - width) / 2
      const top = (window.screen.height - height) / 2

      const pickerUriWithAutoClose = session.pickerUri + '/autoclose'

      const popup = window.open(
        pickerUriWithAutoClose,
        'GooglePhotosPicker',
        `width=${width},height=${height},left=${left},top=${top},toolbar=no,location=no,menubar=no`
      )

      if (!popup) {
        setError('Failed to open picker window. Please allow popups for this site.')
        setStep('idle')
        return
      }

      setPickerWindow(popup)
      setStep('waiting-for-selection')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create picker session')
      setStep('idle')
    }
  }

  const handleSavePhotos = async (photos: Record<string, unknown>[]) => {
    setStep('saving')
    setError('')
    cancelRef.current = false
    setSaveProgress({ current: 0, total: photos.length })
    setSavedCount(0)
    setFailedPhotos([])

    let saved = 0
    const failed: FailedPhoto[] = []

    for (let i = 0; i < photos.length; i++) {
      if (cancelRef.current) break

      const photo = photos[i]
      const photoName = (photo.name as string) || `Photo ${i + 1}`
      setSaveProgress({ current: i + 1, total: photos.length })
      setCurrentPhotoName(photoName)
      setRetryAttempt(0)

      const result = await saveWithRetry(photo, 10, (attempt) => {
        setRetryAttempt(attempt)
      })

      if (result.success) {
        saved++
        setSavedCount(saved)
      } else if (!result.skipped) {
        failed.push({ name: photoName, error: result.error || 'Unknown error' })
        setFailedPhotos([...failed])
      }
    }

    // Invalidate queries to refresh saved books list
    queryClient.invalidateQueries({ queryKey: ['books-from-feed', 'saved-books'] })
    queryClient.invalidateQueries({ queryKey: ['books'] })

    if (cancelRef.current) {
      // User cancelled — show what we accomplished so far
      if (failed.length > 0) {
        setStep('complete-with-errors')
      } else {
        setStep('complete')
        setTimeout(() => {
          onSuccess(saved)
          onClose()
        }, 2000)
      }
    } else if (failed.length > 0) {
      setStep('complete-with-errors')
    } else {
      setStep('complete')
      setTimeout(() => {
        onSuccess(saved)
        onClose()
      }, 2000)
    }
  }

  const handleCancel = () => {
    if (step === 'saving') {
      cancelRef.current = true
    } else {
      onClose()
    }
  }

  const handleCloseWithErrors = () => {
    onSuccess(savedCount)
    onClose()
  }

  const getStepMessage = () => {
    switch (step) {
      case 'creating-session':
        return 'Creating Google Photos Picker session...'
      case 'waiting-for-selection':
        return 'Waiting for photo selection in popup window...'
      case 'fetching-photos':
        return 'Fetching selected photos...'
      case 'saving': {
        const base = `Saving photo ${saveProgress.current} of ${saveProgress.total}...`
        const file = currentPhotoName ? ` (${currentPhotoName})` : ''
        const retry = retryAttempt > 0 ? ` (retry ${retryAttempt}/10)` : ''
        return base + file + retry
      }
      case 'complete':
        return `${savedCount} photo${savedCount !== 1 ? 's' : ''} saved successfully!`
      case 'complete-with-errors':
        return `Completed: ${savedCount} saved, ${failedPhotos.length} failed`
      default:
        return ''
    }
  }

  const isProcessing = step !== 'idle' && step !== 'complete' && step !== 'complete-with-errors'

  return (
    <Modal
      isOpen={isOpen}
      onClose={isProcessing ? () => {} : onClose}
      title="Import from Google Photos"
      size="md"
      footer={
        <div className="flex justify-end gap-3">
          {step === 'complete-with-errors' ? (
            <Button
              variant="primary"
              onClick={handleCloseWithErrors}
              data-test="photo-picker-close-with-errors"
            >
              Close
            </Button>
          ) : (
            <>
              <Button
                variant="ghost"
                onClick={handleCancel}
                disabled={step === 'creating-session' || step === 'fetching-photos'}
                data-test="photo-picker-cancel"
              >
                {step === 'complete' ? 'Close' : step === 'saving' ? 'Cancel Remaining' : 'Cancel'}
              </Button>
              {step === 'idle' && (
                <Button
                  variant="primary"
                  onClick={handleOpenPicker}
                  data-test="open-google-photos-picker"
                >
                  Open Google Photos Picker
                </Button>
              )}
            </>
          )}
        </div>
      }
    >
      <div className="space-y-4">
        {error && <ErrorMessage message={error} />}

        {step === 'idle' && !error && (
          <div className="text-center py-8">
            <p className="text-gray-600 mb-4">
              Click the button below to open Google Photos Picker and select book cover images.
            </p>
            <p className="text-sm text-gray-500">
              You can select multiple photos at once. The picker will open in a popup window.
            </p>
          </div>
        )}

        {isProcessing && (
          <div className="space-y-4">
            {step !== 'saving' && (
              <div className="flex items-center justify-center py-4">
                <Spinner size="lg" />
              </div>
            )}
            <p className="text-center text-gray-700">{getStepMessage()}</p>
            {step === 'saving' && saveProgress.total > 0 && (
              <ProgressBar value={saveProgress.current} max={saveProgress.total} />
            )}
          </div>
        )}

        {step === 'complete' && (
          <div className="text-center py-8">
            <div className="flex justify-center mb-4">
              <PiCheckCircle className="w-16 h-16 text-green-500" />
            </div>
            <p className="text-lg font-medium text-gray-900 mb-2">Success!</p>
            <p className="text-gray-600">{getStepMessage()}</p>
          </div>
        )}

        {step === 'complete-with-errors' && (
          <div className="py-4">
            <div className="flex justify-center mb-4">
              <PiWarningCircle className="w-16 h-16 text-amber-500" />
            </div>
            <p className="text-lg font-medium text-gray-900 mb-2 text-center">{getStepMessage()}</p>
            <div className="mt-4 bg-red-50 border border-red-200 rounded-lg p-4">
              <p className="text-sm font-medium text-red-800 mb-2">Failed photos:</p>
              <ul className="text-sm text-red-700 space-y-1">
                {failedPhotos.map((fp, i) => (
                  <li key={i}>
                    <strong>{fp.name}</strong>: {fp.error}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}

        {step === 'waiting-for-selection' && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mt-4">
            <p className="text-sm text-blue-800">
              <strong>Note:</strong> If the picker window doesn't appear, please check if your
              browser is blocking popups and allow them for this site.
            </p>
          </div>
        )}
      </div>
    </Modal>
  )
}
