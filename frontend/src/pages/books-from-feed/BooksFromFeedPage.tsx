// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { SavedBooksTable } from './components/SavedBooksTable'
import { PhotoPickerModal } from './components/PhotoPickerModal'
import { ProcessingResultsModal } from './components/ProcessingResultsModal'
import {
  useSavedBooks,
  useProcessSavedPhotos,
  type ProcessResultDto,
} from '@/api/books-from-feed'
import { PiUpload, PiMagicWand } from 'react-icons/pi'

export function BooksFromFeedPage() {
  const [showPickerModal, setShowPickerModal] = useState(false)
  const [showResultsModal, setShowResultsModal] = useState(false)
  const [processingResults, setProcessingResults] = useState<ProcessResultDto | null>(null)
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const { data: savedBooks = [], isLoading, refetch } = useSavedBooks()
  const processAll = useProcessSavedPhotos()

  const handleProcessAll = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await processAll.mutateAsync()
      setProcessingResults(result)
      setShowResultsModal(true)
      await refetch()
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to process photos')
    }
  }

  const handlePickerSuccess = (count: number) => {
    setSuccessMessage(`Successfully saved ${count} photo${count === 1 ? '' : 's'} to database`)
    setTimeout(() => setSuccessMessage(''), 5000)
  }

  const booksNeedingProcessing = savedBooks.filter((b) => b.needsProcessing)

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Books from Google Photos</h1>
          <p className="text-gray-600 mt-1">
            Import book photos from Google Photos and process them using AI
          </p>
        </div>
        <div className="flex gap-3">
          <Button
            variant="outline"
            onClick={() => setShowPickerModal(true)}
            leftIcon={<PiUpload />}
            data-test="open-picker"
          >
            Select Photos
          </Button>
          {booksNeedingProcessing.length > 0 && (
            <Button
              variant="primary"
              onClick={handleProcessAll}
              isLoading={processAll.isPending}
              disabled={processAll.isPending}
              leftIcon={<PiMagicWand />}
              data-test="process-all"
            >
              Process All ({booksNeedingProcessing.length})
            </Button>
          )}
        </div>
      </div>

      {successMessage && <SuccessMessage message={successMessage} />}
      {errorMessage && <ErrorMessage message={errorMessage} />}

      {/* Info Section */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold text-blue-900 mb-3">How It Works</h2>
        <div className="space-y-2 text-sm text-blue-800">
          <div className="flex items-start gap-2">
            <span className="font-bold">1.</span>
            <p>
              <strong>Select Photos:</strong> Click "Select Photos" to open Google Photos Picker
              and choose book cover images from your library.
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="font-bold">2.</span>
            <p>
              <strong>Save to Database:</strong> Selected photos are saved as placeholder books
              with temporary titles based on upload timestamps.
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="font-bold">3.</span>
            <p>
              <strong>AI Processing:</strong> Click "Process All" to use AI to extract book
              information (title, author, ISBN) from the cover images and update the book records.
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="font-bold">4.</span>
            <p>
              <strong>Review & Edit:</strong> After processing, review the books in the main Books
              page and edit any details as needed.
            </p>
          </div>
        </div>
      </div>

      {/* Saved Books Table */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Saved Books ({savedBooks.length})
          </h2>
          {booksNeedingProcessing.length > 0 && (
            <p className="text-sm text-gray-600 mt-1">
              {booksNeedingProcessing.length} book{booksNeedingProcessing.length === 1 ? '' : 's'}{' '}
              need{booksNeedingProcessing.length === 1 ? 's' : ''} AI processing
            </p>
          )}
        </div>

        <div className="p-6">
          <SavedBooksTable books={savedBooks} isLoading={isLoading} onRefresh={refetch} />
        </div>
      </div>

      {/* Photo Picker Modal */}
      <PhotoPickerModal
        isOpen={showPickerModal}
        onClose={() => setShowPickerModal(false)}
        onSuccess={handlePickerSuccess}
      />

      {/* Processing Results Modal */}
      {processingResults && (
        <ProcessingResultsModal
          isOpen={showResultsModal}
          onClose={() => setShowResultsModal(false)}
          results={processingResults}
        />
      )}
    </div>
  )
}
