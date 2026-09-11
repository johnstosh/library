// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { EntityLink } from '@/components/ui/EntityLink'
import type { BookPriceDto, BookPriceLookupResultDto } from '@/types/dtos'
import { formatUsd } from '@/utils/formatters'
import { PiCheckCircle, PiXCircle } from 'react-icons/pi'

interface PriceLookupResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: BookPriceLookupResultDto[]
}

function listingCell(price?: BookPriceDto) {
  if (!price) return '—'
  if (price.lookupError) return price.lookupError
  if (price.priceDollars == null) return 'No listing'
  const total = formatUsd(price.totalDollars)
  const condition = price.condition ? ` · ${price.condition}` : ''
  return `${total}${condition}`
}

export function PriceLookupResultsModal({
  isOpen,
  onClose,
  results,
}: PriceLookupResultsModalProps) {
  const successCount = results.filter((r) => r.success).length
  const cancelledCount = results.filter((r) => r.cancelled).length
  const failureCount = results.filter((r) => !r.success && !r.cancelled).length

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="AbeBooks Price Lookup Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="close-price-results">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="bg-gray-50 rounded-lg p-4">
          <div className={`grid gap-4 ${cancelledCount > 0 ? 'grid-cols-4' : 'grid-cols-3'}`}>
            <div>
              <p className="text-sm font-medium text-gray-500">Total</p>
              <p className="text-2xl font-bold text-gray-900">{results.length}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Found a listing</p>
              <p className="text-2xl font-bold text-green-600">{successCount}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">None found</p>
              <p className="text-2xl font-bold text-red-600">{failureCount}</p>
            </div>
            {cancelledCount > 0 && (
              <div>
                <p className="text-sm font-medium text-gray-500">Cancelled</p>
                <p className="text-2xl font-bold text-amber-600">{cancelledCount}</p>
              </div>
            )}
          </div>
        </div>

        <div className="max-h-96 overflow-y-auto">
          <table className="min-w-full table-fixed divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '8%' }}>
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '28%' }}>
                  Book
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '27%' }}>
                  Hardcover
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '37%' }}>
                  Softcover
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {results.map((result) => (
                <tr key={result.bookId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 whitespace-nowrap">
                    {result.success ? (
                      <PiCheckCircle className="w-5 h-5 text-green-600" />
                    ) : (
                      <PiXCircle className={`w-5 h-5 ${result.cancelled ? 'text-amber-500' : 'text-red-600'}`} />
                    )}
                  </td>
                  <td className="px-4 py-3 truncate">
                    <EntityLink to={`/books/${result.bookId}`}>{result.bookTitle ?? `Book ${result.bookId}`}</EntityLink>
                    {result.errorMessage && !result.success && (
                      <p className="text-xs text-gray-500 truncate">{result.errorMessage}</p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-800">{listingCell(result.hardcover)}</td>
                  <td className="px-4 py-3 text-sm text-gray-800">{listingCell(result.softcover)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Modal>
  )
}
