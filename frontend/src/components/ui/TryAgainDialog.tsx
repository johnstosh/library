// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Modal } from './Modal'
import { Button } from './Button'
import { ErrorMessage } from './ErrorMessage'
import { isTransientApiError, getTransientErrorMessage } from '@/utils/api'

export interface TryAgainDialogProps {
  isOpen: boolean
  onClose: () => void
  onTryAgain: () => void
  error: unknown
  title?: string
  isRetrying?: boolean
  'data-test'?: string
}

/**
 * Dialog for transient mutation/save errors (B in Issue #319).
 * Allows "Try again" without losing form draft data. Never does full page refresh.
 * For non-transient errors, shows the error but still allows dismiss.
 */
export function TryAgainDialog({
  isOpen,
  onClose,
  onTryAgain,
  error,
  title = 'Save failed',
  isRetrying = false,
  'data-test': dataTest = 'try-again-dialog',
}: TryAgainDialogProps) {
  const [localError, setLocalError] = useState<string | null>(null)

  const isTransient = isTransientApiError(error)
  const message = getTransientErrorMessage(error)

  const handleTryAgain = () => {
    setLocalError(null)
    onTryAgain()
  }

  const handleClose = () => {
    setLocalError(null)
    onClose()
  }

  if (!isOpen) return null

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={title}
      size="sm"
      footer={
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={handleClose}
            disabled={isRetrying}
            data-test="try-again-dismiss"
          >
            Dismiss
          </Button>
          <Button
            variant="primary"
            onClick={handleTryAgain}
            isLoading={isRetrying}
            data-test="try-again-button"
          >
            Try Again
          </Button>
        </div>
      }
    >
      <div className="text-gray-700">
        {isTransient ? (
          <p>{message}</p>
        ) : (
          <p>The operation failed. {message}</p>
        )}
      </div>

      {(localError || (typeof error === 'string' ? error : error instanceof Error ? error.message : null)) && (
        <ErrorMessage 
          message={localError || (error instanceof Error ? error.message : String(error))} 
          className="mt-4" 
          data-test="try-again-error"
        />
      )}
    </Modal>
  )
}
