// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'
import { PiArrowClockwise, PiArrowCounterClockwise, PiCheckCircle } from 'react-icons/pi'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import {
  applyChicago,
  applyGrokipedia,
  applyGrokipediaAuthors,
  fetchChicagoUndoable,
  fetchFreeTextCandidateIds,
  fetchGrokipediaAuthorCandidateIds,
  fetchGrokipediaCandidateIds,
  fetchGrokipediaAuthorUndoable,
  fetchGrokipediaUndoable,
  previewChicago,
  previewGrokipedia,
  previewGrokipediaAuthors,
  searchFreeText,
  undoChicago,
  undoGrokipedia,
  undoGrokipediaAuthors,
  type CatalogToolApplyResultDto,
  type CatalogToolChangeDto,
  type CatalogToolPreviewDto,
  type CatalogToolUndoableDto,
} from '@/api/data-management'

const GROKIPEDIA_BATCH = 20
const FREE_TEXT_BATCH = 8

type Phase = 'idle' | 'preview' | 'done'
type Busy = 'recalc' | 'apply' | 'undo' | null

interface ToolCopy {
  testKey: string
  name: string
  description: string
  formatChange: (change: CatalogToolChangeDto) => string
  recalc: (onProgress: (text: string) => void) => Promise<CatalogToolPreviewDto>
  apply: (changes: CatalogToolChangeDto[]) => Promise<CatalogToolApplyResultDto>
  undo: (actionId: number) => Promise<CatalogToolApplyResultDto>
  undoable: () => Promise<CatalogToolUndoableDto>
}

/**
 * One Maintenance row: Recalc lists every change, Do It writes that list, Undo puts it back.
 */
function CatalogToolRow({ tool }: { tool: ToolCopy }) {
  const [count, setCount] = useState<number | null>(null)
  const [changes, setChanges] = useState<CatalogToolChangeDto[]>([])
  const [skipped, setSkipped] = useState<CatalogToolChangeDto[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [progress, setProgress] = useState('')
  const [phase, setPhase] = useState<Phase>('idle')
  const [scanComplete, setScanComplete] = useState(false)
  const [undoActionId, setUndoActionId] = useState<number | null>(null)
  const [busy, setBusy] = useState<Busy>(null)

  useEffect(() => {
    let cancelled = false
    tool.undoable()
      .then((result) => {
        if (!cancelled && result?.actionId) {
          setUndoActionId(result.actionId)
        }
      })
      .catch(() => {
        // The row still works. Undo stays off until the next Do It.
      })
    return () => {
      cancelled = true
    }
  }, [tool])

  const handleRecalc = async () => {
    setBusy('recalc')
    setError('')
    setMessage('')
    setChanges([])
    setSkipped([])
    setCount(null)
    setScanComplete(false)
    setPhase('idle')
    try {
      const preview = await tool.recalc(setProgress)
      const nextChanges = preview.changes ?? []
      const nextSkipped = preview.skipped ?? []
      setChanges(nextChanges)
      setSkipped(nextSkipped)
      setMessage(preview.message || preview.error || '')
      if (preview.error) {
        setError(preview.error)
        setScanComplete(false)
        return
      }
      setCount(nextChanges.length)
      setScanComplete(true)
      setPhase('preview')
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Recalc failed'
      setError(msg)
      setScanComplete(false)
    } finally {
      setBusy(null)
      setProgress('')
    }
  }

  const handleApply = async () => {
    setBusy('apply')
    setError('')
    try {
      const result = await tool.apply(changes)
      if (result.error) {
        setError(result.error)
        return
      }
      setChanges(result.changes ?? [])
      setSkipped(result.skippedChanges ?? [])
      setCount(result.applied ?? 0)
      setMessage(result.message || 'Done')
      setScanComplete(false)
      setPhase('done')
      if (result.actionId) {
        setUndoActionId(result.actionId)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Do It failed')
    } finally {
      setBusy(null)
    }
  }

  const handleUndo = async () => {
    if (undoActionId == null) return
    setBusy('undo')
    setError('')
    try {
      const result = await tool.undo(undoActionId)
      if (result.error) {
        setError(result.error)
        return
      }
      setChanges(result.changes ?? [])
      setSkipped(result.skippedChanges ?? [])
      setCount(result.applied ?? 0)
      setMessage(result.message || 'Undone')
      setPhase('done')
      setScanComplete(false)
      const next = await tool.undoable()
      setUndoActionId(next?.actionId ?? null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Undo failed')
    } finally {
      setBusy(null)
    }
  }

  return (
    <tr data-test={`maintenance-row-${tool.testKey}`}>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
        {count === null ? (
          <span className="text-gray-400 text-xl font-light" data-test={`maintenance-${tool.testKey}-count`}>-</span>
        ) : (
          <span className="text-lg font-bold tabular-nums text-indigo-700" data-test={`maintenance-${tool.testKey}-count`}>
            {count}
          </span>
        )}
      </td>
      <td className="px-6 py-4">
        <div className="text-sm font-medium text-gray-900 mb-2">{tool.name}</div>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={handleRecalc}
            isLoading={busy === 'recalc'}
            disabled={busy !== null}
            leftIcon={<PiArrowClockwise className="w-4 h-4" />}
            data-test={`recalc-${tool.testKey}`}
          >
            Recalc
          </Button>
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={handleApply}
            isLoading={busy === 'apply'}
            disabled={busy !== null || !scanComplete || changes.length === 0}
            leftIcon={<PiCheckCircle className="w-4 h-4" />}
            data-test={`apply-${tool.testKey}`}
          >
            Do It
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleUndo}
            isLoading={busy === 'undo'}
            disabled={busy !== null || undoActionId == null}
            leftIcon={<PiArrowCounterClockwise className="w-4 h-4" />}
            data-test={`undo-${tool.testKey}`}
          >
            Undo
          </Button>
        </div>
      </td>
      <td className="px-6 py-4 text-sm text-gray-600">{tool.description}</td>
      <td className="px-6 py-4 text-sm" data-test={`maintenance-${tool.testKey}-results`}>
        {error ? <ErrorMessage message={error} /> : null}
        {progress ? (
          <p className="text-sm text-gray-600 mb-2" data-test={`maintenance-${tool.testKey}-progress`}>{progress}</p>
        ) : null}
        {phase === 'done' && message ? (
          <div
            className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-2"
            role="alert"
            data-test={`maintenance-${tool.testKey}-success`}
          >
            {message}
          </div>
        ) : null}
        {phase === 'preview' && message ? (
          <p className="text-sm text-gray-700 mb-2">{message}</p>
        ) : null}
        {changes.length > 0 ? (
          <div className="max-h-64 overflow-y-auto space-y-1">
            {changes.map((change) => (
              <p
                key={changeKey(change)}
                className="text-sm text-gray-800 break-words"
                data-test={`maintenance-${tool.testKey}-change-${change.bookId ?? change.authorId}`}
              >
                {tool.formatChange(change)}
              </p>
            ))}
          </div>
        ) : null}
        {skipped.length > 0 ? (
          <div className="max-h-40 overflow-y-auto space-y-1 mt-2">
            {skipped.map((change) => (
              <p
                key={`skip-${changeKey(change)}`}
                className="text-sm text-gray-500 break-words"
                data-test={`maintenance-${tool.testKey}-skipped-${change.bookId ?? change.authorId}`}
              >
                {(change.title || change.before || `Book ${change.bookId ?? change.authorId}`)}: {change.reason}
              </p>
            ))}
          </div>
        ) : null}
        {!error && !progress && changes.length === 0 && skipped.length === 0 && phase === 'idle' ? (
          <span className="text-gray-400 text-sm">Click Recalc to see every change here.</span>
        ) : null}
      </td>
    </tr>
  )
}

function changeKey(change: CatalogToolChangeDto): string {
  if (change.authorId != null) return `author-${change.authorId}`
  return `book-${change.bookId}`
}

async function previewInBatches(
  loadIds: () => Promise<{ bookIds?: number[]; authorIds?: number[] }>,
  previewBatch: (bookIds: number[]) => Promise<CatalogToolPreviewDto>,
  batchSize: number,
  emptyMessage: string,
  doneMessage: (changeCount: number) => string,
  onProgress: (text: string) => void,
): Promise<CatalogToolPreviewDto> {
  const candidates = await loadIds()
  const ids = candidates.authorIds ?? candidates.bookIds ?? []
  if (ids.length === 0) {
    return {
      booksScanned: 0,
      changeCount: 0,
      changes: [],
      skipped: [],
      message: emptyMessage,
    }
  }
  const changes: CatalogToolChangeDto[] = []
  const skipped: CatalogToolChangeDto[] = []
  for (let index = 0; index < ids.length; index += batchSize) {
    const batch = ids.slice(index, index + batchSize)
    onProgress(`Checking ${Math.min(index + batch.length, ids.length)} of ${ids.length}`)
    const preview = await previewBatch(batch)
    if (preview.error) {
      return {
        ...preview,
        changes: [...changes, ...(preview.changes ?? [])],
        skipped: [...skipped, ...(preview.skipped ?? [])],
        booksScanned: index,
        changeCount: changes.length,
      }
    }
    changes.push(...(preview.changes ?? []))
    skipped.push(...(preview.skipped ?? []))
  }
  return {
    booksScanned: ids.length,
    changeCount: changes.length,
    changes,
    skipped,
    message: doneMessage(changes.length),
  }
}

function bookLabel(change: CatalogToolChangeDto): string {
  return change.title || `Book ${change.bookId}`
}

const grokipediaTool: ToolCopy = {
  testKey: 'grokipedia',
  name: 'Quick Grokipedia lookup',
  description:
    'Checks every book, including withdrawn copies and books that already have a Grokipedia URL. Looks up the primary title and each alternate title. Adds any new Grokipedia page it finds. Does not replace or remove a stored URL, and does not store a dash when no page is found. Recalc lists every URL that would be added. Do It writes that list. Undo removes only those added URLs.',
  formatChange: (change) => `${bookLabel(change)}: add ${change.added || change.after}`,
  recalc: (onProgress) => previewInBatches(
    fetchGrokipediaCandidateIds,
    previewGrokipedia,
    GROKIPEDIA_BATCH,
    'No books to check.',
    (count) => `${count} ${count === 1 ? 'book would gain a Grokipedia link.' : 'books would gain a Grokipedia link.'}`,
    onProgress,
  ),
  apply: applyGrokipedia,
  undo: undoGrokipedia,
  undoable: fetchGrokipediaUndoable,
}

const grokipediaAuthorTool: ToolCopy = {
  testKey: 'grokipedia-authors',
  name: 'Quick Grokipedia lookup for authors',
  description:
    'Checks every author, including authors who already have a Grokipedia URL. Looks up the primary name and each alternate name. Adds any new Grokipedia page it finds. Does not replace or remove a stored URL, and does not store a dash when no page is found. Recalc lists every URL that would be added. Do It writes that list. Undo removes only those added URLs.',
  formatChange: (change) => `${bookLabel(change)}: add ${change.added || change.after}`,
  recalc: (onProgress) => previewInBatches(
    fetchGrokipediaAuthorCandidateIds,
    previewGrokipediaAuthors,
    GROKIPEDIA_BATCH,
    'No authors to check.',
    (count) => `${count} ${count === 1 ? 'author would gain a Grokipedia link.' : 'authors would gain a Grokipedia link.'}`,
    onProgress,
  ),
  apply: applyGrokipediaAuthors,
  undo: undoGrokipediaAuthors,
  undoable: fetchGrokipediaAuthorUndoable,
}

const chicagoTool: ToolCopy = {
  testKey: 'chicago',
  name: 'Chicago Title Case',
  description:
    'Rewrites every book title that is not already in Chicago title case, including withdrawn copies. Recalc lists each title that will change, with the current wording and the wording Do It will save. A title is left alone when the Chicago form is already used by another book. Undo puts the previous titles back.',
  formatChange: (change) => `${change.before} → ${change.after}`,
  recalc: async (onProgress) => {
    onProgress('Checking every title…')
    return previewChicago()
  },
  apply: applyChicago,
  undo: undoChicago,
  undoable: fetchChicagoUndoable,
}

const FREE_TEXT_LIMITS = [100, 200, 400, 800] as const

export function GrokipediaMaintenanceRow() {
  return <CatalogToolRow tool={grokipediaTool} />
}

export function GrokipediaAuthorMaintenanceRow() {
  return <CatalogToolRow tool={grokipediaAuthorTool} />
}

export function ChicagoTitleMaintenanceRow() {
  return <CatalogToolRow tool={chicagoTool} />
}

export function FreeTextMaintenanceRow() {
  const [limit, setLimit] = useState<(typeof FREE_TEXT_LIMITS)[number]>(100)
  const [count, setCount] = useState<number | null>(null)
  const [changes, setChanges] = useState<CatalogToolChangeDto[]>([])
  const [skipped, setSkipped] = useState<CatalogToolChangeDto[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [progress, setProgress] = useState('')
  const [busy, setBusy] = useState(false)

  const handleSearch = async () => {
    setBusy(true)
    setError('')
    setMessage('')
    setChanges([])
    setSkipped([])
    setCount(null)
    const found: CatalogToolChangeDto[] = []
    const left: CatalogToolChangeDto[] = []
    try {
      const candidates = await fetchFreeTextCandidateIds(limit)
      const ids = candidates.bookIds ?? []
      if (ids.length === 0) {
        setMessage('No books to check.')
        setCount(0)
        return
      }
      for (let index = 0; index < ids.length; index += FREE_TEXT_BATCH) {
        const batch = ids.slice(index, index + FREE_TEXT_BATCH)
        const done = Math.min(index + batch.length, ids.length)
        setProgress(`Checking ${done} of ${ids.length}`)
        try {
          const result = await searchFreeText(batch)
          found.push(...(result.changes ?? []))
          left.push(...(result.skipped ?? []))
          if (result.error) {
            setChanges([...found])
            setSkipped([...left])
            setCount(found.length)
            setError(result.error)
            setMessage(result.message || '')
            return
          }
        } catch (err) {
          setChanges([...found])
          setSkipped([...left])
          setCount(found.length)
          setError(err instanceof Error ? err.message : 'Search failed')
          return
        }
      }
      setChanges(found)
      setSkipped(left)
      setCount(found.length)
      setMessage(
        `${found.length} ${found.length === 1 ? 'book gained a free-text link.' : 'books gained a free-text link.'}`,
      )
    } catch (err) {
      setChanges([...found])
      setSkipped([...left])
      setCount(found.length)
      setError(err instanceof Error ? err.message : 'Search failed')
    } finally {
      setBusy(false)
      setProgress('')
    }
  }

  return (
    <tr data-test="maintenance-row-free-text">
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
        {count === null ? (
          <span className="text-gray-400 text-xl font-light" data-test="maintenance-free-text-count">-</span>
        ) : (
          <span className="text-lg font-bold tabular-nums text-indigo-700" data-test="maintenance-free-text-count">
            {count}
          </span>
        )}
      </td>
      <td className="px-6 py-4">
        <div className="text-sm font-medium text-gray-900 mb-2">Find Links to Free Online Text</div>
        <div className="flex flex-wrap items-center gap-2">
          <label className="inline-flex items-center gap-2 text-sm text-gray-600">
            Search
            <select
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value) as (typeof FREE_TEXT_LIMITS)[number])}
              disabled={busy}
              className="px-2 py-1 border border-gray-300 rounded-md text-sm bg-white"
              data-test="free-text-limit"
              aria-label="How many books to search"
            >
              {FREE_TEXT_LIMITS.map((size) => (
                <option key={size} value={size}>{size}</option>
              ))}
            </select>
          </label>
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={handleSearch}
            isLoading={busy}
            disabled={busy}
            leftIcon={<PiArrowClockwise className="w-4 h-4" />}
            data-test="search-free-text"
          >
            Search
          </Button>
        </div>
      </td>
      <td className="px-6 py-4 text-sm text-gray-600">
        Searches the books that have never been searched, then the ones searched longest ago.
        Blank and temporary titles are skipped. Each search tries the primary title and each
        alternate title. Adds any new links onto the links already stored. Does not replace or
        remove a link. A finished search is stored before the next book, including a miss.
      </td>
      <td className="px-6 py-4 text-sm" data-test="maintenance-free-text-results">
        {error ? <ErrorMessage message={error} /> : null}
        {progress ? (
          <p className="text-sm text-gray-600 mb-2" data-test="maintenance-free-text-progress">{progress}</p>
        ) : null}
        {message ? (
          <p className="text-sm text-gray-700 mb-2" data-test="maintenance-free-text-message">{message}</p>
        ) : null}
        {changes.length > 0 ? (
          <div className="max-h-64 overflow-y-auto space-y-1">
            {changes.map((change) => (
              <p
                key={changeKey(change)}
                className="text-sm text-gray-800 break-words"
                data-test={`maintenance-free-text-change-${change.bookId}`}
              >
                {bookLabel(change)}: add {change.added || change.after}
              </p>
            ))}
          </div>
        ) : null}
        {skipped.length > 0 ? (
          <div className="max-h-40 overflow-y-auto space-y-1 mt-2">
            {skipped.map((change) => (
              <p
                key={`skip-${changeKey(change)}`}
                className="text-sm text-gray-500 break-words"
                data-test={`maintenance-free-text-skipped-${change.bookId}`}
              >
                {(change.title || `Book ${change.bookId}`)}: {change.reason}
              </p>
            ))}
          </div>
        ) : null}
        {!error && !progress && changes.length === 0 && skipped.length === 0 && count === null ? (
          <span className="text-gray-400 text-sm">Choose how many books, then Search.</span>
        ) : null}
      </td>
    </tr>
  )
}
