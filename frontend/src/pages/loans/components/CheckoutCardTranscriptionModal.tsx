// (c) Copyright 2025 by Muczynski
import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useTranscribeCheckoutCard } from '@/api/loans'
import type { CheckoutCardTranscriptionDto } from '@/types/dtos'

interface CheckoutCardTranscriptionModalProps {
  isOpen: boolean
  onClose: () => void
  onTranscriptionComplete?: (result: CheckoutCardTranscriptionDto) => void
  captureMode: 'file' | 'camera'
}

export function CheckoutCardTranscriptionModal({
  isOpen,
  onClose,
  onTranscriptionComplete,
  captureMode,
}: CheckoutCardTranscriptionModalProps) {
  const navigate = useNavigate()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const transcribeMutation = useTranscribeCheckoutCard()

  if (!isOpen) return null

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      const url = URL.createObjectURL(file)
      setPreviewUrl(url)
    }
  }

  const handleTranscribe = async () => {
    if (!selectedFile) return

    try {
      const result = await transcribeMutation.mutateAsync(selectedFile)

      if (onTranscriptionComplete) {
        onTranscriptionComplete(result)
      }

      // Build query params for pre-filling the loan form
      const params = new URLSearchParams()
      if (result.title && result.title !== 'N/A') {
        params.set('title', result.title)
      }
      if (result.author && result.author !== 'N/A') {
        params.set('author', result.author)
      }
      if (result.callNumber && result.callNumber !== 'N/A') {
        params.set('locNumber', result.callNumber)
      }
      if (result.lastIssuedTo && result.lastIssuedTo !== 'N/A') {
        params.set('borrower', result.lastIssuedTo)
      }
      if (result.lastDate && result.lastDate !== 'N/A') {
        params.set('checkoutDate', result.lastDate)
      }
      if (result.lastDue && result.lastDue !== 'N/A') {
        params.set('dueDate', result.lastDue)
      }

      // Close modal and navigate to checkout form with pre-filled data
      handleClose()
      navigate(`/loans/new?${params.toString()}`)
    } catch (error) {
      console.error('Transcription failed:', error)
    }
  }

  const handleClose = () => {
    setSelectedFile(null)
    setPreviewUrl(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
    onClose()
  }

  const getAccept = () => {
    return captureMode === 'camera' ? 'image/*' : 'image/*'
  }

  const getCapture = () => {
    return captureMode === 'camera' ? 'environment' : undefined
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <h2 className="text-2xl font-bold mb-4">
            {captureMode === 'camera' ? 'Checkout by Camera' : 'Checkout by Photo'}
          </h2>

          {/* File input */}
          <div className="mb-4">
            <input
              ref={fileInputRef}
              type="file"
              accept={getAccept()}
              capture={getCapture()}
              onChange={handleFileSelect}
              className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
              data-test="checkout-card-photo-input"
            />
          </div>

          {/* Image preview */}
          {previewUrl && (
            <div className="mb-4">
              <img
                src={previewUrl}
                alt="Checkout card preview"
                className="max-w-full h-auto rounded border border-gray-300"
              />
            </div>
          )}

          {/* Error message */}
          {transcribeMutation.isError && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded">
              <p className="text-red-700">
                {transcribeMutation.error instanceof Error
                  ? transcribeMutation.error.message
                  : 'Failed to transcribe checkout card'}
              </p>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={handleClose} data-test="close-transcription-modal">
              Cancel
            </Button>
            {selectedFile && (
              <Button
                variant="primary"
                onClick={handleTranscribe}
                disabled={transcribeMutation.isPending}
                data-test="transcribe-button"
              >
                {transcribeMutation.isPending ? 'Transcribing...' : 'Transcribe'}
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
