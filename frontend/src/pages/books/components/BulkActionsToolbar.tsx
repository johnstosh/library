// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Modal } from '@/components/ui/Modal'
import { useDeleteBooks, useBulkBookFromImage, useLookupGenres } from '@/api/books'
import { useLookupBulkBooks, type LocLookupResultDto } from '@/api/loc-lookup'
import { useLookupBulkBooksGrokipedia, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { useLookupBulkFreeTextWithProgress, type FreeTextLookupResultDto } from '@/api/free-text-lookup'
import { generateLabelsPdf } from '@/api/labels'
import { LocLookupResultsModal } from './LocLookupResultsModal'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { FreeTextLookupResultsModal } from '@/components/FreeTextLookupResultsModal'
import { BookFromImageResultsModal } from './BookFromImageResultsModal'
import { GenreLookupResultsModal } from './GenreLookupResultsModal'
import { PiFilePdf } from 'react-icons/pi'
import { PiCamera } from 'react-icons/pi'
import { PiBookOpen } from 'react-icons/pi'
import type { BulkDeleteResultDto, GenreLookupResultDto } from '@/types/dtos'
import { PiTag } from 'react-icons/pi'

interface BulkActionsToolbarProps {
  selectedIds: Set<number>
  onClearSelection: () => void
}

export type BookFromImageResult = { id: number; success: boolean; book?: { title: string }; error?: string }

export function BulkActionsToolbar({ selectedIds, onClearSelection }: BulkActionsToolbarProps) {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showDeleteResults, setShowDeleteResults] = useState(false)
  const [deleteResults, setDeleteResults] = useState<BulkDeleteResultDto | null>(null)
  const [showResults, setShowResults] = useState(false)
  const [lookupResults, setLookupResults] = useState<LocLookupResultDto[]>([])
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])
  const [showBookFromImageResults, setShowBookFromImageResults] = useState(false)
  const [bookFromImageResults, setBookFromImageResults] = useState<BookFromImageResult[]>([])
  const [isGeneratingLabels, setIsGeneratingLabels] = useState(false)
  const [showFreeTextResults, setShowFreeTextResults] = useState(false)
  const [freeTextResults, setFreeTextResults] = useState<FreeTextLookupResultDto[]>([])
  const [freeTextProgress, setFreeTextProgress] = useState(0)
  const [showGenreResults, setShowGenreResults] = useState(false)
  const [genreResults, setGenreResults] = useState<GenreLookupResultDto[]>([])
  const [genreProgress, setGenreProgress] = useState(0)
  const [isGenreLookupRunning, setIsGenreLookupRunning] = useState(false)

  const deleteBooks = useDeleteBooks()
  const lookupBulk = useLookupBulkBooks()
  const lookupGrokipedia = useLookupBulkBooksGrokipedia()
  const lookupFreeText = useLookupBulkFreeTextWithProgress((completed, _total) => {
    setFreeTextProgress(completed)
  })
  const bulkBookFromImage = useBulkBookFromImage()
  const lookupGenres = useLookupGenres()

  const handleBulkDelete = async () => {
    try {
      const result = await deleteBooks.mutateAsync(Array.from(selectedIds))
      setShowDeleteConfirm(false)
      onClearSelection()
      // Show results if there were any failures
      if (result.failedCount > 0) {
        setDeleteResults(result)
        setShowDeleteResults(true)
      }
    } catch (error) {
      console.error('Failed to delete books:', error)
      setShowDeleteConfirm(false)
    }
  }

  const handleBulkLookup = async () => {
    try {
      const results = await lookupBulk.mutateAsync(Array.from(selectedIds))
      setLookupResults(results)
      setShowResults(true)
    } catch (error) {
      console.error('Failed to lookup LOC:', error)
    }
  }

  const handleGrokipediaLookup = async () => {
    try {
      const results = await lookupGrokipedia.mutateAsync(Array.from(selectedIds))
      setGrokipediaResults(results)
      setShowGrokipediaResults(true)
    } catch (error) {
      console.error('Failed to lookup Grokipedia URLs:', error)
    }
  }

  const handleGenerateLabels = async () => {
    if (selectedIds.size === 0) return

    setIsGeneratingLabels(true)
    try {
      const { blob, filename } = await generateLabelsPdf(Array.from(selectedIds))

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to generate labels PDF:', error)
      alert('Failed to generate labels PDF. Please try again.')
    } finally {
      setIsGeneratingLabels(false)
    }
  }

  const handleBookFromImage = async () => {
    try {
      const results = await bulkBookFromImage.mutateAsync(Array.from(selectedIds))
      setBookFromImageResults(results)
      setShowBookFromImageResults(true)
    } catch (error) {
      console.error('Failed to process books from images:', error)
    }
  }

  const handleFreeTextLookup = async () => {
    setFreeTextProgress(0)
    try {
      const results = await lookupFreeText.mutateAsync(Array.from(selectedIds))
      setFreeTextResults(results)
      setShowFreeTextResults(true)
    } catch (error) {
      console.error('Failed to lookup free online text:', error)
    }
  }

  const handleGenreLookup = async () => {
    const ids = Array.from(selectedIds)
    setGenreProgress(0)
    setGenreResults([])
    setShowGenreResults(true)
    setIsGenreLookupRunning(true)
    try {
      for (let i = 0; i < ids.length; i++) {
        try {
          const result = await lookupGenres.mutateAsync(ids[i])
          setGenreResults((prev) => [...prev, result])
        } catch (error) {
          setGenreResults((prev) => [
            ...prev,
            { bookId: ids[i], success: false, errorMessage: String(error) } as GenreLookupResultDto,
          ])
        }
        setGenreProgress(i + 1)
      }
    } finally {
      setIsGenreLookupRunning(false)
    }
  }

  if (selectedIds.size === 0) return null

  return (
    <>
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-blue-900">
              {selectedIds.size} {selectedIds.size === 1 ? 'book' : 'books'} selected
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={onClearSelection}
              data-test="clear-selection"
            >
              Clear Selection
            </Button>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleBulkLookup}
              isLoading={lookupBulk.isPending}
              disabled={lookupBulk.isPending}
              leftIcon={<span>🗃️</span>}
              data-test="bulk-lookup-loc"
            >
              Lookup LOC
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleGrokipediaLookup}
              isLoading={lookupGrokipedia.isPending}
              disabled={lookupGrokipedia.isPending}
              leftIcon={<span>🌐</span>}
              data-test="bulk-lookup-grokipedia"
            >
              Find Grokipedia URLs
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleFreeTextLookup}
              isLoading={lookupFreeText.isPending}
              disabled={lookupFreeText.isPending}
              leftIcon={<PiBookOpen />}
              data-test="bulk-lookup-free-text"
            >
              {lookupFreeText.isPending
                ? `Finding... (${freeTextProgress}/${selectedIds.size})`
                : 'Find links to free online text'}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleGenerateLabels}
              isLoading={isGeneratingLabels}
              disabled={isGeneratingLabels}
              leftIcon={<PiFilePdf />}
              data-test="bulk-generate-labels"
            >
              Generate Labels
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleBookFromImage}
              isLoading={bulkBookFromImage.isPending}
              disabled={bulkBookFromImage.isPending}
              leftIcon={<PiCamera />}
              data-test="bulk-book-from-images"
            >
              Book from Images
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleGenreLookup}
              isLoading={isGenreLookupRunning}
              disabled={isGenreLookupRunning}
              leftIcon={<PiTag />}
              data-test="bulk-lookup-genres"
            >
              {isGenreLookupRunning
                ? `Genres... (${genreProgress}/${selectedIds.size})`
                : 'Lookup Genres'}
            </Button>
            <Button
              variant="danger"
              size="sm"
              onClick={() => setShowDeleteConfirm(true)}
              data-test="bulk-delete"
            >
              Delete Selected
            </Button>
          </div>
        </div>
      </div>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleBulkDelete}
        title="Delete Books"
        message={`Are you sure you want to delete ${selectedIds.size} ${
          selectedIds.size === 1 ? 'book' : 'books'
        }? This action cannot be undone.`}
        confirmText="Delete"
        variant="danger"
        isLoading={deleteBooks.isPending}
      />

      <LocLookupResultsModal
        isOpen={showResults}
        onClose={() => setShowResults(false)}
        results={lookupResults}
      />

      <GrokipediaLookupResultsModal
        isOpen={showGrokipediaResults}
        onClose={() => setShowGrokipediaResults(false)}
        results={grokipediaResults}
        entityType="book"
      />

      <BookFromImageResultsModal
        isOpen={showBookFromImageResults}
        onClose={() => setShowBookFromImageResults(false)}
        results={bookFromImageResults}
      />

      <FreeTextLookupResultsModal
        isOpen={showFreeTextResults}
        onClose={() => setShowFreeTextResults(false)}
        results={freeTextResults}
      />

      <GenreLookupResultsModal
        isOpen={showGenreResults}
        onClose={() => setShowGenreResults(false)}
        results={genreResults}
      />

      <Modal
        isOpen={showDeleteResults}
        onClose={() => setShowDeleteResults(false)}
        title="Delete Results"
        size="md"
        footer={
          <div className="flex justify-end">
            <Button variant="primary" onClick={() => setShowDeleteResults(false)} data-test="delete-results-close">
              Close
            </Button>
          </div>
        }
      >
        {deleteResults && (
          <div className="space-y-4">
            {deleteResults.deletedCount > 0 && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                <p className="text-green-800 font-medium">
                  {deleteResults.deletedCount} {deleteResults.deletedCount === 1 ? 'book' : 'books'} deleted successfully
                </p>
              </div>
            )}
            {deleteResults.failedCount > 0 && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-red-800 font-medium mb-2">
                  {deleteResults.failedCount} {deleteResults.failedCount === 1 ? 'book' : 'books'} could not be deleted:
                </p>
                <ul className="list-disc list-inside space-y-1">
                  {deleteResults.failures.map((failure) => (
                    <li key={failure.id} className="text-red-700 text-sm">
                      <span className="font-medium">{failure.title}</span>: {failure.errorMessage}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>
    </>
  )
}
