// (c) Copyright 2025 by Muczynski

/**
 * Canonical author name: given name(s), then family name(s).
 *
 * Twin of CanonicalAuthorName.java. Strips appended birth and death years,
 * turns "Family, Given" around, and expands initials when a parenthetical
 * spells them out. The expanded form drops parentheses, dashes, and periods.
 * An initial with no parenthetical expansion is left as written. A comma
 * inside a phrase ("Sisters of Charity of Our Lady, Mother of the Church",
 * "Ignatius, of Loyola") is left in place. Editor and translator credits
 * are removed and are not stored anywhere else.
 */

const SUFFIXES = new Set([
  'jr', 'sr', 'ii', 'iii', 'iv', 'v', 'vi', 'vii', 'viii', 'ix', 'x',
  'esq', 'phd', 'md', 'op', 'sj', 'osb', 'ofm', 'cssr', 'fr', 'rev', 'dr',
])

const ROMAN_DENY = new Set([
  'mix', 'dix', 'liv', 'mid', 'dim', 'lid', 'did', 'vim', 'mil',
  'civil', 'mill', 'dill', 'livid', 'civic', 'mimic', 'mild',
])

/** Whole words that mark a phrase rather than a family or given name. */
const PHRASE_WORDS = new Set(['of', 'the'])

const YEAR_PREFIX = '(?:(?:b|d|c|ca|fl)\\.?|born|died|circa|floruit)?'
const YEAR_BODY = '\\d{3,4}\\??\\s*(?:[-\\u2013\\u2014]\\s*\\d{0,4}\\??)?'
const ROMAN = /^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$/i

/**
 * An editor or translator role. "ed" does not match "Edith" or "edition";
 * the two-letter forms require the period.
 */
const CREDIT_ROLE =
  'edited\\s+by|editors|editor|translated\\s+by|translated|translators|translator|transl\\.|trans\\.|tr\\.|eds\\.|eds|ed\\.|ed'

/** True when a non-blank name is not already in canonical form. */
export function authorNeedsCanonicalName(name: string | null | undefined): boolean {
  if (name == null || name.trim() === '') return false
  const canonical = toCanonicalAuthorName(name)
  return canonical !== name
}

/** Canonical form. Blank input is returned unchanged. */
export function toCanonicalAuthorName(raw: string | null | undefined): string {
  if (raw == null || raw.trim() === '') return raw ?? ''
  const collapsed = raw.trim().replace(/\s+/g, ' ')
  const yearStripped = stripYears(collapsed)
  if (yearStripped.trim() === '') return raw
  const prepared = stripCredits(yearStripped)
  if (prepared.trim() === '') return raw

  const paren = /\(([^)]*)\)/.exec(prepared)
  const expansion = paren && paren[1].trim() !== '' ? paren[1].trim() : null
  let outside = prepared.replace(/\([^)]*\)/g, ' ')
  outside = outside.trim().replace(/\s+/g, ' ').replace(/\s+,/g, ',').replace(/,\s*/g, ', ')
  outside = outside.replace(/\s+/g, ' ').trim()
  while (outside.endsWith(',')) outside = outside.slice(0, -1).trim()

  const result = expansion != null && containsInitial(outside)
    ? finishExpanded(expansion, outside)
    : invertCommas(prepared)
  if (result.trim() === '') return raw
  return result
}

/** Drops editor and translator credits. Fuller-name parentheticals and edition notes stay. */
function stripCredits(value: string): string {
  let current = value
  let changed = true
  while (changed) {
    changed = false
    let next = current
      .replace(creditParen(), '')
      .replace(creditClause(), '')
      .replace(creditTrail(), '')
      .replace(/\(\s*\)/g, '')
      .replace(/\s+/g, ' ')
      .trim()
      .replace(/\s+([,;])/g, '$1')
      .replace(/([,;])(?!\s)/g, '$1 ')
    while (next.endsWith(',') || next.endsWith(';')) next = next.slice(0, -1).trim()
    if (next !== current) {
      current = next
      changed = true
    }
  }
  return current
}

function creditParen(): RegExp {
  return new RegExp(`\\s*\\(\\s*(?:${CREDIT_ROLE})(?=[\\s).;])[^)]*\\)`, 'gi')
}

function creditClause(): RegExp {
  return new RegExp(`;\\s*(?:${CREDIT_ROLE})(?=[\\s).;])[^);]*`, 'gi')
}

function creditTrail(): RegExp {
  return /(?:\s*[.;])?\s+(?:edited|translated)\s+by\b.*$|,\s*(?:editors?|translators?|trans\.?|transl\.?|tr\.)\s*$/i
}

function parenYear(): RegExp {
  return new RegExp(`\\s*\\(\\s*${YEAR_PREFIX}\\s*${YEAR_BODY}\\s*\\)\\s*`, 'gi')
}

function commaYear(): RegExp {
  return new RegExp(`\\s*,\\s*${YEAR_PREFIX}\\s*${YEAR_BODY}\\s*$`, 'i')
}

function stripYears(value: string): string {
  let current = value
  let changed = true
  while (changed) {
    changed = false
    let next = current.replace(parenYear(), ' ').replace(commaYear(), '')
    next = next.replace(/\s+/g, ' ').trim()
    while (next.endsWith(',')) next = next.slice(0, -1).trim()
    if (next !== current) {
      current = next
      changed = true
    }
  }
  return current
}

function finishExpanded(expansion: string, outside: string): string {
  const family = cleanExpanded(familyName(outside))
  let cleaned = cleanExpanded(expansion)
  if (family !== '' && !containsPhrase(cleaned, family)) {
    cleaned = `${cleaned} ${family}`.trim()
  }
  return cleaned.replace(/\s+/g, ' ').trim()
}

function familyName(outside: string): string {
  const comma = outside.indexOf(',')
  if (comma >= 0) return outside.slice(0, comma).trim()
  const tokens = outside.split(/\s+/)
  for (let i = tokens.length - 1; i >= 0; i--) {
    if (tokens[i] !== '' && !isInitialToken(tokens[i])) return tokens[i]
  }
  return ''
}

function cleanExpanded(value: string): string {
  return value
    .replace(/[\u2013\u2014-]/g, ' ')
    .replace(/[.()]/g, '')
    .trim()
    .replace(/\s+/g, ' ')
}

function containsPhrase(name: string, phrase: string): boolean {
  const haystack = ` ${name.toLowerCase().replace(/\s+/g, ' ')} `
  const needle = ` ${phrase.toLowerCase().replace(/\s+/g, ' ').trim()} `
  return haystack.includes(needle)
}

function containsInitial(outside: string): boolean {
  if (outside.trim() === '') return false
  return outside.split(/\s+/).some((token) => isInitialToken(token.replace(/^,+|,+$/g, '')))
}

function isInitialToken(token: string): boolean {
  if (token.trim() === '') return false
  const pieces = token.split(/[.\-]+/)
  let any = false
  for (const piece of pieces) {
    if (piece === '') continue
    if (!/^[A-Za-z]$/.test(piece)) return false
    any = true
  }
  return any
}

function invertCommas(value: string): string {
  let collapsed = value.trim().replace(/\s+/g, ' ').replace(/\s*,\s*/g, ', ').trim()
  while (collapsed.endsWith(',')) collapsed = collapsed.slice(0, -1).trim()
  if (!collapsed.includes(',')) return collapsed
  const parts = collapsed.split(',').map((part) => part.trim()).filter((part) => part !== '')
  if (parts.length < 2) return collapsed
  if (parts.length === 2 && isSuffix(parts[1])) return `${parts[0]} ${parts[1]}`
  for (let i = 2; i < parts.length; i++) {
    if (!isSuffix(parts[i])) return collapsed
  }
  if (isPhrase(parts[0]) || isPhrase(parts[1])) return collapsed
  return [parts[1], parts[0], ...parts.slice(2)].join(' ').replace(/\s+/g, ' ').trim()
}

/** A comma-joined phrase such as "Mother of the Church" or "of Loyola". */
function isPhrase(part: string): boolean {
  return part.split(/\s+/).some((token) => {
    const key = token.replace(/^[^\p{L}]+|[^\p{L}]+$/gu, '').toLowerCase()
    return PHRASE_WORDS.has(key)
  })
}

function isSuffix(part: string): boolean {
  const key = part.replace(/\./g, '').replace(/ /g, '').toLowerCase()
  if (SUFFIXES.has(key)) return true
  if (ROMAN_DENY.has(key) || key.length < 2 || key.length > 6) return false
  return ROMAN.test(key)
}
