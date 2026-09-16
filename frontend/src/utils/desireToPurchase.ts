// (c) Copyright 2025 by Muczynski

/** 0–10 plus Unset, matching the book form. Unset last so empty books can be included. */
export const DESIRE_TO_PURCHASE_VALUES = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10] as const

export const DESIRE_TO_PURCHASE_UNSET = 'unset' as const

export type DesireToPurchaseFilter = (typeof DESIRE_TO_PURCHASE_VALUES)[number] | typeof DESIRE_TO_PURCHASE_UNSET

export const DESIRE_TO_PURCHASE_LABELS: Record<number, string> = {
  0: 'Already own enough',
  1: 'Too expensive',
  2: 'Last resort',
  3: 'Expensive; low priority',
  4: 'Pricey; wait',
  5: 'Fair; medium priority',
  6: 'Good value',
  7: 'Strong buy soon',
  8: 'High priority',
  9: 'Very high priority',
  10: 'First priority',
}

export function desireToPurchaseChipLabel(value: DesireToPurchaseFilter): string {
  if (value === DESIRE_TO_PURCHASE_UNSET) return 'Unset'
  return `${value} — ${DESIRE_TO_PURCHASE_LABELS[value]}`
}

const KNOWN_NUMBERS = new Set<number>(DESIRE_TO_PURCHASE_VALUES)

export function parseDesireToPurchaseFilter(value: string): DesireToPurchaseFilter | null {
  const key = value.trim().toLowerCase()
  if (key === DESIRE_TO_PURCHASE_UNSET) return DESIRE_TO_PURCHASE_UNSET
  if (!/^\d+$/.test(key)) return null
  const n = Number(key)
  return KNOWN_NUMBERS.has(n) ? (n as DesireToPurchaseFilter) : null
}

export function desireToPurchaseFromSearchParams(params: URLSearchParams): DesireToPurchaseFilter[] {
  const raw = params.get('desireToPurchase')
  if (!raw) return []
  const seen = new Set<string>()
  const selected: DesireToPurchaseFilter[] = []
  for (const part of raw.split(',')) {
    const parsed = parseDesireToPurchaseFilter(part)
    if (parsed == null) continue
    const key = String(parsed)
    if (seen.has(key)) continue
    seen.add(key)
    selected.push(parsed)
  }
  return selected
}

export function matchesDesireToPurchaseFilter(
  value: number | null | undefined,
  selected: DesireToPurchaseFilter[],
): boolean {
  if (!selected.length) return true
  if (value == null) return selected.includes(DESIRE_TO_PURCHASE_UNSET)
  return selected.includes(value as DesireToPurchaseFilter)
}

export function applyDesireToPurchaseFilter<T extends { desireToPurchase?: number | null }>(
  books: T[],
  selected: DesireToPurchaseFilter[],
): T[] {
  if (!selected.length) return books
  return books.filter((book) => matchesDesireToPurchaseFilter(book.desireToPurchase, selected))
}
