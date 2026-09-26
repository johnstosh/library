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

function decode(body: Uint8Array): unknown {
  return JSON.parse(new TextDecoder().decode(body))
}

describe('computeTargetBytes', () => {
  it('uses at least 64KiB', () => {
    expect(computeTargetBytes(1000)).toBe(64 * 1024)
  })

  it('uses floor(fileSize/33) when larger than 64KiB', () => {
    const size = 33 * 100_000
    expect(computeTargetBytes(size)).toBe(100_000)
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
    expect(chunks.map((c) => c.kind)).toEqual(['prelude', 'books', 'trailing'])
    const booksBody = decode(chunks[1]!.body) as { books: Array<{ title: string }> }
    expect(booksBody.books).toHaveLength(2)
    expect(booksBody.books[0]!.title).toBe('Book {with} braces')
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
    const bookChunks = chunks.filter((c) => c.kind === 'books')
    expect(bookChunks.length).toBeGreaterThan(1)
    const totalBooks = bookChunks.reduce((n, c) => {
      const body = decode(c.body) as { books: unknown[] }
      expect(Object.keys(body)).toEqual(['books'])
      return n + body.books.length
    }, 0)
    expect(totalBooks).toBe(10)
    expect(chunks[0]!.kind).toBe('prelude')
    expect(chunks[chunks.length - 1]!.kind).toBe('trailing')
  })

  it('emits prelude then books for a full catalog shape', async () => {
    const json = JSON.stringify({
      libraries: [{ branchName: 'Main', librarySystemName: 'Sys' }],
      authors: [{ name: 'Author One' }],
      users: [{ username: 'alice' }],
      books: [
        { title: 'First', libraryName: 'Main', authorName: 'Author One' },
        { title: 'Second', libraryName: 'Main', authorName: 'Author One' },
      ],
      loans: [{ bookTitle: 'First', username: 'alice' }],
      photos: [],
      favorites: [],
      prices: [],
    })
    const chunks = await collectJsonImportChunks(fileFrom(json), { targetBytes: 1_000_000 })
    expect(chunks.map((c) => c.kind)).toEqual(['prelude', 'books', 'trailing'])

    const prelude = decode(chunks[0]!.body) as {
      libraries: unknown[]
      authors: unknown[]
      users: unknown[]
    }
    expect(prelude.libraries).toHaveLength(1)
    expect(prelude.authors).toHaveLength(1)
    expect(prelude.users).toHaveLength(1)
    expect('books' in prelude).toBe(false)

    const books = decode(chunks[1]!.body) as { books: unknown[] }
    expect(books.books).toHaveLength(2)

    const trailing = decode(chunks[2]!.body) as {
      loans: unknown[]
      photos: unknown[]
      favorites: unknown[]
      prices: unknown[]
    }
    expect(trailing.loans).toHaveLength(1)
    expect(trailing.photos).toEqual([])
    expect(trailing.favorites).toEqual([])
    expect(trailing.prices).toEqual([])
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
})
