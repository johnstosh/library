// (c) Copyright 2025 by Muczynski
import { Modal } from './Modal'
import { Button } from './Button'
import { ErrorMessage } from './ErrorMessage'

export interface ConfirmDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  error?: string
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'primary'
  isLoading?: boolean
  confirmDataTest?: string
  cancelDataTest?: string
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  error,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant = 'primary',
  isLoading = false,
  confirmDataTest = 'confirm-dialog-confirm',
  cancelDataTest = 'confirm-dialog-cancel',
}: ConfirmDialogProps) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      size="sm"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={isLoading} data-test={cancelDataTest}>
            {cancelText}
          </Button>
          <Button
            variant={variant}
            onClick={onConfirm}
            isLoading={isLoading}
            data-test={confirmDataTest}
          >
            {confirmText}
          </Button>
        </div>
      }
    >
      <p className="text-gray-700">{message}</p>
      {error && (
        <ErrorMessage message={error} className="mt-4" data-test="confirm-dialog-error" />
      )}
    </Modal>
  )
}
