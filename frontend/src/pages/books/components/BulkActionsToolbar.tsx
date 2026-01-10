// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useDeleteBooks, useBulkBookFromImage } from '@/api/books'
import { useLookupBulkBooks, type LocLookupResultDto } from '@/api/loc-lookup'
import { useLookupBulkBooksGrokipedia, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { generateLabelsPdf } from '@/api/labels'
import { LocLookupResultsModal } from './LocLookupResultsModal'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { BookFromImageResultsModal } from './BookFromImageResultsModal'
import { PiFilePdf } from 'react-icons/pi'
import { PiCamera } from 'react-icons/pi'

interface BulkActionsToolbarProps {
  selectedIds: Set<number>
  onClearSelection: () => void
}

export type BookFromImageResult = { id: number; success: boolean; book?: { title: string }; error?: string }

export function BulkActionsToolbar({ selectedIds, onClearSelection }: BulkActionsToolbarProps) {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showResults, setShowResults] = useState(false)
  const [lookupResults, setLookupResults] = useState<LocLookupResultDto[]>([])
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])
  const [showBookFromImageResults, setShowBookFromImageResults] = useState(false)
  const [bookFromImageResults, setBookFromImageResults] = useState<BookFromImageResult[]>([])
  const [isGeneratingLabels, setIsGeneratingLabels] = useState(false)

  const deleteBooks = useDeleteBooks()
  const lookupBulk = useLookupBulkBooks()
  const lookupGrokipedia = useLookupBulkBooksGrokipedia()
  const bulkBookFromImage = useBulkBookFromImage()

  const handleBulkDelete = async () => {
    try {
      await deleteBooks.mutateAsync(Array.from(selectedIds))
      onClearSelection()
      setShowDeleteConfirm(false)
    } catch (error) {
      console.error('Failed to delete books:', error)
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
      const blob = await generateLabelsPdf(Array.from(selectedIds))

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'book-labels.pdf'
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
              data-test="bulk-book-from-image"
            >
              Book from Image
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
    </>
  )
}
