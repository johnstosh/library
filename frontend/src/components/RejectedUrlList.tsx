// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import type { RejectedUrl } from '@/types/dtos'
import { appendRejectedUrls, httpUrls, liveUrlIsRejected, type LiveMatch } from '@/utils/rejectedMatches'

interface RejectedUrlListProps {
  title: string
  testId: string
  rows: RejectedUrl[]
  onChange: (rows: RejectedUrl[]) => void
  /** Live field. A match shows a non-blocking warning. */
  liveValue?: string
  liveKind?: LiveMatch
  /** Move every URL in the live field onto this list and clear the field. */
  onMove?: (reason: string) => void
  addPlaceholder?: string
}

export function RejectedUrlList({
  title,
  testId,
  rows,
  onChange,
  liveValue = '',
  liveKind = 'any',
  onMove,
  addPlaceholder = 'https://...',
}: RejectedUrlListProps) {
  const [url, setUrl] = useState('')
  const [reason, setReason] = useState('')
  const [moveReason, setMoveReason] = useState('')
  const warning = liveUrlIsRejected(liveValue, rows, liveKind)

  const add = () => {
    const trimmedUrl = url.trim()
    const trimmedReason = reason.trim()
    if (!trimmedUrl || !trimmedReason) return
    onChange(appendRejectedUrls(rows, [trimmedUrl], trimmedReason))
    setUrl('')
    setReason('')
  }

  return (
    <div className="space-y-2">
      {warning && (
        <p className="text-sm text-amber-700" data-test={`${testId}-warning`}>
          A URL in this field is already rejected. You can still save.
        </p>
      )}
      {onMove && (
        <div className="flex flex-wrap items-end gap-2">
          <label className="flex-1 min-w-[12rem] text-sm text-gray-700">
            Reason
            <input
              value={moveReason}
              onChange={(event) => setMoveReason(event.target.value)}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
              data-test={`${testId}-move-reason`}
            />
          </label>
          <Button
            type="button"
            variant="outline"
            size="md"
            disabled={!moveReason.trim() || httpUrls(liveValue).length === 0}
            onClick={() => {
              onMove(moveReason.trim())
              setMoveReason('')
            }}
            data-test={`${testId}-move`}
          >
            Move to rejected
          </Button>
        </div>
      )}
      <details className="rounded-md border border-gray-200 px-3 py-2" data-test={testId}>
        <summary className="cursor-pointer text-sm font-medium text-gray-700">
          {title} ({rows.length})
        </summary>
        <ul className="mt-3 space-y-2">
          {rows.map((row, index) => (
            <li key={`${row.url}-${index}`} className="flex items-start justify-between gap-2 text-sm">
              <span>
                <span className="break-all">{row.url}</span>
                {row.reason ? <span className="text-gray-500"> — {row.reason}</span> : null}
                {row.date || row.by ? (
                  <span className="block text-xs text-gray-400">
                    {[row.date, row.by].filter(Boolean).join(' · ')}
                  </span>
                ) : null}
              </span>
              <button
                type="button"
                className="text-red-600 hover:text-red-800 shrink-0"
                data-test={`${testId}-remove`}
                onClick={() => onChange(rows.filter((_, rowIndex) => rowIndex !== index))}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="mt-3 flex flex-wrap items-end gap-2">
          <label className="flex-1 min-w-[12rem] text-sm text-gray-700">
            URL
            <input
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              placeholder={addPlaceholder}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
              data-test={`${testId}-url`}
            />
          </label>
          <label className="flex-1 min-w-[10rem] text-sm text-gray-700">
            Reason
            <input
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              placeholder="different person"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
              data-test={`${testId}-reason`}
            />
          </label>
          <Button type="button" variant="outline" size="md" onClick={add} data-test={`${testId}-add`}>
            Add
          </Button>
        </div>
      </details>
    </div>
  )
}
