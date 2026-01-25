// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { GenreLookupResultDto } from '@/types/dtos'

interface GenreLookupResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: GenreLookupResultDto[]
}

export function GenreLookupResultsModal({ isOpen, onClose, results }: GenreLookupResultsModalProps) {
  const successCount = results.filter((r) => r.success).length
  const failedCount = results.filter((r) => !r.success).length

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Genre Lookup Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="genre-lookup-results-close">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
          <p className="text-blue-800 font-medium">
            {successCount} {successCount === 1 ? 'book' : 'books'} processed successfully
            {failedCount > 0 && `, ${failedCount} skipped or failed`}
          </p>
        </div>

        <div className="max-h-96 overflow-y-auto space-y-3">
          {results.map((result) => (
            <div
              key={result.bookId}
              className={`rounded-lg p-3 border ${
                result.success
                  ? 'bg-green-50 border-green-200'
                  : 'bg-yellow-50 border-yellow-200'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <p className={`font-medium ${result.success ? 'text-green-800' : 'text-yellow-800'}`}>
                    {result.title || `Book #${result.bookId}`}
                  </p>
                  {result.success && result.suggestedGenres && result.suggestedGenres.length > 0 ? (
                    <div className="flex flex-wrap gap-1 mt-2">
                      {result.suggestedGenres.map((genre) => (
                        <span
                          key={genre}
                          className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-indigo-100 text-indigo-800"
                        >
                          {genre}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-yellow-700 mt-1">
                      {result.errorMessage || 'No genres suggested'}
                    </p>
                  )}
                </div>
                <span className={`text-sm ${result.success ? 'text-green-600' : 'text-yellow-600'}`}>
                  {result.success ? 'Suggested' : 'Skipped'}
                </span>
              </div>
            </div>
          ))}
        </div>

        <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
          <p className="text-gray-700 text-sm">
            <strong>Note:</strong> These are suggestions only. To apply genres to a book, edit the book and add the suggested tags.
          </p>
        </div>
      </div>
    </Modal>
  )
}
