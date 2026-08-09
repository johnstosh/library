// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { EmuLookupResultDto } from '@/api/emu-lookup'
import { PiCheckCircle, PiXCircle } from 'react-icons/pi'

interface EmuLookupResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: EmuLookupResultDto[]
}

function AvailabilityBadge({ available }: { available?: boolean }) {
  if (available === undefined) {
    return <span className="text-gray-400">—</span>
  }
  return available ? (
    <PiCheckCircle className="w-5 h-5 text-green-600" />
  ) : (
    <PiXCircle className="w-5 h-5 text-gray-400" />
  )
}

export function EmuLookupResultsModal({ isOpen, onClose, results }: EmuLookupResultsModalProps) {
  const successCount = results.filter((r) => r.success).length
  const failureCount = results.length - successCount

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="EMU Halle Library Availability Lookup Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="close-emu-lookup-results">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="bg-gray-50 rounded-lg p-4">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Total</p>
              <p className="text-2xl font-bold text-gray-900">{results.length}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Found</p>
              <p className="text-2xl font-bold text-green-600">{successCount}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Not Found</p>
              <p className="text-2xl font-bold text-red-600">{failureCount}</p>
            </div>
          </div>
        </div>

        <div className="max-h-96 overflow-y-auto">
          <table className="min-w-full table-fixed divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '40%' }}>
                  Book
                </th>
                <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '13%' }}>
                  Audio
                </th>
                <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '13%' }}>
                  Paper
                </th>
                <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '13%' }}>
                  Ebook
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '21%' }}>
                  Message
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {results.map((result) => (
                <tr key={result.bookId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 overflow-hidden text-sm text-gray-900">
                    <div className="truncate" title={result.matchedTitle}>
                      {result.matchedTitle || `Book #${result.bookId}`}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <AvailabilityBadge available={result.success ? result.audioAvailable : undefined} />
                  </td>
                  <td className="px-4 py-3 text-center">
                    <AvailabilityBadge available={result.success ? result.paperAvailable : undefined} />
                  </td>
                  <td className="px-4 py-3 text-center">
                    <AvailabilityBadge available={result.success ? result.ebookAvailable : undefined} />
                  </td>
                  <td className="px-4 py-3 overflow-hidden text-sm text-gray-600">
                    <div className="truncate" title={result.errorMessage}>
                      {result.success ? '—' : result.errorMessage || 'Not found'}
                    </div>
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
