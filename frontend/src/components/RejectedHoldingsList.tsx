// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import type { CatalogMatchRef, PublicLibrary, RejectedHolding } from '@/types/dtos'
import { appendHoldings } from '@/utils/rejectedMatches'

const LIBRARIES: PublicLibrary[] = ['YDL', 'EMU', 'ACLA', 'WCC', 'MSU']

interface RejectedHoldingsListProps {
  rows: RejectedHolding[]
  onChange: (rows: RejectedHolding[]) => void
}

export function RejectedHoldingsList({ rows, onChange }: RejectedHoldingsListProps) {
  const [library, setLibrary] = useState<PublicLibrary>('YDL')
  const [recordKey, setRecordKey] = useState('')
  const [reason, setReason] = useState('')

  const add = () => {
    const key = recordKey.trim()
    const trimmedReason = reason.trim()
    if (!key || !trimmedReason) return
    onChange(appendHoldings(rows, library, [{ recordKey: key }], trimmedReason))
    setRecordKey('')
    setReason('')
  }

  return (
    <details className="rounded-md border border-gray-200 px-3 py-2" data-test="book-invalid-holdings">
      <summary className="cursor-pointer text-sm font-medium text-gray-700">
        Rejected catalog matches ({rows.length})
      </summary>
      <ul className="mt-3 space-y-2">
        {rows.map((row, index) => (
          <li key={`${row.library}-${row.recordKey}-${index}`} className="flex items-start justify-between gap-2 text-sm">
            <span>
              <span className="font-medium">{row.library}</span> {row.recordKey}
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
              data-test="book-invalid-holdings-remove"
              onClick={() => onChange(rows.filter((_, rowIndex) => rowIndex !== index))}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <div className="mt-3 flex flex-wrap items-end gap-2">
        <label className="text-sm text-gray-700">
          Library
          <select
            value={library}
            onChange={(event) => setLibrary(event.target.value as PublicLibrary)}
            className="mt-1 block px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
            data-test="book-invalid-holdings-library"
          >
            {LIBRARIES.map((name) => (
              <option key={name} value={name}>{name}</option>
            ))}
          </select>
        </label>
        <label className="flex-1 min-w-[10rem] text-sm text-gray-700">
          Record id
          <input
            value={recordKey}
            onChange={(event) => setRecordKey(event.target.value)}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
            data-test="book-invalid-holdings-key"
          />
        </label>
        <label className="flex-1 min-w-[10rem] text-sm text-gray-700">
          Reason
          <input
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="different book"
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
            data-test="book-invalid-holdings-reason"
          />
        </label>
        <Button type="button" variant="outline" size="md" onClick={add} data-test="book-invalid-holdings-add">
          Add
        </Button>
      </div>
    </details>
  )
}

interface RejectCatalogMatchesButtonProps {
  testId: string
  matches?: CatalogMatchRef[]
  onReject: (reason: string) => void
}

/** Shown after a lookup returned record ids. Clears that library's flags in the parent. */
export function RejectCatalogMatchesButton({ testId, matches, onReject }: RejectCatalogMatchesButtonProps) {
  const [reason, setReason] = useState('')
  const keys = (matches ?? []).filter((match) => match.recordKey?.trim())
  if (keys.length === 0) return null
  return (
    <div className="flex flex-wrap items-end gap-2">
      <label className="text-sm text-gray-700">
        Reason
        <input
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          className="mt-1 block px-3 py-2 border border-gray-300 rounded-md shadow-sm min-h-[44px]"
          data-test={`${testId}-reason`}
        />
      </label>
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={!reason.trim()}
        onClick={() => {
          onReject(reason.trim())
          setReason('')
        }}
        data-test={testId}
      >
        Reject these matches
      </Button>
    </div>
  )
}
