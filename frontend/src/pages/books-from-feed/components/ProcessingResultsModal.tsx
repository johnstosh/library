// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { ProcessResultDto } from '@/api/books-from-feed'
import { PiCheckCircle, PiXCircle, PiWarning } from 'react-icons/pi'

interface ProcessingResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: ProcessResultDto
}

export function ProcessingResultsModal({
  isOpen,
  onClose,
  results,
}: ProcessingResultsModalProps) {
  const successRate =
    results.totalBooks > 0 ? (results.processedCount / results.totalBooks) * 100 : 0

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="AI Processing Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="close-results">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Overall Status */}
        <div className="text-center">
          {results.processedCount === results.totalBooks ? (
            <div className="flex justify-center mb-4">
              <PiCheckCircle className="w-16 h-16 text-green-500" />
            </div>
          ) : results.processedCount > 0 ? (
            <div className="flex justify-center mb-4">
              <PiWarning className="w-16 h-16 text-yellow-500" />
            </div>
          ) : (
            <div className="flex justify-center mb-4">
              <PiXCircle className="w-16 h-16 text-red-500" />
            </div>
          )}

          <h3 className="text-2xl font-bold text-gray-900 mb-2">
            {results.processedCount === results.totalBooks
              ? 'All Books Processed Successfully!'
              : results.processedCount > 0
              ? 'Processing Completed with Some Failures'
              : 'Processing Failed'}
          </h3>

          {results.message && (
            <p className="text-gray-600">{results.message}</p>
          )}
        </div>

        {/* Statistics */}
        <div className="bg-gray-50 rounded-lg p-6">
          <div className="grid grid-cols-3 gap-6">
            <div className="text-center">
              <p className="text-sm font-medium text-gray-500 mb-1">Total Books</p>
              <p className="text-3xl font-bold text-gray-900">{results.totalBooks}</p>
            </div>
            <div className="text-center">
              <p className="text-sm font-medium text-gray-500 mb-1">Successful</p>
              <p className="text-3xl font-bold text-green-600">{results.processedCount}</p>
            </div>
            <div className="text-center">
              <p className="text-sm font-medium text-gray-500 mb-1">Failed</p>
              <p className="text-3xl font-bold text-red-600">{results.failedCount}</p>
            </div>
          </div>

          {/* Success Rate Bar */}
          <div className="mt-6">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Success Rate</span>
              <span className="text-sm font-bold text-gray-900">
                {successRate.toFixed(1)}%
              </span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-3">
              <div
                className={`h-3 rounded-full transition-all duration-500 ${
                  successRate === 100
                    ? 'bg-green-600'
                    : successRate >= 50
                    ? 'bg-yellow-500'
                    : 'bg-red-600'
                }`}
                style={{ width: `${successRate}%` }}
              />
            </div>
          </div>
        </div>

        {/* Error Message */}
        {results.error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <PiXCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-red-900 mb-1">Error Details</p>
                <p className="text-sm text-red-700">{results.error}</p>
              </div>
            </div>
          </div>
        )}

        {/* Next Steps */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="text-sm font-semibold text-blue-900 mb-2">Next Steps</h4>
          <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
            <li>
              Successfully processed books are now available in the Books page
            </li>
            <li>
              Review book details and edit any information as needed
            </li>
            {results.failedCount > 0 && (
              <>
                <li>
                  Failed books remain in the saved books list for retry
                </li>
                <li>
                  You can try processing individual books or upload different photos
                </li>
              </>
            )}
          </ul>
        </div>
      </div>
    </Modal>
  )
}
