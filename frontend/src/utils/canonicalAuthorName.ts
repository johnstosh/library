// (c) Copyright 2025 by Muczynski

/**
 * Canonical author name: given name(s), then family name(s).
 *
 * Twin of CanonicalAuthorName.java. Strips appended birth and death years
 * (including "approximately" and a year phrase moved in front of the name),
 * turns "Family, Given" around, and expands initials when a parenthetical
 * spells them out. A two-letter abbreviation such as "Wm." or "L.-Cl."
 * counts as an initial. A parenthetical that is only initials ("C. C.")
 * is removed. The expanded form drops parentheses, dashes, and periods.
 * An initial with no parenthetical expansion is left as written. A comma
 * inside a phrase ("Sisters of Charity of Our Lady, Mother of the Church",
 * "Ignatius, of Loyola") is left in place. Editor and translator credits
 * are removed and are not stored anywhere else. "[from old catalog]" is
 * removed. UTF-8 letters that were read as Latin-1 are repaired.
 * A dotted abbreviation that is a prefix of the parenthetical name is
 * expanded ("Joh. Evang." becomes "Johannes Evangelist"). A shorter copy
 * of the same name is dropped. "Thomas, à Kempis" stays "Thomas à Kempis".
 * A "tr." relator is removed as a translator credit. A name whose letters
 * are all capitals or all lowercase is
 * rewritten with initial capitals; particles such as "von" and "de la"
 * stay lower. A no-space run of two to six capital letters is a
 * postnominal suffix ("OCD", "D.D."). Spaced initials such as "C. L"
 * stay in the given-name slot.
 */

const SUFFIXES = new Set([
  'jr', 'sr', 'ii', 'iii', 'iv', 'v', 'vi', 'vii', 'viii', 'ix', 'x',
  'esq', 'phd', 'md', 'op', 'sj', 'osb', 'ofm', 'cssr', 'osa', 'slg', 'cssp', 'opraem',
  'fr', 'rev', 'dr', 'inc', 'llc',
])

const ROMAN_DENY = new Set([
  'mix', 'dix', 'liv', 'mid', 'dim', 'lid', 'did', 'vim', 'mil',
  'civil', 'mill', 'dill', 'livid', 'civic', 'mimic', 'mild',
])

/** Whole words that mark a phrase rather than a family or given name. */
const PHRASE_WORDS = new Set(['of', 'the'])

/**
 * Titles and saint-abbreviations. "St." is not a given-name initial,
 * so a parenthetical fuller name stays.
 */
const TITLE_ABBREV = new Set([
  'st', 'fr', 'dr', 'mr', 'ms', 'mrs', 'sr', 'jr', 'bp', 'br', 'mt', 'rt', 'ss', 'mm',
])

/**
 * Postnominals that stay capital when an all-caps or all-lower name is
 * recased. Word suffixes such as "Jr." and "Rev." are not here.
 */
const KEEP_UPPER = new Set([
  'op', 'sj', 'osb', 'ofm', 'cssr', 'osa', 'slg', 'cssp', 'opraem',
  'llc', 'phd', 'md', 'ocd', 'cp', 'mic', 'bvm', 'dd', 'ss', 'mm',
])

/** A second comma-part that begins with one of these is another person, not a given name. */
const CLERICAL_TITLES = new Set([
  'rev', 'reverend', 'rt', 'fr', 'father', 'cardinal', 'archbishop', 'bishop',
  'abbot', 'pope', 'sister', 'brother', 'dom', 'monsignor', 'msgr',
])

/** Particles and the few function words that stay lower inside a personal name. */
const NAME_SMALL = new Set([
  'and', 'or', 'of', 'the',
  'de', 'di', 'da', 'du', 'van', 'von', 'del', 'della', 'den', 'der', 'ten', 'ter', 'y',
])

/** Two-letter regnal numbers. "Cl." is Claude, not 150. */
const REGNAL_TWO = new Set(['ii', 'iv', 'vi', 'ix', 'xi', 'xv', 'xx'])

const YEAR_PREFIX = '(?:approximately|approx\\.?|circa|floruit|born|died|(?:b|d|c|ca|fl)\\.?)?'
const YEAR_QUALIFIER = '(?:approximately|approx\\.?|circa|floruit|born|died|(?:ca|fl|b|d|c)\\.)'
const YEAR_BODY = '\\d{3,4}\\??\\s*(?:[-\\u2013\\u2014]\\s*\\d{0,4}\\??)?'
const ROMAN = /^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$/i
const ROMAN_TOKEN = /^(\W*)([ivxlcdm]+)(\W*)$/i
const FRENCH_PARTICLE = /^(\W*)([dln])(['’])(\p{L})(.*)$/iu
const MC_NAME = /^(\W*)Mc(\p{L})(.*)$/iu
/** Single-letter initials with a space, such as "S. J." after a given name. */
const SPACED_INITIALS = /^(?:[A-Za-z]\.\s+)+[A-Za-z]\.?$/

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
  let collapsed = raw.trim().replace(/\s+/g, ' ').normalize('NFC')
  collapsed = repairMojibake(collapsed).normalize('NFC')
  collapsed = stripCatalogNote(collapsed)
  collapsed = stripStrayBrackets(collapsed)
  if (collapsed.trim() === '') return raw
  const yearStripped = stripYears(collapsed)
  if (yearStripped.trim() === '') return raw
  const kempis = thomasAKempis(yearStripped)
  if (kempis != null) return kempis
  const prepared = stripCredits(yearStripped)
  if (prepared.trim() === '') return raw

  const paren = /\(([^)]*)\)/.exec(prepared)
  const expansion = paren && paren[1].trim() !== '' ? paren[1].trim() : null
  let outside = prepared.replace(/\([^)]*\)/g, ' ')
  outside = outside.trim().replace(/\s+/g, ' ').replace(/\s+,/g, ',').replace(/,\s*/g, ', ')
  outside = outside.replace(/\s+/g, ' ').trim()
  while (outside.endsWith(',')) outside = outside.slice(0, -1).trim()

  const result = expansion != null && isRedundantInitials(expansion)
    ? invertCommas(outside)
    : expansion != null && (containsInitial(outside) || abbreviatesParenthetical(outside, expansion))
      ? finishExpanded(expansion, outside)
      : invertCommas(prepared)
  if (result.trim() === '') return raw
  return normalizeUniformCase(result)
}

/** Drops a square bracket left over from a catalog heading, such as "Joachim]". */
function stripStrayBrackets(value: string): string {
  if (!value.includes('[') && !value.includes(']')) return value
  return value.replace(/[[\]]/g, '').replace(/\s+/g, ' ').trim()
}

/**
 * Repairs UTF-8 text that was read as Latin-1. "é" stored as U+00C3 U+00A9
 * becomes "é" again. A name that is already Unicode is left alone.
 */
function repairMojibake(value: string): string {
  let out = ''
  let changed = false
  let i = 0
  while (i < value.length) {
    const cp = value.charCodeAt(i)
    if ((cp === 0xc2 || cp === 0xc3) && i + 1 < value.length) {
      const next = value.charCodeAt(i + 1)
      if (next >= 0x80 && next <= 0xbf) {
        const decoded = decodeUtf8([cp, next])
        if (decoded != null && [...decoded].length === 1) {
          out += decoded
          i += 2
          changed = true
          continue
        }
      }
    }
    if (cp >= 0xe0 && cp <= 0xef && i + 2 < value.length) {
      const n1 = value.charCodeAt(i + 1)
      const n2 = value.charCodeAt(i + 2)
      if (n1 >= 0x80 && n1 <= 0xbf && n2 >= 0x80 && n2 <= 0xbf) {
        const decoded = decodeUtf8([cp, n1, n2])
        if (decoded != null && [...decoded].length === 1) {
          out += decoded
          i += 3
          changed = true
          continue
        }
      }
    }
    out += value[i]
    i++
  }
  return changed ? out : value
}

function decodeUtf8(bytes: number[]): string | null {
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes))
  } catch {
    return null
  }
}

/** "Thomas, à Kempis" and the inverted "à Kempis Thomas" are the same name. */
function thomasAKempis(value: string): string | null {
  const key = value.toLowerCase().replace(/,/g, ' ').replace(/\s+/g, ' ').trim()
  if (key === 'thomas à kempis' || key === 'à kempis thomas' || key === 'thomas a kempis' || key === 'a kempis thomas') {
    return 'Thomas à Kempis'
  }
  return null
}

/** Drops a Library of Congress "[from old catalog]" note. */
function stripCatalogNote(value: string): string {
  let next = value.replace(/\s*\[\s*from\s+old\s+catalog\s*\]/gi, '')
  next = next.replace(/\s+/g, ' ').trim()
  while (next.endsWith(',')) next = next.slice(0, -1).trim()
  return next
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
  return /(?:\s*[.;])?\s+(?:edited|translated)\s+by\b.*$|,\s*(?:editors?|translators?|trans\.?|transl\.?|tr\.)\s*$|\btr\.(?=\s|$)/i
}

function parenYear(): RegExp {
  return new RegExp(`\\s*\\(\\s*${YEAR_PREFIX}\\s*${YEAR_BODY}\\s*\\)\\s*`, 'gi')
}

function commaYear(): RegExp {
  return new RegExp(`\\s*,\\s*${YEAR_PREFIX}\\s*${YEAR_BODY}\\s*$`, 'i')
}

function leadingYear(): RegExp {
  return new RegExp(`^${YEAR_QUALIFIER}\\s+${YEAR_BODY}\\s+`, 'i')
}

function trailingYear(): RegExp {
  return new RegExp(`\\s+${YEAR_QUALIFIER}\\s+${YEAR_BODY}\\s*$`, 'i')
}

function stripYears(value: string): string {
  let current = value
  let changed = true
  while (changed) {
    changed = false
    let next = current.replace(parenYear(), ' ').replace(commaYear(), '').replace(trailingYear(), '').replace(leadingYear(), '')
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

/**
 * One- or two-letter pieces separated by periods or hyphens, such as
 * "B.", "J.-P.", "Wm.", or "L.-Cl.". A spelled name is not an initial.
 * "St." and "Ph.D." are not given-name initials. A two-letter piece needs
 * a period, and a degree such as "LL.D." is not one.
 */
function isInitialToken(token: string): boolean {
  if (token.trim() === '') return false
  const pieces = token.split(/[.\-]+/)
  let any = false
  let two = false
  for (const piece of pieces) {
    if (piece === '') continue
    if (!/^[A-Za-z]{1,2}$/.test(piece)) return false
    if (piece.length === 2) two = true
    any = true
  }
  if (!any) return false
  if (!two) return true
  if (!token.includes('.') || isSuffix(token)) return false
  const key = token.replace(/[.\-]/g, '').toLowerCase()
  if (TITLE_ABBREV.has(key)) return false
  return token.includes('-') || key.length === 2
}

/**
 * Parenthetical given-name initials, such as "C. C." or "Wm.".
 * A degree or religious postnominal ("D.D.", "S.J.", "OP") stays.
 */
function isRedundantInitials(value: string): boolean {
  const trimmed = value.trim()
  if (trimmed === '') return false
  const key = trimmed.replace(/[.\-\s]/g, '').toLowerCase()
  if (key === '' || SUFFIXES.has(key) || KEEP_UPPER.has(key)) return false
  return trimmed.split(/\s+/).every((token) => isInitialToken(token))
}

/**
 * "Joh. Evang." abbreviates "Johannes Evangelist": every dotted token is a
 * shorter prefix of the matching parenthetical word.
 */
function abbreviatesParenthetical(outside: string, expansion: string): boolean {
  const comma = outside.indexOf(',')
  if (comma < 0) return false
  const given = outside.slice(comma + 1).trim()
  if (given === '') return false
  const givenTokens = given.split(/\s+/)
  const expanded = expansion.trim().split(/\s+/)
  if (givenTokens.length === 0 || givenTokens.length !== expanded.length) return false
  for (let i = 0; i < givenTokens.length; i++) {
    const token = givenTokens[i].replace(/^,+|,+$/g, '')
    if (!token.endsWith('.')) return false
    const prefix = token.replace(/[^\p{L}]/gu, '')
    const full = expanded[i].replace(/[^\p{L}]/gu, '')
    if (prefix === '' || prefix.length >= full.length) return false
    if (!full.toLowerCase().startsWith(prefix.toLowerCase())) return false
  }
  return true
}

function invertCommas(value: string): string {
  let collapsed = value.trim().replace(/\s+/g, ' ').replace(/\s*,\s*/g, ', ').trim()
  while (collapsed.endsWith(',')) collapsed = collapsed.slice(0, -1).trim()
  if (!collapsed.includes(',')) return collapsed
  const parts = collapsed.split(',').map((part) => part.trim()).filter((part) => part !== '')
  if (parts.length < 2) return collapsed
  if (parts.length === 2 && isSuffix(parts[1])) return `${parts[0]} ${parts[1]}`
  if (parts.length === 2) {
    const longer = longerSameName(parts[0], parts[1])
    if (longer != null) return longer
    if (isAnotherPerson(parts[0], parts[1])) return collapsed
  }
  for (let i = 2; i < parts.length; i++) {
    if (!isSuffix(parts[i]) && !isSpacedInitials(parts[i])) return collapsed
  }
  if (isPhrase(parts[0]) || isPhrase(parts[1])) return collapsed
  return [parts[1], parts[0], ...parts.slice(2)].join(' ').replace(/\s+/g, ' ').trim()
}

/**
 * "Joseph Hergenröther, Joseph Adam Gustav Hergenröther" is one person
 * written twice. Keep the longer form when one side is an ordered subset
 * of the other and both end with the same surname.
 */
function longerSameName(left: string, right: string): string | null {
  const a = left.trim().split(/\s+/).filter((word) => word !== '')
  const b = right.trim().split(/\s+/).filter((word) => word !== '')
  if (a.length < 2 || b.length < 2) return null
  if (wordKey(a[a.length - 1]) !== wordKey(b[b.length - 1])) return null
  if (a.length < b.length && isOrderedSubsequence(a, b)) return right
  if (b.length < a.length && isOrderedSubsequence(b, a)) return left
  return null
}

function isOrderedSubsequence(shorter: string[], longer: string[]): boolean {
  let found = 0
  for (const word of longer) {
    if (found < shorter.length && wordKey(word) === wordKey(shorter[found])) found++
  }
  return found === shorter.length
}

/**
 * "John Baptist Scaramelli, Rev. Cardinal Archbishop Manning" names two
 * people. A clerical title on the second side is not a given name.
 */
function isAnotherPerson(left: string, right: string): boolean {
  const a = left.trim().split(/\s+/).filter((word) => word !== '')
  const b = right.trim().split(/\s+/).filter((word) => word !== '')
  if (a.length < 2 || b.length < 2) return false
  if (!CLERICAL_TITLES.has(wordKey(b[0]))) return false
  return wordKey(a[a.length - 1]) !== wordKey(b[b.length - 1])
}

/** A comma-joined phrase such as "Mother of the Church" or "of Loyola". */
function isPhrase(part: string): boolean {
  return part.split(/\s+/).some((token) => {
    const key = token.replace(/^[^\p{L}]+|[^\p{L}]+$/gu, '').toLowerCase()
    return PHRASE_WORDS.has(key)
  })
}

/** No-space all-caps postnominal: SJ, S.J., OCD, D.D. A lowercase letter keeps Joseph out. Inc is listed separately. */
function isPostnominalInitialism(part: string): boolean {
  let letters = 0
  for (const c of part) {
    if (/\p{L}/u.test(c)) {
      if (c !== c.toUpperCase()) return false
      letters++
    } else if (c !== '.') {
      return false
    }
  }
  return letters >= 2 && letters <= 6
}

/** "S. J." after a given name. Spaced initials in the given-name slot are not suffixes. */
function isSpacedInitials(part: string): boolean {
  return SPACED_INITIALS.test(part)
}

function isSuffix(part: string): boolean {
  if (/\s/.test(part)) return false
  const key = part.replace(/\./g, '').toLowerCase()
  if (SUFFIXES.has(key)) return true
  if (isPostnominalInitialism(part)) return true
  if (ROMAN_DENY.has(key) || key.length < 2 || key.length > 6) return false
  return ROMAN.test(key)
}

/**
 * Initial capitals for a name whose letters are all upper or all lower.
 * A mixed-case name is returned unchanged, so "McFadden" and "de la" stay as stored.
 */
function normalizeUniformCase(value: string): string {
  if (!isUniformLetterCase(value)) return value
  const tokens = value.split(' ')
  let previous = ''
  const cased = tokens.map((token, index) => {
    const next = caseToken(token, index === 0, previous)
    previous = wordKey(token)
    return next
  })
  return cased.join(' ')
}

function isUniformLetterCase(value: string): boolean {
  let upper: boolean | null = null
  for (const c of value) {
    if (!/\p{L}/u.test(c)) continue
    const isUpper = c === c.toUpperCase() && c !== c.toLowerCase()
    const isLower = c === c.toLowerCase() && c !== c.toUpperCase()
    if (!isUpper && !isLower) continue
    if (upper == null) upper = isUpper
    else if (upper !== isUpper) return false
  }
  return upper != null
}

function caseToken(token: string, first: boolean, previous: string): string {
  if (!token.includes('-')) return caseTokenPart(token, first, previous)
  const parts = token.split('-')
  let prev = previous
  const cased = parts.map((part, index) => {
    const next = caseTokenPart(part, first && index === 0, prev)
    prev = wordKey(part)
    return next
  })
  return cased.join('-')
}

function caseTokenPart(token: string, first: boolean, previous: string): string {
  if (token === '') return token
  const roman = romanNameToken(token)
  if (roman != null) return roman
  const dotted = formatWrappedDotted(token)
  if (dotted != null) return dotted
  const french = frenchParticle(token)
  if (french != null) return french
  const key = wordKey(token)
  if (KEEP_UPPER.has(key)) return uppercaseLetters(token)
  if (!first && isNameSmall(key, previous)) return lowercaseLetters(token)
  return fixMc(capitalizeLike(token))
}

/** "XIV" and "IV." stay numerals. "Cl." does not. */
function romanNameToken(token: string): string | null {
  const match = ROMAN_TOKEN.exec(token)
  if (!match) return null
  const key = match[2].toLowerCase()
  if (key.length < 2 || ROMAN_DENY.has(key) || !ROMAN.test(key)) return null
  if (key.length === 2 && match[3].includes('.') && !REGNAL_TWO.has(key)) return null
  return `${match[1]}${key.toUpperCase()}${match[3]}`
}

function isNameSmall(key: string, previous: string): boolean {
  if (NAME_SMALL.has(key)) return true
  return (key === 'la' || key === 'le') && previous === 'de'
}

/** "D.D.," keeps the abbreviation and the punctuation around it. */
function formatWrappedDotted(token: string): string | null {
  const match = /^([^A-Za-z.]*)([A-Za-z][A-Za-z.]*)([^A-Za-z.]*)$/.exec(token)
  if (!match || !match[2].includes('.') || !isDottedAbbreviation(match[2])) return null
  return match[1] + formatDotted(match[2]) + match[3]
}

function isDottedAbbreviation(token: string): boolean {
  if (!token.includes('.')) return false
  const parts = token.split('.')
  let any = false
  for (const part of parts) {
    if (part === '') continue
    if (!/^[A-Za-z]{1,2}$/.test(part)) return false
    any = true
  }
  return any
}

function formatDotted(token: string): string {
  const trailingDot = token.endsWith('.')
  const parts = token.split('.').filter((part) => part !== '')
  const body = parts
    .map((part) => {
      if (part.length === 2 && part.charAt(0).toLowerCase() === part.charAt(1).toLowerCase()) {
        return part.toUpperCase()
      }
      return part.charAt(0).toUpperCase() + part.slice(1).toLowerCase()
    })
    .join('.')
  return trailingDot ? `${body}.` : body
}

/** "d'Arc". The particle stays lower. */
function frenchParticle(token: string): string | null {
  const match = FRENCH_PARTICLE.exec(token)
  if (!match) return null
  return `${match[1]}${match[2].toLowerCase()}${match[3]}${match[4].toUpperCase()}${match[5].toLowerCase()}`
}

function fixMc(token: string): string {
  const match = MC_NAME.exec(token)
  if (!match) return token
  return `${match[1]}Mc${match[2].toUpperCase()}${match[3]}`
}

function uppercaseLetters(token: string): string {
  return Array.from(token)
    .map((c) => (/\p{L}/u.test(c) ? c.toUpperCase() : c))
    .join('')
}

function lowercaseLetters(token: string): string {
  return Array.from(token)
    .map((c) => (/\p{L}/u.test(c) ? c.toLowerCase() : c))
    .join('')
}

/** Capitalizes the first letter and the letter after a one-letter apostrophe ("O'Brien"). */
function capitalizeLike(token: string): string {
  let out = ''
  let capNext = true
  let capAfterApostrophe = false
  let lettersSeen = 0
  for (const c of token) {
    if (/\p{L}/u.test(c)) {
      if (capNext || capAfterApostrophe) {
        out += c.toUpperCase()
        capNext = false
        capAfterApostrophe = false
      } else {
        out += c.toLowerCase()
      }
      lettersSeen++
    } else {
      out += c
      if (c === "'" && lettersSeen === 1) capAfterApostrophe = true
    }
  }
  return out
}

function wordKey(token: string): string {
  return token
    .replace(/^[^\p{L}\p{N}]+/u, '')
    .replace(/[^\p{L}\p{N}]+$/u, '')
    .toLowerCase()
}
