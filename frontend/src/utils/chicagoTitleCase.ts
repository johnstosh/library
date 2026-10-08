// (c) Copyright 2025 by Muczynski

/**
 * Chicago Manual of Style headline-style title case for a whole title,
 * including the subtitle after a colon, semicolon, or slash, and the sentence
 * after a period, question mark, or exclamation point.
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
  'de', 'di', 'da', 'du', 'van', 'von',
])

const ROMAN_DENY = new Set([
  'mix', 'dix', 'liv', 'mid', 'dim', 'lid', 'did', 'vim', 'mil',
  'civil', 'mill', 'dill', 'livid', 'civic', 'mimic', 'mild',
  'di',
])

/** One-word abbreviations. Their period does not start a new sentence. */
const ABBREV = new Set([
  'st', 'ste', 'mr', 'mrs', 'ms', 'mss', 'dr', 'jr', 'sr',
  'fr', 'br', 'mt', 'ft', 'gen', 'col', 'capt', 'sgt', 'lt',
  'prof', 'rev', 'hon', 'pres', 'sen', 'gov', 'rep',
  'vol', 'vols', 'ed', 'eds', 'no', 'nos', 'pp', 'ch', 'chap', 'chaps',
  'fig', 'figs', 'al', 'cf', 'vs', 'op', 'cit', 'ibid', 'viz',
  'trans', 'pt', 'pts', 'ser', 'pl', 'pls', 'inc', 'ltd', 'co', 'corp',
  'approx', 'esp', 'ca', 'bp', 'abp', 'ven', 'bl', 'messrs', 'mme', 'mlle',
])

/** Short all-caps tokens that are names, not words to recase. */
const ACRONYM = new Set([
  'abc', 'youcat', 'html', 'xhtml', 'css', 'sql', 'dvd', 'cd', 'tv', 'pdf', 'isbn',
])

const COPY_SUFFIX = /,\s*c\.?\s*(\d+)\s*$/i
const ROMAN = /^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$/i
const SEGMENT_SEP = /\s*([:;])\s*|\s+(\/)\s+/g
const NUMBER_SUFFIX = /^(\W*)(\d+)([A-Za-z]+)(\W*)$/
const ROMAN_TOKEN = /^(\W*)([ivxlcdm]+)(['’]s)?(\W*)$/i
const FRENCH_PARTICLE = /^(\W*)([dln])(['’])(\p{L})(.*)$/iu
const MC_NAME = /^(\W*)Mc(\p{L})(.*)$/iu

/** True when a non-blank title is not already in Chicago title case. */
export function titleNeedsChicagoCase(title: string | null | undefined): boolean {
  if (title == null || title.trim() === '') return false
  const chicago = toChicagoTitleCase(title)
  return chicago != null && chicago !== title
}

/**
 * Rewrites the whole title, including a subtitle after a colon, semicolon,
 * or slash, and each sentence after a period, question mark, or exclamation
 * point. A trailing catalog copy suffix (", c. N") is kept and normalized.
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

  const parts: string[] = []
  const marks: string[] = []
  let cursor = 0
  for (const match of main.matchAll(SEGMENT_SEP)) {
    parts.push(main.slice(cursor, match.index))
    marks.push(match[1] != null ? `${match[1]} ` : ' / ')
    cursor = (match.index ?? 0) + match[0].length
  }
  parts.push(main.slice(cursor))

  let out = ''
  for (let i = 0; i < parts.length; i++) {
    const cased = titleCaseSegment(parts[i])
    if (cased === '') continue
    if (out !== '') out += marks[i - 1]
    out += cased
  }
  const result = `${out}${copy}`.trim()
  return result === '' ? title : result
}

function titleCaseSegment(segment: string): string {
  if (segment.trim() === '') return ''
  const words = segment.trim().split(/\s+/)
  return words
    .map((word, index) => {
      const next = index + 1 < words.length ? words[index + 1] : null
      const edge = index === 0 || index === words.length - 1
      const sentenceStart = index > 0 && endsSentence(words[index - 1], word)
      const sentenceEnd = endsSentence(word, next)
      const previous = index > 0 ? words[index - 1] : ''
      const core = comparableWord(word)
      const afterOr = previous.toLowerCase().includes(',') && comparableWord(previous) === 'or'
      if (isVolumeNumber(word, next)) return lowercaseVolume(word)
      if (
        !edge &&
        isDeFamily(comparableWord(previous)) &&
        (core === 'la' || core === 'le' || core === 'les')
      ) {
        return lowercaseLetters(word)
      }
      const forceWord = edge || sentenceStart || sentenceEnd || afterOr
      return titleCaseWord(word, edge, forceWord)
    })
    .join(' ')
}

function isDeFamily(core: string): boolean {
  return core === 'de' || core === 'di' || core === 'da' || core === 'du'
}

/** Manga and catalog "v. 3" / "v.1" stay lowercase. */
function isVolumeNumber(word: string, next: string | null): boolean {
  if (/^v\.\d+$/i.test(word)) return true
  return /^v\.$/i.test(word) && next != null && /^\d+/.test(next)
}

function lowercaseVolume(word: string): string {
  return word.replace(/^v\./i, 'v.')
}

function titleCaseWord(word: string, edge: boolean, forceWord: boolean): string {
  const parts = word.split('-')
  if (parts.length === 1) return titleCaseToken(word, forceWord)
  return parts
    .map((part, index) => {
      const last = index === parts.length - 1
      const force = index === 0 || (edge && last) || (forceWord && endsSentence(word, null) && last)
      return titleCaseToken(part, force)
    })
    .join('-')
}

/** True when this token ends a sentence, so the next word is capitalized. */
function endsSentence(token: string, next: string | null): boolean {
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
  if (body === '' || isInitialism(body)) return false
  if (/^[A-Za-z]$/.test(body)) {
    const letter = body.toLowerCase()
    if (!'ivxlcdm'.includes(letter) || (next != null && isSingleInitial(next))) return false
    return true
  }
  const core = body
    .replace(/^[^\p{L}\p{N}]+/u, '')
    .replace(/[^\p{L}\p{N}]+$/u, '')
    .toLowerCase()
  return !ABBREV.has(core)
}

/** A one-letter initial such as J. or A. */
function isSingleInitial(token: string): boolean {
  return comparableWord(token).length === 1 && token.includes('.')
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
  if (hasInternalCap(token)) return token
  const roman = romanToken(token)
  if (roman != null) return roman
  const numbered = numberSuffix(token)
  if (numbered != null) return numbered
  if (keepAcronym(token)) return uppercaseLetters(token)
  const dotted = formatWrappedDotted(token)
  if (dotted != null) return dotted
  const comparable = comparableWord(token)
  if (!force && SMALL.has(comparable)) {
    if (hasLeadingQuote(token) || token.startsWith('(')) return fixMc(capitalizeLike(token))
    return lowercaseLetters(token)
  }
  const french = frenchParticle(token)
  if (french != null) return french
  return fixMc(capitalizeLike(token))
}

/** A roman numeral, optionally possessive (II's) or punctuated (ii.). */
function romanToken(token: string): string | null {
  if (/\d/.test(token)) return null
  const match = ROMAN_TOKEN.exec(token)
  if (!match) return null
  const numeral = match[2]
  if (ROMAN_DENY.has(numeral.toLowerCase()) || !ROMAN.test(numeral)) return null
  const possessive = match[3] == null ? '' : "'s"
  return `${match[1]}${numeral.toUpperCase()}${possessive}${match[4]}`
}

function keepAcronym(token: string): boolean {
  if (token.includes('.')) return false
  const letters = token.replace(/[^\p{L}]/gu, '')
  if (letters.length < 2 || letters.length > 10) return false
  if ([...letters].some((c) => c === c.toLowerCase() && c !== c.toUpperCase())) return false
  const lower = letters.toLowerCase()
  if (ACRONYM.has(lower)) return true
  return letters.length <= 6 && !/[aeiou]/.test(lower)
}

function hasLeadingQuote(token: string): boolean {
  return token !== '' && "\"'“‘".includes(token.charAt(0))
}

/** 21st, 16th, (4th, and a lowercase tail such as 122ff. */
function numberSuffix(token: string): string | null {
  const match = NUMBER_SUFFIX.exec(token)
  if (!match) return null
  const letters = match[3]
  let suffix: string
  if (/^(st|nd|rd|th)$/i.test(letters)) suffix = letters.toLowerCase()
  else if (letters === letters.toLowerCase() || letters === letters.toUpperCase()) {
    suffix = letters === letters.toUpperCase() ? letters.toUpperCase() : letters
  } else return null
  return `${match[1]}${match[2]}${suffix}${match[4]}`
}

/** GIFTionary, McFadden, Ph.D. already mark their own capitals. */
function hasInternalCap(token: string): boolean {
  let seenLetter = false
  let seenLower = false
  let seenUpperAfter = false
  for (const c of token) {
    if (!/\p{L}/u.test(c)) continue
    if (!seenLetter) {
      seenLetter = true
      continue
    }
    if (c === c.toUpperCase() && c !== c.toLowerCase()) seenUpperAfter = true
    else if (c === c.toLowerCase() && c !== c.toUpperCase()) seenLower = true
  }
  return seenUpperAfter && seenLower
}

/** d'Arc and l'Histoire. The particle stays lower. */
function frenchParticle(token: string): string | null {
  const match = FRENCH_PARTICLE.exec(token)
  if (!match) return null
  return `${match[1]}${match[2].toLowerCase()}'${match[4].toUpperCase()}${match[5].toLowerCase()}`
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

/** D.D., and [i.e. keep the abbreviation and the punctuation around it. */
function formatWrappedDotted(token: string): string | null {
  const match = /^([^A-Za-z.]*)([A-Za-z][A-Za-z.]*)([^A-Za-z.]*)$/.exec(token)
  if (!match || !match[2].includes('.')) return null
  if (!isDottedAbbreviation(match[2])) return null
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
