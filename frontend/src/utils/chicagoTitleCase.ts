// (c) Copyright 2025 by Muczynski

/**
 * Chicago Manual of Style headline-style title case for a whole title,
 * including the subtitle after a colon and the sentence after a period,
 * question mark, or exclamation point.
 *
 * Twin of ChicagoTitleCase.java. Major words are capitalized, the first and
 * last word of the title, of each subtitle, and of each sentence are
 * capitalized, and short function words stay lower elsewhere. A period after
 * an initial (J.) or an abbreviation (St., U.S.) does not start a new sentence.
 */

const SMALL = new Set([
  'a', 'an', 'the',
  'and', 'but', 'or', 'nor', 'for',
  'to', 'as',
  'of', 'in', 'on', 'at', 'by', 'from', 'with', 'into', 'onto', 'upon',
  'over', 'under', 'about', 'after', 'before', 'between', 'through',
  'during', 'without', 'within', 'against', 'among', 'around', 'across',
  'behind', 'beyond', 'despite', 'except', 'toward', 'towards', 'until',
  'via', 'per', 'vs', 'versus', 'amid', 'amongst', 'beside', 'besides',
  'concerning', 'regarding', 'unlike', 'near', 'off', 'out', 'up', 'down',
])

const ROMAN_DENY = new Set([
  'mix', 'dix', 'liv', 'mid', 'dim', 'lid', 'did', 'vim', 'mil',
  'civil', 'mill', 'dill', 'livid', 'civic', 'mimic', 'mild',
])

/** One-word abbreviations. Their period does not start a new sentence. */
const ABBREV = new Set([
  'st', 'ste', 'mr', 'mrs', 'ms', 'mss', 'dr', 'jr', 'sr',
  'fr', 'br', 'mt', 'ft', 'gen', 'col', 'capt', 'sgt', 'lt',
  'prof', 'rev', 'hon', 'pres', 'sen', 'gov', 'rep',
  'vol', 'vols', 'ed', 'eds', 'no', 'nos', 'pp', 'ch', 'chap', 'chaps',
  'fig', 'figs', 'etc', 'al', 'cf', 'vs', 'op', 'cit', 'ibid', 'viz',
  'trans', 'pt', 'pts', 'ser', 'pl', 'pls', 'inc', 'ltd', 'co', 'corp',
  'approx', 'esp', 'ca', 'bp', 'abp', 'ven', 'bl', 'messrs', 'mme', 'mlle',
])

const COPY_SUFFIX = /,\s*c\.?\s*(\d+)\s*$/i
const ROMAN = /^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$/i

/** True when a non-blank title is not already in Chicago title case. */
export function titleNeedsChicagoCase(title: string | null | undefined): boolean {
  if (title == null || title.trim() === '') return false
  const chicago = toChicagoTitleCase(title)
  return chicago != null && chicago !== title
}

/**
 * Rewrites the whole title, including a subtitle after a colon and each
 * sentence after a period, question mark, or exclamation point.
 * A trailing catalog copy suffix (", c. N") is kept and normalized.
 */
export function toChicagoTitleCase(title: string | null | undefined): string {
  if (title == null || title.trim() === '') return title ?? ''
  const trimmed = title.trim().replace(/\s+/g, ' ')
  const suffix = COPY_SUFFIX.exec(trimmed)
  let main = trimmed
  let copy = ''
  if (suffix && suffix.index > 0) {
    main = trimmed.slice(0, suffix.index).trim()
    copy = `, c. ${suffix[1]}`
    if (main === '') return title
  }

  const segments = main.split(/\s*:\s*/)
  const parts: string[] = []
  for (const segment of segments) {
    const cased = titleCaseSegment(segment)
    if (cased !== '') parts.push(cased)
  }
  const result = `${parts.join(': ')}${copy}`.trim()
  return result === '' ? title : result
}

function titleCaseSegment(segment: string): string {
  if (segment.trim() === '') return ''
  const words = segment.trim().split(/\s+/)
  return words
    .map((word, index) => {
      const edge = index === 0 || index === words.length - 1
      const sentenceStart = index > 0 && endsSentence(words[index - 1])
      const sentenceEnd = endsSentence(word)
      return titleCaseWord(word, edge, sentenceStart || sentenceEnd)
    })
    .join(' ')
}

function titleCaseWord(word: string, edge: boolean, sentenceEdge: boolean): string {
  const parts = word.split('-')
  if (parts.length === 1) return titleCaseToken(word, edge || sentenceEdge)
  return parts
    .map((part, index) => {
      const last = index === parts.length - 1
      const force = index === 0 || (edge && last) || (sentenceEdge && endsSentence(word) && last)
      return titleCaseToken(part, force)
    })
    .join('-')
}

/** True when this token ends a sentence, so the next word is capitalized. */
function endsSentence(token: string): boolean {
  const stripped = token.replace(/["'”’)\]}]+$/u, '')
  if (
    stripped.endsWith('?') ||
    stripped.endsWith('!') ||
    stripped.endsWith('…') ||
    stripped.endsWith('...')
  ) {
    return true
  }
  if (!stripped.endsWith('.')) return false
  const body = stripped.slice(0, -1)
  if (body === '' || isInitialism(body) || /^[A-Za-z]$/.test(body)) return false
  const core = body
    .replace(/^[^\p{L}\p{N}]+/u, '')
    .replace(/[^\p{L}\p{N}]+$/u, '')
    .toLowerCase()
  return !ABBREV.has(core)
}

/** U.S or Ph.D: two or more short letter groups. */
function isInitialism(body: string): boolean {
  if (!body.includes('.')) return false
  const parts = body.split('.')
  let groups = 0
  for (const part of parts) {
    if (part === '') continue
    if (!/^[A-Za-z]{1,2}$/.test(part)) return false
    groups++
  }
  return groups >= 2
}

function titleCaseToken(token: string, force: boolean): string {
  if (token === '') return token
  if (isRomanNumeral(token)) return uppercaseLetters(token)
  const comparable = comparableWord(token)
  if (!force && SMALL.has(comparable)) return lowercaseLetters(token)
  if (isDottedAbbreviation(token)) return formatDotted(token)
  return capitalizeLike(token)
}

function isRomanNumeral(token: string): boolean {
  const core = token.replace(/^[^\p{L}]+/u, '').replace(/[^\p{L}]+$/u, '')
  if (core === '' || !/^[ivxlcdm]+$/i.test(core)) return false
  if (ROMAN_DENY.has(core.toLowerCase())) return false
  return ROMAN.test(core)
}

function uppercaseLetters(token: string): string {
  return Array.from(token)
    .map((c) => (/\p{L}/u.test(c) ? c.toUpperCase() : c))
    .join('')
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
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join('.')
  return trailingDot ? `${body}.` : body
}

function comparableWord(token: string): string {
  return token
    .replace(/^[^\p{L}\p{N}]+/u, '')
    .replace(/[^\p{L}\p{N}]+$/u, '')
    .toLowerCase()
}

function lowercaseLetters(token: string): string {
  return Array.from(token)
    .map((c) => (/\p{L}/u.test(c) ? c.toLowerCase() : c))
    .join('')
}

function capitalizeLike(token: string): string {
  let capNext = true
  let capAfterApostrophe = false
  let lettersSeen = 0
  let out = ''
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
