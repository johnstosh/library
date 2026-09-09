// (c) Copyright 2025 by Muczynski
import { ReadingDifficulty } from '@/types/enums'

/** Enum keys in form order, with Unset last so empty books can be included. */
export const READING_DIFFICULTY_FILTER_VALUES: ReadingDifficulty[] = [
  ReadingDifficulty.CHILDREN,
  ReadingDifficulty.ACCESSIBLE,
  ReadingDifficulty.MODERATE,
  ReadingDifficulty.DEMANDING,
  ReadingDifficulty.ADVANCED,
  ReadingDifficulty.UNSET,
]

/** Chip labels used on Search/Books; Unset matches the book form and view pages. */
export const READING_DIFFICULTY_FILTER_LABELS: Record<ReadingDifficulty, string> = {
  [ReadingDifficulty.CHILDREN]: 'Children',
  [ReadingDifficulty.ACCESSIBLE]: 'Accessible',
  [ReadingDifficulty.MODERATE]: 'Moderate',
  [ReadingDifficulty.DEMANDING]: 'Demanding',
  [ReadingDifficulty.ADVANCED]: 'Advanced',
  [ReadingDifficulty.UNSET]: 'Unset',
}

const KNOWN_KEYS = new Set<string>(READING_DIFFICULTY_FILTER_VALUES)

export function isReadingDifficultyKey(value: string): value is ReadingDifficulty {
  return KNOWN_KEYS.has(value)
}

/** null, blank, and unknown values count as Unset, matching other book pages. */
export function normalizeReadingDifficulty(
  value: string | null | undefined,
): ReadingDifficulty {
  if (!value || value.trim() === '') return ReadingDifficulty.UNSET
  const key = value.trim().toLowerCase()
  return isReadingDifficultyKey(key) ? key : ReadingDifficulty.UNSET
}

export function readingDifficultiesFromSearchParams(params: URLSearchParams): ReadingDifficulty[] {
  const raw = params.get('readingDifficulty')
  if (!raw) return []
  const seen = new Set<ReadingDifficulty>()
  const selected: ReadingDifficulty[] = []
  for (const part of raw.split(',')) {
    const key = part.trim().toLowerCase()
    if (!isReadingDifficultyKey(key) || seen.has(key)) continue
    seen.add(key)
    selected.push(key)
  }
  return selected
}

export function matchesReadingDifficultyFilter(
  value: string | null | undefined,
  selected: string[],
): boolean {
  if (!selected.length) return true
  return selected.includes(normalizeReadingDifficulty(value))
}

export function applyReadingDifficultyFilter<T extends { readingDifficulty?: string | null }>(
  books: T[],
  selected: string[],
): T[] {
  if (!selected.length) return books
  return books.filter((book) => matchesReadingDifficultyFilter(book.readingDifficulty, selected))
}
