// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { Spinner } from '@/components/progress/Spinner'
import { ProgressBar } from '@/components/progress/ProgressBar'
import {
  useCreatePickerSession,
  usePickerSessionStatus,
  usePickerMediaItems,
  useSavePhotosFromPicker,
} from '@/api/books-from-feed'
import { PiCheckCircle } from 'react-icons/pi'

interface PhotoPickerModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (count: number) => void
}

type Step = 'idle' | 'creating-session' | 'waiting-for-selection' | 'fetching-photos' | 'saving' | 'complete'

export function PhotoPickerModal({ isOpen, onClose, onSuccess }: PhotoPickerModalProps) {
  const [step, setStep] = useState<Step>('idle')
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [pickerWindow, setPickerWindow] = useState<Window | null>(null)

  const createSession = useCreatePickerSession()
  const { data: sessionStatus } = usePickerSessionStatus(sessionId)
  const { data: mediaItemsData, refetch: refetchMediaItems } = usePickerMediaItems(sessionId)
  const savePhotos = useSavePhotosFromPicker()

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      setStep('idle')
      setSessionId(null)
      setError('')
      setPickerWindow(null)
    }
  }, [isOpen])

  // Handle session status updates
  useEffect(() => {
    if (sessionStatus?.mediaItemsSet && step === 'waiting-for-selection') {
      // Close the picker window if it's still open
      if (pickerWindow && !pickerWindow.closed) {
        pickerWindow.close()
      }
      setStep('fetching-photos')
      refetchMediaItems()
    }
  }, [sessionStatus, step, pickerWindow, refetchMediaItems])

  // Handle fetched media items
  useEffect(() => {
    if (mediaItemsData && step === 'fetching-photos') {
      handleSavePhotos(mediaItemsData.mediaItems)
    }
  }, [mediaItemsData, step])

  const handleOpenPicker = async () => {
    setError('')
    setStep('creating-session')

    try {
      const session = await createSession.mutateAsync()
      setSessionId(session.id)

      // Open Google Photos Picker in a popup window
      const width = 800
      const height = 600
      const left = (window.screen.width - width) / 2
      const top = (window.screen.height - height) / 2

      const popup = window.open(
        session.pickerUri,
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

  const handleSavePhotos = async (photos: any[]) => {
    setStep('saving')
    setError('')

    try {
      const result = await savePhotos.mutateAsync(photos)

      if (result.error) {
        setError(result.error)
        setStep('idle')
      } else {
        setStep('complete')
        setTimeout(() => {
          onSuccess(result.savedCount)
          onClose()
        }, 2000)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save photos')
      setStep('idle')
    }
  }

  const getStepMessage = () => {
    switch (step) {
      case 'creating-session':
        return 'Creating Google Photos Picker session...'
      case 'waiting-for-selection':
        return 'Waiting for photo selection in popup window...'
      case 'fetching-photos':
        return 'Fetching selected photos...'
      case 'saving':
        return 'Saving photos to database...'
      case 'complete':
        return 'Photos saved successfully!'
      default:
        return ''
    }
  }

  const isProcessing = step !== 'idle' && step !== 'complete'

  return (
    <Modal
      isOpen={isOpen}
      onClose={isProcessing ? () => {} : onClose}
      title="Import from Google Photos"
      size="md"
      footer={
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={onClose}
            disabled={isProcessing}
            data-test="photo-picker-cancel"
          >
            {step === 'complete' ? 'Close' : 'Cancel'}
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
            <div className="flex items-center justify-center py-4">
              <Spinner size="lg" />
            </div>
            <p className="text-center text-gray-700">{getStepMessage()}</p>
            {step === 'saving' && <ProgressBar value={50} showLabel={false} />}
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
