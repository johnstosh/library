// (c) Copyright 2025 by Muczynski
/**
 * Streaming JSON import chunker for large catalog POSTs.
 * Reads via file.stream() and never JSON.parse()s the whole file.
 * Emits sequential request bodies (~fileSize/33) so Cloud Run stays under 600s.
 */

const TEXT_ENCODER = new TextEncoder()
const TEXT_DECODER = new TextDecoder()

const PRELUDE_KEYS = new Set(['libraries', 'authors', 'users', 'branches'])
const BOOK_KEY = 'books'
const TRAILING_KEYS = new Set(['loans', 'photos', 'favorites', 'prices'])

export type JsonImportChunkKind = 'prelude' | 'books' | 'trailing'

export interface JsonImportChunk {
  /** Compact JSON object body for one POST /api/import/json */
  body: Uint8Array
  /** Bytes of the source file consumed when this chunk became ready */
  bytesRead: number
  kind: JsonImportChunkKind
}

export function computeTargetBytes(fileSize: number): number {
  return Math.max(64 * 1024, Math.floor(fileSize / 33))
}

function concatBytes(parts: Uint8Array[]): Uint8Array {
  const total = parts.reduce((n, p) => n + p.length, 0)
  const out = new Uint8Array(total)
  let offset = 0
  for (const p of parts) {
    out.set(p, offset)
    offset += p.length
  }
  return out
}

function buildObjectBody(sections: Map<string, Uint8Array[]>): Uint8Array {
  const parts: Uint8Array[] = [TEXT_ENCODER.encode('{')]
  let firstKey = true
  for (const [key, elements] of sections) {
    if (!firstKey) parts.push(TEXT_ENCODER.encode(','))
    firstKey = false
    parts.push(TEXT_ENCODER.encode(JSON.stringify(key)))
    parts.push(TEXT_ENCODER.encode(':['))
    for (let i = 0; i < elements.length; i++) {
      if (i > 0) parts.push(TEXT_ENCODER.encode(','))
      parts.push(elements[i])
    }
    parts.push(TEXT_ENCODER.encode(']'))
  }
  parts.push(TEXT_ENCODER.encode('}'))
  return concatBytes(parts)
}

export interface TopLevelArrayEvent {
  key: string
  /** Empty when arrayEnded is true (end-of-array marker). */
  element: Uint8Array
  bytesRead: number
  arrayEnded: boolean
}

/**
 * Brace/string-aware tokenizer over file.stream().
 * Yields complete top-level array element byte slices (and end-of-array markers).
 */
/** Prefer file.stream(); fall back for jsdom/test File polyfills without Blob.stream. */
function openFileStream(file: File): ReadableStream<Uint8Array> {
  if (typeof file.stream === 'function') {
    return file.stream()
  }
  return new ReadableStream<Uint8Array>({
    async start(controller) {
      const buf = new Uint8Array(await file.arrayBuffer())
      if (buf.length > 0) controller.enqueue(buf)
      controller.close()
    },
  })
}

export async function* iterateTopLevelArrayElements(
  file: File,
): AsyncGenerator<TopLevelArrayEvent> {
  const reader = openFileStream(file).getReader()
  let buffer = new Uint8Array(0)
  let consumed = 0
  let eof = false

  const ensure = async (absoluteEnd: number): Promise<boolean> => {
    while (consumed + buffer.length < absoluteEnd) {
      if (eof) return false
      const { value, done } = await reader.read()
      if (done || !value) {
        eof = true
        return consumed + buffer.length >= absoluteEnd
      }
      const next = new Uint8Array(buffer.length + value.length)
      next.set(buffer)
      next.set(value, buffer.length)
      buffer = next
    }
    return true
  }

  const byteAt = async (abs: number): Promise<number | null> => {
    if (!(await ensure(abs + 1))) return null
    return buffer[abs - consumed]!
  }

  const sliceAbs = async (start: number, end: number): Promise<Uint8Array> => {
    await ensure(end)
    return buffer.slice(start - consumed, end - consumed)
  }

  const dropBefore = (abs: number) => {
    if (abs <= consumed) return
    buffer = buffer.slice(abs - consumed)
    consumed = abs
  }

  const skipWs = async (pos: number): Promise<number> => {
    let p = pos
    while (true) {
      const b = await byteAt(p)
      if (b === null) return p
      if (b === 0x20 || b === 0x0a || b === 0x0d || b === 0x09) {
        p++
        continue
      }
      return p
    }
  }

  const parseString = async (start: number): Promise<{ end: number; value: string }> => {
    let p = start + 1
    let escaped = false
    while (true) {
      const b = await byteAt(p)
      if (b === null) throw new Error('Unexpected EOF inside JSON string')
      if (escaped) {
        escaped = false
        p++
        continue
      }
      if (b === 0x5c) {
        escaped = true
        p++
        continue
      }
      if (b === 0x22) {
        const quoted = TEXT_DECODER.decode(await sliceAbs(start, p + 1))
        return { end: p + 1, value: JSON.parse(quoted) as string }
      }
      p++
    }
  }

  /** End (exclusive) of a complete JSON value; brace/bracket depth is string-aware. */
  const scanValueEnd = async (pos: number): Promise<number> => {
    const startByte = await byteAt(pos)
    if (startByte === null) throw new Error('Unexpected EOF looking for JSON value')

    if (startByte === 0x22) {
      const { end } = await parseString(pos)
      return end
    }

    if (startByte === 0x7b || startByte === 0x5b) {
      let depth = 0
      let p = pos
      let inString = false
      let escaped = false
      while (true) {
        const b = await byteAt(p)
        if (b === null) throw new Error('Unexpected EOF inside JSON structure')
        if (inString) {
          if (escaped) {
            escaped = false
          } else if (b === 0x5c) {
            escaped = true
          } else if (b === 0x22) {
            inString = false
          }
          p++
          continue
        }
        if (b === 0x22) {
          inString = true
          p++
          continue
        }
        if (b === 0x7b || b === 0x5b) {
          depth++
          p++
          continue
        }
        if (b === 0x7d || b === 0x5d) {
          depth--
          p++
          if (depth === 0) return p
          continue
        }
        p++
      }
    }

    // number / literal
    let p = pos
    while (true) {
      const b = await byteAt(p)
      if (b === null) return p
      if (
        b === 0x2c ||
        b === 0x5d ||
        b === 0x7d ||
        b === 0x20 ||
        b === 0x0a ||
        b === 0x0d ||
        b === 0x09
      ) {
        return p
      }
      p++
    }
  }

  try {
    let pos = await skipWs(0)
    const root = await byteAt(pos)
    if (root !== 0x7b) throw new Error('Expected root JSON object')
    pos++

    while (true) {
      pos = await skipWs(pos)
      const b = await byteAt(pos)
      if (b === null) break
      if (b === 0x7d) {
        pos++
        break
      }
      if (b === 0x2c) {
        pos++
        continue
      }
      if (b !== 0x22) throw new Error(`Expected string key at offset ${pos}`)

      const { end: keyEnd, value: key } = await parseString(pos)
      pos = await skipWs(keyEnd)
      const colon = await byteAt(pos)
      if (colon !== 0x3a) throw new Error(`Expected ':' after key ${key}`)
      pos = await skipWs(pos + 1)

      const valueStart = await byteAt(pos)
      if (valueStart === 0x5b) {
        pos++ // past [
        while (true) {
          pos = await skipWs(pos)
          const eb = await byteAt(pos)
          if (eb === null) throw new Error(`Unexpected EOF in array ${key}`)
          if (eb === 0x5d) {
            pos++
            yield { key, element: new Uint8Array(0), bytesRead: pos, arrayEnded: true }
            dropBefore(Math.max(0, pos - 64 * 1024))
            break
          }
          if (eb === 0x2c) {
            pos++
            continue
          }
          const elemStart = pos
          const elemEnd = await scanValueEnd(pos)
          const element = await sliceAbs(elemStart, elemEnd)
          pos = elemEnd
          yield { key, element, bytesRead: pos, arrayEnded: false }
          dropBefore(Math.max(0, elemStart - 1024))
        }
      } else {
        pos = await scanValueEnd(pos)
        dropBefore(Math.max(0, pos - 64 * 1024))
      }
    }
  } finally {
    reader.releaseLock()
  }
}

/**
 * Stream a catalog export File into POST-sized JSON bodies:
 * 1) one prelude with libraries/authors/users (whichever present)
 * 2) sequential {"books":[...]} chunks targeting ~fileSize/33 bytes (min 64KiB)
 * 3) one trailing body with loans/photos/favorites/prices
 */
export async function* streamJsonImportChunks(
  file: File,
  options?: { targetBytes?: number },
): AsyncGenerator<JsonImportChunk> {
  const targetBytes = options?.targetBytes ?? computeTargetBytes(file.size)

  const prelude = new Map<string, Uint8Array[]>()
  const trailing = new Map<string, Uint8Array[]>()
  let bookBatch: Uint8Array[] = []
  let bookBatchBytes = 0
  let preludeEmitted = false
  let lastBytesRead = 0

  const emitPrelude = (bytesRead: number): JsonImportChunk | null => {
    if (preludeEmitted) return null
    preludeEmitted = true
    if (prelude.size === 0) return null
    return { body: buildObjectBody(prelude), bytesRead, kind: 'prelude' }
  }

  const flushBooks = (bytesRead: number): JsonImportChunk | null => {
    if (bookBatch.length === 0) return null
    const sections = new Map<string, Uint8Array[]>()
    sections.set(BOOK_KEY, bookBatch)
    const chunk: JsonImportChunk = {
      body: buildObjectBody(sections),
      bytesRead,
      kind: 'books',
    }
    bookBatch = []
    bookBatchBytes = 0
    return chunk
  }

  const ensureSection = (map: Map<string, Uint8Array[]>, key: string) => {
    if (!map.has(key)) map.set(key, [])
  }

  for await (const { key, element, bytesRead, arrayEnded } of iterateTopLevelArrayElements(file)) {
    lastBytesRead = bytesRead
    const normalizedKey = key === 'branches' ? 'libraries' : key

    if (arrayEnded) {
      if (PRELUDE_KEYS.has(key) || PRELUDE_KEYS.has(normalizedKey)) {
        ensureSection(prelude, normalizedKey)
        continue
      }
      if (key === BOOK_KEY) {
        const pre = emitPrelude(bytesRead)
        if (pre) yield pre
        const books = flushBooks(bytesRead)
        if (books) yield books
        continue
      }
      if (TRAILING_KEYS.has(key)) {
        const pre = emitPrelude(bytesRead)
        if (pre) yield pre
        const books = flushBooks(bytesRead)
        if (books) yield books
        ensureSection(trailing, key)
        continue
      }
      continue
    }

    if (PRELUDE_KEYS.has(key) || PRELUDE_KEYS.has(normalizedKey)) {
      const list = prelude.get(normalizedKey) ?? []
      list.push(element)
      prelude.set(normalizedKey, list)
      continue
    }

    if (key === BOOK_KEY) {
      const pre = emitPrelude(bytesRead)
      if (pre) yield pre

      bookBatch.push(element)
      bookBatchBytes += element.length
      if (bookBatchBytes >= targetBytes) {
        const books = flushBooks(bytesRead)
        if (books) yield books
      }
      continue
    }

    if (TRAILING_KEYS.has(key)) {
      const pre = emitPrelude(bytesRead)
      if (pre) yield pre
      const books = flushBooks(bytesRead)
      if (books) yield books

      const list = trailing.get(key) ?? []
      list.push(element)
      trailing.set(key, list)
      continue
    }
  }

  const pre = emitPrelude(lastBytesRead || file.size)
  if (pre) yield pre
  const books = flushBooks(lastBytesRead || file.size)
  if (books) yield books
  if (trailing.size > 0) {
    yield {
      body: buildObjectBody(trailing),
      bytesRead: lastBytesRead || file.size,
      kind: 'trailing',
    }
  }
}

/** Collect all chunks (for tests / callers that prefer an array). */
export async function collectJsonImportChunks(
  file: File,
  options?: { targetBytes?: number },
): Promise<JsonImportChunk[]> {
  const chunks: JsonImportChunk[] = []
  for await (const chunk of streamJsonImportChunks(file, options)) {
    chunks.push(chunk)
  }
  return chunks
}
