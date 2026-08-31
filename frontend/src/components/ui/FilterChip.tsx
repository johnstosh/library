// (c) Copyright 2025 by Muczynski
import { useEffect, useId, useRef, useState } from 'react'
import { PiFunnel, PiInfo } from 'react-icons/pi'

export interface FilterChipProps {
  label: string
  active: boolean
  onClick: () => void
  tooltip: string
  dataTest: string
  disabled?: boolean
  hideOnMobile?: boolean
}

export function FilterChip({
  label,
  active,
  onClick,
  tooltip,
  dataTest,
  disabled = false,
  hideOnMobile = false,
}: FilterChipProps) {
  const [hover, setHover] = useState(false)
  const [pinned, setPinned] = useState(false)
  const infoRef = useRef<HTMLDivElement>(null)
  const tooltipId = useId()
  const infoOpen = hover || pinned

  useEffect(() => {
    if (!pinned) return
    const onDocClick = (event: MouseEvent) => {
      if (infoRef.current && !infoRef.current.contains(event.target as Node)) {
        setPinned(false)
      }
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setPinned(false)
    }
    document.addEventListener('mousedown', onDocClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDocClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [pinned])

  return (
    <div
      className={`${hideOnMobile ? 'hidden sm:inline-flex' : 'inline-flex'} items-center rounded-full border text-sm transition-colors select-none ${
        disabled
          ? 'border-gray-200 text-gray-400 bg-gray-50'
          : active
            ? 'border-blue-500 bg-blue-50 text-blue-700 font-medium hover:bg-blue-100'
            : 'border-gray-300 text-gray-600 bg-white hover:border-blue-400 hover:text-blue-600'
      }`}
    >
      <button
        type="button"
        onClick={disabled ? undefined : onClick}
        disabled={disabled}
        aria-pressed={active}
        aria-disabled={disabled}
        data-test={dataTest}
        className={`inline-flex items-center gap-1.5 pl-3 pr-3 sm:pr-1 py-1.5 ${
          disabled ? 'cursor-not-allowed' : 'cursor-pointer'
        }`}
      >
        {active ? (
          <svg className="hidden sm:block w-3.5 h-3.5 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        ) : (
          <PiFunnel className="hidden sm:block w-3.5 h-3.5 text-gray-400 shrink-0" />
        )}
        {label}
      </button>
      <div
        ref={infoRef}
        className="relative hidden sm:block"
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
      >
        <button
          type="button"
          data-test={`${dataTest}-info`}
          aria-label={`About ${label}`}
          aria-expanded={infoOpen}
          aria-controls={tooltipId}
          disabled={disabled}
          onClick={(event) => {
            event.preventDefault()
            event.stopPropagation()
            setPinned((open) => !open)
          }}
          className={`inline-flex items-center pr-3 pl-0.5 py-1.5 ${
            disabled ? 'cursor-not-allowed text-gray-300' : 'cursor-help text-gray-400 hover:text-blue-600'
          }`}
        >
          <PiInfo className="w-3.5 h-3.5" aria-hidden="true" />
        </button>
        {infoOpen && (
          <div
            id={tooltipId}
            role="tooltip"
            data-test={`${dataTest}-tooltip`}
            className="absolute z-30 left-1/2 -translate-x-1/2 top-full mt-1 w-64 rounded-md bg-gray-900 px-3 py-2 text-left text-xs font-normal text-white shadow-lg whitespace-normal"
          >
            {tooltip}
          </div>
        )}
      </div>
    </div>
  )
}
