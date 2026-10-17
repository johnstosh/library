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
 * A trailing catalog credit ("by Jack Stong", "[by] Name", "translated by Name")
 * is removed. A "by" that belongs to the title stays.
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
 * A trailing statement of responsibility is removed before title case.
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
  main = stripByCredits(main)

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

const CREDIT_HONORIFIC = new Set([
  'rt', 'rev', 'reverend', 'dr', 'doctor', 'fr', 'father', 'st', 'saint',
  'mr', 'mrs', 'ms', 'miss', 'sir', 'lady', 'dom', 'cardinal', 'bishop',
  'archbishop', 'abbe', 'abbé', 'mother', 'sister', 'brother', 'ven',
  'venerable', 'bl', 'blessed', 'mgr', 'monsignor', 'canon', 'prof',
  'professor', 'hon', 'dame', 'pope', 'abp', 'bp', 'card', 'revd',
])

/** Words that make a trailing phrase a title, not a person's name. */
const CREDIT_STOP = new Set([
  'right', 'divine', 'holy', 'sacred', 'good', 'true', 'great', 'new', 'old',
  'last', 'first', 'day', 'week', 'god', 'christ', 'jesus', 'faith', 'love',
  'grace', 'charity', 'blood', 'cross', 'fire', 'way', 'means', 'virtue',
  'command', 'lives', 'life', 'legend', 'legends', 'ladder', 'church', 'see',
  'cat', 'book', 'books', 'silence', 'silences', 'herself', 'himself',
  'themselves', 'author', 'translator', 'word', 'words', 'power', 'name',
  'heaven', 'earth', 'soul', 'souls', 'mystic', 'mystics', 'exercise',
  'exercises', 'creation', 'wind', 'men', 'man', 'anglican', 'anglicans',
  'mohawk', 'iroquois', 'spiritual', 'question', 'answer', 'former', 'pupil',
  'and', 'the', 'a', 'an', 'or', 'for', 'with', 'from', 'into', 'signs',
  'ceremonies', 'passages', 'scripture', 'scriptures', 'prayer', 'bible',
  'council', 'synod', 'conversation', 'method', 'others',
])

const CREDIT_PARTICLE = new Set([
  'de', 'di', 'da', 'du', 'van', 'von', 'del', 'della', 'le', 'la', 'bin', 'ibn', 'of',
  'à', 'á', 'a',
])

const CREDIT_ABBREV = new Set([
  'st', 'mr', 'mrs', 'ms', 'dr', 'rev', 'fr', 'sr', 'jr', 'prof', 'gen', 'vol', 'no', 'ed',
  'vs', 'etc', 'mt', 'bp', 'abp', 'bro', 'capt', 'col', 'lt', 'sgt',
])

/**
 * A verb immediately before a bare "by" means the phrase is part of the title
 * ("written by herself", "appointed by Christ"). A punctuated boundary still
 * allows a later credit to be removed.
 */
const CREDIT_KEEP_BEFORE = new Set([
  'guided', 'told', 'seen', 'inspired', 'explained', 'written', 'slain', 'felt',
  'touched', 'appointed', 'declared', 'related', 'helped', 'assisted', 'saved',
  'led', 'taught', 'made', 'known', 'called', 'gathered', 'accompanied',
  'preceded', 'followed', 'founded', 'approved', 'received', 'accepted',
  'presented', 'selected', 'taken', 'given', 'offered', 'revealed', 'confirmed',
  'attested', 'defined', 'published', 'recommended', 'addressed', 'delivered',
  'held', 'performed', 'shot', 'converted', 'consumed', 'decreed', 'ordered',
  'sanctified', 'mean', 'means',
])

const CREDIT_ROLE = new Set([
  's', 'j', 'o', 'p', 'd', 'c', 'ss', 'r', 'sj', 'osa', 'op', 'osb', 'ofm',
  'cardinal', 'archbishop', 'bishop', 'priest', 'sister', 'sisters', 'father',
])

const CREDIT_DANGLING = /(?:[.;,]\s*)?(?:and\s+|with\s+)?(?:a\s+|an\s+)?(?:preface|pref\.?|prefatory\s+letter(?:\s+from\s+[\s\S]+)?|introduction|introd\.?|notes(?:\s+and\s+comments)?)\s*$/iu
const CREDIT_DANGLING_VERB = /(?:(?:^|\s)[.;]\s*)?(?:translated(?:\s+and\s+annotated)?(?:\s+(?:into|from)\s+(?:the\s+)?(?:original\s+)?[\p{L}.'’-]+)*|edited(?:\s+with\s+an\s+introd\.?)?|compiled|illustrated|adapted|annotated|englished(?:\s+and\s+adapted)?|pictures|drawn|rhymed|selected\s+and\s+edited|authorised\s+translation|authorized\s+translation|tr\.?\s+from\s+the\s+[\p{L}.'’-]+|new\s+revised\s+translation)\s*$/iu
const CREDIT_BY = /^(.*)(\s*(?:[.;,/]|\/)\s*|\s+)(?:\[by\]|by)(?:\s*\.{2,}\s*|\s*…\s*|\s+)(\S[\s\S]*)$/i

/** Removes trailing catalog credits. The last "by" wins, up to six times. */
function stripByCredits(title: string): string {
  let cur = title
  for (let n = 0; n < 6; n++) {
    const next = stripOneCredit(cur)
    if (next == null || next === cur) return cur
    cur = next
  }
  return cur
}

function stripOneCredit(title: string): string | null {
  const match = CREDIT_BY.exec(title)
  if (!match) return null
  const before = match[1]
  const sep = match[2]
  const rest = match[3]
  const boundary = /[.;/,\[]/.test(sep)
  if (!boundary) {
    const prev = /(\p{L}+)['’.]?$/u.exec(before)
    const word = prev ? prev[1].toLowerCase() : ''
    if (CREDIT_KEEP_BEFORE.has(word)) return null
  }
  const decision = decideCredit(rest)
  if (!decision) return null
  let left = cleanCreditIntroducers(before).replace(/[\s,;:/.\-]+$/u, '').trim()
  if (!left) return null
  if (!decision.keep) return left
  const joiner = decision.sep.includes(';') ? '; ' : '. '
  return `${left}${joiner}${decision.keep}`
}

function cleanCreditIntroducers(left: string): string {
  let cur = left
  for (let guard = 0; guard < 4; guard++) {
    const next = cur.replace(CREDIT_DANGLING_VERB, '').replace(CREDIT_DANGLING, '')
    if (next === cur) return cur
    cur = next
  }
  return cur
}

/** A credit through the end of the title, or a credit sentence plus a real subtitle. */
function decideCredit(rest: string): { keep: string; sep: string } | null {
  const cleaned = peelCreditTail(rest)
  if (!cleaned) return null
  if (isCreditObject(cleaned)) return { keep: '', sep: '' }
  const re = /[.;]\s+/g
  let match: RegExpExecArray | null
  while ((match = re.exec(cleaned))) {
    const head = cleaned.slice(0, match.index).trim()
    const token = (/(\S+)$/.exec(head) || [])[1] || ''
    if (isCreditInitialOrAbbrev(token)) continue
    if (!isCreditObject(head)) continue
    const tail = cleaned.slice(match.index + match[0].length).trim()
    if (isCreditContinuation(tail)) return { keep: '', sep: '' }
    return { keep: tail, sep: match[0] }
  }
  return null
}

function isCreditContinuation(tail: string): boolean {
  return /^(?:with\s+(?:a\s+|an\s+)?(?:preface|pref\.?|prefatory|introduction|introd)|with\s+\d+\s+illustrations|and\s+(?:a\s+|an\s+)?(?:preface|introduction|introd)|translated\b|edited\b|compiled\b|illustrated\b|adapted\b|annotated\b|englished\b|authorised\b|authorized\b|tr\.?\s+from\b|pictures\b|drawn\b|selected\b|preface\b|introduction\b)/i.test(tail)
}

function peelCreditTail(rest: string): string {
  return rest.trim()
    .replace(/\s*(?:&|and)\s+others\s*$/iu, '')
    .trim()
    .replace(/\s*\[[^\]]*\]\s*$/u, '')
    .trim()
    .replace(/\s*translated(?:\s+from\s+the\s+[\p{L}.'’-]+)?\s*$/iu, '')
    .trim()
    .replace(/^[.\s…]+/u, '')
    .trim()
    .replace(/[.\s]+$/u, '')
}

function isCreditObject(rest: string): boolean {
  const s = peelCreditTail(rest)
  if (/^(the author|the translator|the editor|the compiler|the illustrator)(?:\s+of\b.*)?$/i.test(s)) return true
  if (/^(?:a|an)\s+(?:sister|brother|nun|religious|member|father|priest)\b/i.test(s)) return true
  return isPersonPhrase(s)
}

function isPersonPhrase(raw: string): boolean {
  let s = raw.trim().replace(/[.\s]+$/u, '')
  s = s.replace(/\s*\[[^\]]*\]\s*$/u, '').trim().replace(/[.\s]+$/u, '')
  if (!s || /[;:/]/.test(s)) return false
  const comma = s.indexOf(',')
  if (comma >= 0) {
    const role = s.slice(comma + 1).trim()
    s = s.slice(0, comma).trim()
    if (!isCreditRole(role)) {
      if (/^of\b/i.test(role) && role.length <= 40 && isSimpleCreditName(s)) return true
      const parts = raw.split(',').map((part) => part.trim().replace(/[.\s]+$/u, ''))
      if (parts.length >= 2 && parts.every((part) => isSimpleCreditName(part))) return true
      return false
    }
  }
  return isSimpleCreditName(s)
}

function isCreditRole(role: string): boolean {
  const text = role.replace(/\./g, ' ').replace(/\s+/g, ' ').trim().toLowerCase()
  if (!text || text.length > 48) return false
  if (/^(author of\b|etc|and others|others)/.test(text)) return true
  const words = text.split(' ')
  if (words.length > 4) return false
  return words.every((word) => CREDIT_ROLE.has(word) || word.length === 1)
}

function isSimpleCreditName(value: string): boolean {
  const tokens = value.split(/\s+/).filter((part) => part !== '')
  if (tokens.length === 0 || tokens.length > 8) return false
  let i = 0
  if (tokens[0] && ['the', 'a', 'an'].includes(creditNorm(tokens[0]))) i = 1
  if (creditNorm(tokens[i] || '') === 'his' && /^(eminence|holiness|grace)$/i.test(creditNorm(tokens[i + 1] || ''))) {
    i += 2
  }
  while (i < tokens.length && ['right', 'very', 'most'].includes(creditNorm(tokens[i])) && isCreditHonorific(tokens[i + 1] || '')) {
    i++
  }
  while (i < tokens.length && isCreditHonorific(tokens[i])) i++
  const names = tokens.slice(i)
  if (names.length === 0) return false
  let real = 0
  for (const name of names) {
    const norm = creditNorm(name)
    if (norm === 'and' || norm === '&') continue
    if (CREDIT_PARTICLE.has(norm)) continue
    if (isCreditInitial(name) || /^\*+$/.test(name) || /^[*…]+$/.test(name)) {
      real++
      continue
    }
    if (CREDIT_STOP.has(norm) || !isCreditNameWord(name)) return false
    real++
  }
  return real >= 1
}

function isCreditNameWord(token: string): boolean {
  if (isCreditInitial(token)) return true
  const norm = creditNorm(token)
  if (!norm || CREDIT_STOP.has(norm) || CREDIT_PARTICLE.has(norm) || norm.length < 2) return false
  return /\p{L}/u.test(token)
}

function isCreditHonorific(token: string): boolean {
  return CREDIT_HONORIFIC.has(creditNorm(token))
}

function isCreditInitialOrAbbrev(token: string): boolean {
  return isCreditInitial(token) || CREDIT_ABBREV.has(creditNorm(token))
}

function isCreditInitial(token: string): boolean {
  const core = token.replace(/^[^\p{L}]+/u, '').replace(/[^\p{L}.]+$/u, '')
  return /^[A-Za-z](?:\.[A-Za-z])*\.?$/.test(core)
}

function creditNorm(token: string): string {
  return token.replace(/^[^\p{L}\p{N}]+/u, '').replace(/[^\p{L}\p{N}.]+$/u, '').replace(/\./g, '').toLowerCase()
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
