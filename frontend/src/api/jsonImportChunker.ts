// (c) Copyright 2025 by Muczynski
/**
 * Streaming JSON import chunker for large catalog POSTs.
 * Reads via file.stream() and never JSON.parse()s the whole file.
 * Emits sequential request bodies (~fileSize/33) so Cloud Run stays under 600s.
 * Every top-level array (libraries, authors, users, books, loans, photos,
 * favorites, prices) is split by the same byte budget — authors alone may
 * span multiple POSTs.
 */

const TEXT_ENCODER = new TextEncoder()
const TEXT_DECODER = new TextDecoder()

/** Canonical section order for catalog export/import JSON. */
const SECTION_KEYS = [
  'libraries',
  'authors',
  'users',
  'books',
  'loans',
  'photos',
  'favorites',
  'prices',
] as const

const KNOWN_KEYS = new Set<string>([...SECTION_KEYS, 'branches'])

export type JsonImportChunkKind = string

export interface JsonImportChunk {
  /** Compact JSON object body for one POST /api/import/json */
  body: Uint8Array
  /** Bytes of the source file consumed when this chunk became ready */
  bytesRead: number
  /** Section key(s) in this body, joined with '+' (e.g. "authors" or "libraries+authors") */
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
    if (elements.length === 0) continue
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

function normalizeKey(key: string): string {
  return key === 'branches' ? 'libraries' : key
}

function chunkKind(sections: Map<string, Uint8Array[]>): string {
  return [...sections.keys()].filter((k) => (sections.get(k)?.length ?? 0) > 0).join('+')
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
 * Stream a catalog export File into POST-sized JSON bodies.
 * Every known top-level array is chunked by the same byte budget (~fileSize/33,
 * min 64KiB). A single POST may contain one or more consecutive sections;
 * a large section (e.g. authors) may span multiple POSTs. Section order is
 * preserved across requests: libraries → authors → users → books → …
 */
export async function* streamJsonImportChunks(
  file: File,
  options?: { targetBytes?: number },
): AsyncGenerator<JsonImportChunk> {
  const targetBytes = options?.targetBytes ?? computeTargetBytes(file.size)

  let batch = new Map<string, Uint8Array[]>()
  let batchBytes = 0
  let lastBytesRead = 0

  const flush = (bytesRead: number): JsonImportChunk | null => {
    const nonEmpty = new Map<string, Uint8Array[]>()
    for (const [key, elements] of batch) {
      if (elements.length > 0) nonEmpty.set(key, elements)
    }
    if (nonEmpty.size === 0) {
      batch = new Map()
      batchBytes = 0
      return null
    }
    const chunk: JsonImportChunk = {
      body: buildObjectBody(nonEmpty),
      bytesRead,
      kind: chunkKind(nonEmpty),
    }
    batch = new Map()
    batchBytes = 0
    return chunk
  }

  for await (const { key, element, bytesRead, arrayEnded } of iterateTopLevelArrayElements(file)) {
    lastBytesRead = bytesRead
    if (arrayEnded) continue
    if (!KNOWN_KEYS.has(key)) continue

    const normalizedKey = normalizeKey(key)

    if (!batch.has(normalizedKey)) batch.set(normalizedKey, [])
    batch.get(normalizedKey)!.push(element)
    batchBytes += element.length

    if (batchBytes >= targetBytes) {
      const chunk = flush(bytesRead)
      if (chunk) yield chunk
    }
  }

  const remaining = flush(lastBytesRead || file.size)
  if (remaining) yield remaining
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
