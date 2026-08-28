// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/hooks/useToast'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Modal } from '@/components/ui/Modal'
import { useDeleteBooks, useBulkBookFromImage, useLookupBulkGenresWithProgress } from '@/api/books'
import { useLookupBulkBooksWithProgress, type LocLookupResultDto } from '@/api/loc-lookup'
import { useLookupBulkBooksGrokipediaWithProgress, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { useLookupBulkFreeTextWithProgress, type FreeTextLookupResultDto } from '@/api/free-text-lookup'
import { useLookupBulkYdlWithProgress, type YdlLookupResultDto } from '@/api/ydl-lookup'
import { useLookupBulkEmuWithProgress, type EmuLookupResultDto } from '@/api/emu-lookup'
import { generateLabelsPdf } from '@/api/labels'
import { LocLookupResultsModal } from './LocLookupResultsModal'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { FreeTextLookupResultsModal } from '@/components/FreeTextLookupResultsModal'
import { BookFromImageResultsModal } from './BookFromImageResultsModal'
import { GenreLookupResultsModal } from './GenreLookupResultsModal'
import { YdlLookupResultsModal } from './YdlLookupResultsModal'
import { EmuLookupResultsModal } from './EmuLookupResultsModal'
import { PiFilePdf } from 'react-icons/pi'
import { PiCamera } from 'react-icons/pi'
import { PiBookOpen } from 'react-icons/pi'
import { AiIcon, EmuIcon, GrokipediaIcon, LocIcon, YdlIcon } from '@/components/ui/Icons'
import type { BulkDeleteResultDto, GenreLookupResultDto } from '@/types/dtos'
import { SelectionSummary, SelectionToolbar, TableCountPlaceholder } from '@/components/table/SelectionToolbar'

interface BulkActionsToolbarProps {
  selectedIds: Set<number>
  onClearSelection: () => void
  tableCount: number
  totalCount?: number
  isLoading?: boolean
}

export type BookFromImageResult = { id: number; success: boolean; book?: { title: string }; error?: string }

function progressLabel(idle: string, running: string, isPending: boolean, completed: number, total: number) {
  return isPending ? `${running} (${completed}/${total})` : idle
}

export function BulkActionsToolbar({
  selectedIds,
  onClearSelection,
  tableCount,
  totalCount,
  isLoading = false,
}: BulkActionsToolbarProps) {
  const toast = useToast()
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
  const [locProgress, setLocProgress] = useState(0)
  const [grokipediaProgress, setGrokipediaProgress] = useState(0)
  const [bookFromImageProgress, setBookFromImageProgress] = useState(0)
  const [freeTextProgress, setFreeTextProgress] = useState(0)
  const [showGenreResults, setShowGenreResults] = useState(false)
  const [genreResults, setGenreResults] = useState<GenreLookupResultDto[]>([])
  const [genreProgress, setGenreProgress] = useState(0)
  const [showYdlResults, setShowYdlResults] = useState(false)
  const [ydlResults, setYdlResults] = useState<YdlLookupResultDto[]>([])
  const [ydlProgress, setYdlProgress] = useState(0)
  const [showEmuResults, setShowEmuResults] = useState(false)
  const [emuResults, setEmuResults] = useState<EmuLookupResultDto[]>([])
  const [emuProgress, setEmuProgress] = useState(0)

  const deleteBooks = useDeleteBooks()
  const lookupBulk = useLookupBulkBooksWithProgress((completed) => {
    setLocProgress(completed)
  })
  const lookupGrokipedia = useLookupBulkBooksGrokipediaWithProgress((completed) => {
    setGrokipediaProgress(completed)
  })
  const lookupFreeText = useLookupBulkFreeTextWithProgress((completed) => {
    setFreeTextProgress(completed)
  })
  const bulkBookFromImage = useBulkBookFromImage((completed) => {
    setBookFromImageProgress(completed)
  })
  const lookupGenres = useLookupBulkGenresWithProgress((completed) => {
    setGenreProgress(completed)
  })
  const lookupYdl = useLookupBulkYdlWithProgress((completed) => {
    setYdlProgress(completed)
  })
  const lookupEmu = useLookupBulkEmuWithProgress((completed) => {
    setEmuProgress(completed)
  })

  const selectedCount = selectedIds.size

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
      toast.error('Failed to delete books')
    }
  }

  const handleBulkLookup = async () => {
    setLocProgress(0)
    try {
      const results = await lookupBulk.mutateAsync(Array.from(selectedIds))
      setLookupResults(results)
      setShowResults(true)
    } catch (error) {
      console.error('Failed to lookup LOC:', error)
      toast.error('Failed to lookup LOC')
    }
  }

  const handleGrokipediaLookup = async () => {
    setGrokipediaProgress(0)
    try {
      const results = await lookupGrokipedia.mutateAsync(Array.from(selectedIds))
      setGrokipediaResults(results)
      setShowGrokipediaResults(true)
    } catch (error) {
      console.error('Failed to lookup Grokipedia URLs:', error)
      toast.error('Failed to lookup Grokipedia URLs')
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
      toast.error('Failed to generate labels PDF. Please try again.')
    } finally {
      setIsGeneratingLabels(false)
    }
  }

  const handleBookFromImage = async () => {
    setBookFromImageProgress(0)
    try {
      const results = await bulkBookFromImage.mutateAsync(Array.from(selectedIds))
      setBookFromImageResults(results)
      setShowBookFromImageResults(true)
    } catch (error) {
      console.error('Failed to process books from images:', error)
      toast.error('Failed to process books from images')
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
      toast.error('Failed to lookup free online text')
    }
  }

  const handleGenreLookup = async () => {
    setGenreProgress(0)
    try {
      const results = await lookupGenres.mutateAsync(Array.from(selectedIds))
      setGenreResults(results)
      setShowGenreResults(true)
    } catch (error) {
      console.error('Failed to lookup genres:', error)
      toast.error('Failed to lookup genres')
    }
  }

  const handleYdlLookup = async () => {
    setYdlProgress(0)
    try {
      const results = await lookupYdl.mutateAsync(Array.from(selectedIds))
      setYdlResults(results)
      setShowYdlResults(true)
    } catch (error) {
      console.error('Failed to lookup YDL availability:', error)
      toast.error('Failed to lookup YDL availability')
    }
  }

  const handleEmuLookup = async () => {
    setEmuProgress(0)
    try {
      const results = await lookupEmu.mutateAsync(Array.from(selectedIds))
      setEmuResults(results)
      setShowEmuResults(true)
    } catch (error) {
      console.error('Failed to lookup EMU availability:', error)
      toast.error('Failed to lookup EMU availability')
    }
  }

  if (selectedIds.size === 0) {
    return (
      <SelectionToolbar dataTest="bulk-actions-toolbar" selected={false}>
        <TableCountPlaceholder
          tableCount={tableCount}
          totalCount={totalCount}
          singular="book"
          plural="books"
          isLoading={isLoading}
        />
      </SelectionToolbar>
    )
  }

  return (
    <>
      <SelectionToolbar dataTest="bulk-actions-toolbar" selected>
        <div className="flex flex-col sm:flex-row sm:items-center gap-3 w-full min-w-0">
          <SelectionSummary
            count={selectedIds.size}
            singular="book"
            plural="books"
            onClear={onClearSelection}
          />
          <div className="flex gap-2 overflow-x-auto flex-nowrap min-w-0 [&_button]:shrink-0">
            <Button
              variant="outline"
              size="sm"
              onClick={handleBulkLookup}
              isLoading={lookupBulk.isPending}
              disabled={lookupBulk.isPending}
              leftIcon={<LocIcon />}
              data-test="bulk-lookup-loc"
            >
              {progressLabel('Lookup LOC', 'LOC...', lookupBulk.isPending, locProgress, selectedCount)}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleGrokipediaLookup}
              isLoading={lookupGrokipedia.isPending}
              disabled={lookupGrokipedia.isPending}
              leftIcon={<GrokipediaIcon />}
              data-test="bulk-lookup-grokipedia"
            >
              {progressLabel(
                'Find Grokipedia URLs',
                'Grokipedia...',
                lookupGrokipedia.isPending,
                grokipediaProgress,
                selectedCount
              )}
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
              {progressLabel(
                'Find links to free online text',
                'Finding...',
                lookupFreeText.isPending,
                freeTextProgress,
                selectedCount
              )}
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
              {progressLabel(
                'Book from Images',
                'Images...',
                bulkBookFromImage.isPending,
                bookFromImageProgress,
                selectedCount
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleGenreLookup}
              isLoading={lookupGenres.isPending}
              disabled={lookupGenres.isPending}
              leftIcon={<AiIcon />}
              data-test="bulk-lookup-genres"
            >
              {progressLabel('Lookup Genres', 'Genres...', lookupGenres.isPending, genreProgress, selectedCount)}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleYdlLookup}
              isLoading={lookupYdl.isPending}
              disabled={lookupYdl.isPending}
              leftIcon={<YdlIcon />}
              data-test="bulk-lookup-ydl"
            >
              {progressLabel(
                'Lookup YDL Availability',
                'Checking YDL...',
                lookupYdl.isPending,
                ydlProgress,
                selectedCount
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleEmuLookup}
              isLoading={lookupEmu.isPending}
              disabled={lookupEmu.isPending}
              leftIcon={<EmuIcon />}
              data-test="bulk-lookup-emu"
            >
              {progressLabel(
                'Lookup EMU Availability',
                'Checking EMU...',
                lookupEmu.isPending,
                emuProgress,
                selectedCount
              )}
            </Button>
            <Button
              variant="danger"
              size="sm"
              className="shrink-0"
              onClick={() => setShowDeleteConfirm(true)}
              data-test="bulk-delete"
            >
              Delete Selected
            </Button>
          </div>
        </div>
      </SelectionToolbar>

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

      <YdlLookupResultsModal
        isOpen={showYdlResults}
        onClose={() => setShowYdlResults(false)}
        results={ydlResults}
      />

      <EmuLookupResultsModal
        isOpen={showEmuResults}
        onClose={() => setShowEmuResults(false)}
        results={emuResults}
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
