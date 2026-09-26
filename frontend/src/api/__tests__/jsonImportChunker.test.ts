// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  collectJsonImportChunks,
  computeTargetBytes,
} from '../jsonImportChunker'

function fileFrom(json: string, name = 'catalog.json'): File {
  const bytes = new TextEncoder().encode(json)
  const file = new File([bytes], name, { type: 'application/json' })
  // jsdom File lacks Blob.stream / arrayBuffer — provide Node-compatible shims for tests
  if (typeof file.stream !== 'function') {
    Object.defineProperty(file, 'stream', {
      configurable: true,
      value: () =>
        new ReadableStream<Uint8Array>({
          start(controller) {
            controller.enqueue(bytes)
            controller.close()
          },
        }),
    })
  }
  if (typeof file.arrayBuffer !== 'function') {
    Object.defineProperty(file, 'arrayBuffer', {
      configurable: true,
      value: async () =>
        bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength),
    })
  }
  Object.defineProperty(file, 'size', { configurable: true, value: bytes.length })
  return file
}

function decode(body: Uint8Array): Record<string, unknown[]> {
  return JSON.parse(new TextDecoder().decode(body)) as Record<string, unknown[]>
}

describe('computeTargetBytes', () => {
  it('uses at least 64KiB', () => {
    expect(computeTargetBytes(1000)).toBe(64 * 1024)
  })

  it('uses floor(fileSize/11) when larger than 64KiB', () => {
    const size = 11 * 100_000
    expect(computeTargetBytes(size)).toBe(100_000)
  })

  it('targets about 11 pieces for a multi-megabyte catalog', () => {
    const size = 11 * 200_000 // ~2.2 MiB
    expect(computeTargetBytes(size)).toBe(200_000)
    expect(Math.ceil(size / computeTargetBytes(size))).toBe(11)
  })
})

describe('collectJsonImportChunks', () => {
  it('keeps braces that appear inside JSON strings', async () => {
    const json = JSON.stringify({
      libraries: [],
      authors: [],
      users: [],
      books: [
        { title: 'Book {with} braces', libraryName: 'L', authorName: 'A' },
        { title: 'Normal', libraryName: 'L', authorName: 'A' },
      ],
      loans: [],
    })
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 1_000_000 })
    expect(chunks).toHaveLength(1)
    expect(chunks[0]!.kind).toBe('books')
    const booksBody = decode(chunks[0]!.body)
    expect(booksBody.books).toHaveLength(2)
    expect((booksBody.books![0] as { title: string }).title).toBe('Book {with} braces')
  })

  it('splits authors across multiple POSTs by byte budget (prelude not monolithic)', async () => {
    const authors = Array.from({ length: 12 }, (_, i) => ({
      name: `Author ${i} with enough padding ________________________________`,
    }))
    const json = JSON.stringify({
      libraries: [{ branchName: 'Lib', librarySystemName: 'Sys' }],
      authors,
      users: [{ username: 'alice' }],
      books: [{ title: 'One', libraryName: 'Lib', authorName: authors[0]!.name }],
    })
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 200 })

    const authorChunks = chunks.filter((c) => c.kind.includes('authors'))
    expect(authorChunks.length).toBeGreaterThan(1)

    const totalAuthors = chunks.reduce((n, c) => {
      const body = decode(c.body)
      return n + (body.authors?.length ?? 0)
    }, 0)
    expect(totalAuthors).toBe(12)

    // libraries/authors/users must not all land in a single first POST
    const first = decode(chunks[0]!.body)
    const firstHasAllPrelude =
      (first.libraries?.length ?? 0) > 0 &&
      (first.authors?.length ?? 0) === 12 &&
      (first.users?.length ?? 0) > 0
    expect(firstHasAllPrelude).toBe(false)
  })

  it('splits books by byte budget into multiple POSTs', async () => {
    const books = Array.from({ length: 10 }, (_, i) => ({
      title: `Book ${i} with enough padding ______________________________`,
      libraryName: 'Lib',
      authorName: 'Auth',
    }))
    const json = JSON.stringify({
      libraries: [{ branchName: 'Lib', librarySystemName: 'Sys' }],
      authors: [{ name: 'Auth' }],
      users: [],
      books,
      loans: [],
      photos: [],
      favorites: [],
      prices: [],
    })
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 200 })
    const bookChunks = chunks.filter((c) => Object.keys(decode(c.body)).includes('books'))
    expect(bookChunks.length).toBeGreaterThan(1)
    const totalBooks = bookChunks.reduce((n, c) => {
      const body = decode(c.body)
      return n + (body.books?.length ?? 0)
    }, 0)
    expect(totalBooks).toBe(10)
  })

  it('preserves section order across requests for a full catalog shape', async () => {
    const json = JSON.stringify({
      libraries: [{ branchName: 'Main', librarySystemName: 'Sys' }],
      authors: [{ name: 'Author One' }],
      users: [{ username: 'alice' }],
      books: [
        { title: 'First', libraryName: 'Main', authorName: 'Author One' },
        { title: 'Second', libraryName: 'Main', authorName: 'Author One' },
      ],
      loans: [{ bookTitle: 'First', username: 'alice' }],
      photos: [{ bookTitle: 'First' }],
      favorites: [{ username: 'alice', authorName: 'Author One' }],
      prices: [{ bookTitle: 'First', amount: 1 }],
    })
    // Large budget → as few POSTs as fit; may be one or a few, but key order must hold
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 1_000_000 })

    const sectionOrder = [
      'libraries',
      'authors',
      'users',
      'books',
      'loans',
      'photos',
      'favorites',
      'prices',
    ]
    const seenKeys: string[] = []
    for (const chunk of chunks) {
      for (const key of Object.keys(decode(chunk.body))) {
        seenKeys.push(key)
      }
    }
    // Flattened key sequence must be non-decreasing in sectionOrder
    let lastIdx = -1
    for (const key of seenKeys) {
      const idx = sectionOrder.indexOf(key)
      expect(idx).toBeGreaterThanOrEqual(0)
      expect(idx).toBeGreaterThanOrEqual(lastIdx)
      lastIdx = idx
    }

    expect(seenKeys).toContain('libraries')
    expect(seenKeys).toContain('authors')
    expect(seenKeys).toContain('users')
    expect(seenKeys).toContain('books')
    expect(seenKeys).toContain('loans')
  })

  it('can pack consecutive small sections into one POST under budget', async () => {
    const json = JSON.stringify({
      libraries: [{ branchName: 'Main', librarySystemName: 'Sys' }],
      authors: [{ name: 'Author One' }],
      users: [{ username: 'alice' }],
      books: [{ title: 'First', libraryName: 'Main', authorName: 'Author One' }],
    })
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 1_000_000 })
    expect(chunks).toHaveLength(1)
    const body = decode(chunks[0]!.body)
    expect(Object.keys(body)).toEqual(['libraries', 'authors', 'users', 'books'])
    expect(chunks[0]!.kind).toBe('libraries+authors+users+books')
  })

  it('supports books-only payloads (map-seed companion)', async () => {
    const json = JSON.stringify({
      books: [{ title: 'Solo', libraryName: 'L', authorName: 'A' }],
    })
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 1_000_000 })
    expect(chunks).toHaveLength(1)
    expect(chunks[0]!.kind).toBe('books')
    expect(decode(chunks[0]!.body)).toEqual({
      books: [{ title: 'Solo', libraryName: 'L', authorName: 'A' }],
    })
  })

  it('aims for roughly 11 chunks on a large synthetic catalog', async () => {
    // Need fileSize/11 > 64KiB => fileSize > 720KiB so the /11 budget (not the floor) applies
    const pad = 'x'.repeat(2000)
    const books = Array.from({ length: 500 }, (_, i) => ({
      title: `Book ${i} ${pad}`,
      libraryName: 'Lib',
      authorName: 'Auth',
    }))
    const json = JSON.stringify({
      libraries: [{ branchName: 'Lib', librarySystemName: 'Sys' }],
      authors: Array.from({ length: 80 }, (_, i) => ({ name: `Auth ${i} ${pad}` })),
      users: [{ username: 'alice' }],
      books,
      loans: [],
      photos: [],
      favorites: [],
      prices: [],
    })
    const file = fileFrom(json)
    expect(file.size).toBeGreaterThan(11 * 64 * 1024)
    const target = computeTargetBytes(file.size)
    expect(target).toBe(Math.floor(file.size / 11))
    expect(target).toBeGreaterThan(64 * 1024)

    const chunks = await collectJsonImportChunks(file)
    // Allow some slack: remainder / section boundaries can nudge off exact 11
    expect(chunks.length).toBeGreaterThanOrEqual(8)
    expect(chunks.length).toBeLessThanOrEqual(14)
  })
})
