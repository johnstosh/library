// (c) Copyright 2025 by Muczynski

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
] as const

/**
 * A feast day is a month and day. The API sends `--MM-DD` (or, if a sentinel
 * year leaked, `YYYY-MM-DD`). Neither form is shown with a year.
 */
export function formatFeastDay(value?: string | null): string {
  const parsed = parseFeastDay(value)
  if (!parsed) return ''
  return `${MONTHS[parsed.month - 1]} ${parsed.day}`
}

/** Wire value `--MM-DD`, or undefined when the librarian left the field blank. */
export function feastDayToWire(display: string): string | undefined {
  const trimmed = display.trim()
  if (!trimmed) return undefined
  const parsed = parseFeastDay(trimmed)
  if (!parsed) {
    throw new Error('Enter a month and day, such as August 11. Do not include a year.')
  }
  const month = String(parsed.month).padStart(2, '0')
  const day = String(parsed.day).padStart(2, '0')
  return `--${month}-${day}`
}

function parseFeastDay(value?: string | null): { month: number; day: number } | null {
  if (!value) return null
  const trimmed = value.trim()
  if (!trimmed) return null

  const iso = trimmed.match(/^(?:--|(\d{4})-)(\d{2})-(\d{2})$/)
  if (iso) {
    return valid(Number(iso[2]), Number(iso[3]))
  }

  const named = trimmed.match(/^([A-Za-z]+)\s+(\d{1,2})$/)
  if (!named) return null
  const month = MONTHS.findIndex((name) => name.toLowerCase() === named[1].toLowerCase()) + 1
  return valid(month, Number(named[2]))
}

/** February 29 is allowed. Year 2000 is only used here so that day exists. */
function valid(month: number, day: number): { month: number; day: number } | null {
  if (month < 1 || month > 12) return null
  const date = new Date(2000, month - 1, day)
  if (date.getFullYear() !== 2000 || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return { month, day }
}
