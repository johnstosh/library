// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { FreeTextLookupResultDto } from '@/api/free-text-lookup'
import { PiCheckCircle, PiXCircle, PiCaretDown, PiCaretRight } from 'react-icons/pi'
import { useState } from 'react'

interface FreeTextLookupResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: FreeTextLookupResultDto[]
}

export function FreeTextLookupResultsModal({
  isOpen,
  onClose,
  results,
}: FreeTextLookupResultsModalProps) {
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set())

  const successCount = results.filter((r) => r.success).length
  const failureCount = results.length - successCount

  const toggleRow = (bookId: number) => {
    const newExpanded = new Set(expandedRows)
    if (newExpanded.has(bookId)) {
      newExpanded.delete(bookId)
    } else {
      newExpanded.add(bookId)
    }
    setExpandedRows(newExpanded)
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Free Online Text Lookup Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="close-free-text-results">
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
              <p className="text-sm font-medium text-gray-500">Found</p>
              <p className="text-2xl font-bold text-green-600">{successCount}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Not Found</p>
              <p className="text-2xl font-bold text-red-600">{failureCount}</p>
            </div>
          </div>
        </div>

        {/* Results Table */}
        <div className="max-h-96 overflow-y-auto">
          <table className="min-w-full table-fixed divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th className="px-2 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '5%' }}>
                  {/* Expand */}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '8%' }}>
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '25%' }}>
                  Book Title
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '17%' }}>
                  Provider
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '45%' }}>
                  URL / Message
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {results.map((result) => (
                <>
                  <tr key={result.bookId} className="hover:bg-gray-50">
                    <td className="px-2 py-3 whitespace-nowrap">
                      {result.providersSearched.length > 0 && (
                        <button
                          onClick={() => toggleRow(result.bookId)}
                          className="text-gray-400 hover:text-gray-600"
                          data-test={`expand-providers-${result.bookId}`}
                        >
                          {expandedRows.has(result.bookId) ? (
                            <PiCaretDown className="w-4 h-4" />
                          ) : (
                            <PiCaretRight className="w-4 h-4" />
                          )}
                        </button>
                      )}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      {result.success ? (
                        <PiCheckCircle className="w-5 h-5 text-green-600" />
                      ) : (
                        <PiXCircle className="w-5 h-5 text-red-600" />
                      )}
                    </td>
                    <td className="px-4 py-3 overflow-hidden text-sm text-gray-900">
                      <div className="truncate" title={result.bookTitle}>
                        {result.bookTitle}
                      </div>
                      {result.authorName && (
                        <div className="text-xs text-gray-500 truncate" title={result.authorName}>
                          by {result.authorName}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 overflow-hidden truncate text-sm text-gray-700">
                      {result.providerName || '—'}
                    </td>
                    <td className="px-4 py-3 overflow-hidden text-sm">
                      {result.success && result.freeTextUrl ? (
                        <a
                          href={result.freeTextUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:underline truncate block"
                          title={result.freeTextUrl}
                        >
                          {result.freeTextUrl}
                        </a>
                      ) : (
                        <span className="text-gray-600">{result.errorMessage || '—'}</span>
                      )}
                    </td>
                  </tr>
                  {/* Expanded row showing providers searched */}
                  {expandedRows.has(result.bookId) && result.providersSearched.length > 0 && (
                    <tr key={`${result.bookId}-providers`} className="bg-gray-50">
                      <td colSpan={5} className="px-4 py-2">
                        <div className="text-xs text-gray-500">
                          <span className="font-medium">Providers searched:</span>{' '}
                          {result.providersSearched.join(', ')}
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Modal>
  )
}
