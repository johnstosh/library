// (c) Copyright 2025 by Muczynski
import { useState, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { SavedBooksTable } from './components/SavedBooksTable'
import { PhotoPickerModal } from './components/PhotoPickerModal'
import { ProcessingResultsModal } from './components/ProcessingResultsModal'
import {
  useSavedBooks,
  processSingleBookApi,
  type ProcessResultDto,
} from '@/api/books-from-feed'
import { PiUpload, PiMagicWand } from 'react-icons/pi'

interface ProcessingProgress {
  current: number
  total: number
  currentBookTitle: string
}

export function BooksFromFeedPage() {
  const [showPickerModal, setShowPickerModal] = useState(false)
  const [showResultsModal, setShowResultsModal] = useState(false)
  const [processingResults, setProcessingResults] = useState<ProcessResultDto | null>(null)
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isProcessingAll, setIsProcessingAll] = useState(false)
  const [progress, setProgress] = useState<ProcessingProgress | null>(null)
  const cancelRef = useRef(false)

  const queryClient = useQueryClient()
  const { data: savedBooks = [], isLoading, refetch } = useSavedBooks()

  const handleProcessAll = async () => {
    setSuccessMessage('')
    setErrorMessage('')
    cancelRef.current = false

    const booksToProcess = savedBooks.filter((b) => b.needsProcessing)
    if (booksToProcess.length === 0) return

    setIsProcessingAll(true)
    let processedCount = 0
    let failedCount = 0
    const totalBooks = booksToProcess.length

    for (let i = 0; i < booksToProcess.length; i++) {
      if (cancelRef.current) break

      const book = booksToProcess[i]
      setProgress({
        current: i + 1,
        total: totalBooks,
        currentBookTitle: book.title,
      })

      try {
        const result = await processSingleBookApi(book.id)
        if (result.success) {
          processedCount++
        } else {
          failedCount++
        }
      } catch {
        failedCount++
      }

      // Refresh the table after each book so the UI updates
      await refetch()
    }

    // Invalidate books query so the main Books page also refreshes
    queryClient.invalidateQueries({ queryKey: ['books'] })

    setIsProcessingAll(false)
    setProgress(null)

    if (!cancelRef.current) {
      const results: ProcessResultDto = {
        success: failedCount === 0,
        processedCount,
        failedCount,
        totalBooks,
      }
      setProcessingResults(results)
      setShowResultsModal(true)
    }
  }

  const handleCancelProcessing = () => {
    cancelRef.current = true
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
            disabled={isProcessingAll}
            leftIcon={<PiUpload />}
            data-test="open-picker"
          >
            Select Photos
          </Button>
          {booksNeedingProcessing.length > 0 && !isProcessingAll && (
            <Button
              variant="primary"
              onClick={handleProcessAll}
              leftIcon={<PiMagicWand />}
              data-test="process-all"
            >
              Process All ({booksNeedingProcessing.length})
            </Button>
          )}
          {isProcessingAll && (
            <Button
              variant="outline"
              onClick={handleCancelProcessing}
              data-test="cancel-processing"
            >
              Cancel
            </Button>
          )}
        </div>
      </div>

      {successMessage && <SuccessMessage message={successMessage} />}
      {errorMessage && <ErrorMessage message={errorMessage} />}

      {/* Processing Progress */}
      {progress && (
        <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4 mb-6" data-test="processing-progress">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-indigo-900">
              Processing book {progress.current} of {progress.total}...
            </span>
            <span className="text-sm font-bold text-indigo-900">
              {Math.round((progress.current / progress.total) * 100)}%
            </span>
          </div>
          <div className="w-full bg-indigo-200 rounded-full h-2.5">
            <div
              className="bg-indigo-600 h-2.5 rounded-full transition-all duration-300"
              style={{ width: `${(progress.current / progress.total) * 100}%` }}
            />
          </div>
          <p className="text-xs text-indigo-700 mt-1 truncate">
            {progress.currentBookTitle}
          </p>
        </div>
      )}

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
