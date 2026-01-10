// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { PiCheckCircle, PiXCircle } from 'react-icons/pi'
import type { BookFromImageResult } from './BulkActionsToolbar'

interface BookFromImageResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: BookFromImageResult[]
}

export function BookFromImageResultsModal({
  isOpen,
  onClose,
  results,
}: BookFromImageResultsModalProps) {
  const successCount = results.filter((r) => r.success).length
  const failureCount = results.length - successCount

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Book from Image Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="close-book-from-image-results">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        {/* Summary */}
        <div className="bg-gray-50 rounded-lg p-4">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Total</p>
              <p className="text-2xl font-bold text-gray-900">{results.length}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Success</p>
              <p className="text-2xl font-bold text-green-600">{successCount}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Failed</p>
              <p className="text-2xl font-bold text-red-600">{failureCount}</p>
            </div>
          </div>
        </div>

        {/* Results Table */}
        <div className="max-h-96 overflow-y-auto">
          <table className="min-w-full table-fixed divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th
                  className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  style={{ width: '10%' }}
                >
                  Status
                </th>
                <th
                  className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  style={{ width: '15%' }}
                >
                  Book ID
                </th>
                <th
                  className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  style={{ width: '35%' }}
                >
                  Title
                </th>
                <th
                  className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  style={{ width: '40%' }}
                >
                  Message
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {results.map((result) => (
                <tr key={result.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 whitespace-nowrap">
                    {result.success ? (
                      <PiCheckCircle className="w-5 h-5 text-green-600" />
                    ) : (
                      <PiXCircle className="w-5 h-5 text-red-600" />
                    )}
                  </td>
                  <td className="px-4 py-3 overflow-hidden truncate text-sm text-gray-900">
                    {result.id}
                  </td>
                  <td className="px-4 py-3 overflow-hidden truncate text-sm text-gray-900">
                    {result.book?.title || '—'}
                  </td>
                  <td className="px-4 py-3 overflow-hidden truncate text-sm text-gray-600">
                    {result.success ? 'Successfully processed' : result.error || 'Failed'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Modal>
  )
}
