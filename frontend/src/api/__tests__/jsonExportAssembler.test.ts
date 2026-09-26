// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import {
  appendChunkItems,
  assertExportComplete,
  assembleExportBlob,
  assembleExportJson,
  emptyCatalog,
  estimateExportRequestCount,
  runChunkedJsonExport,
  type ExportChunk,
  type ExportChunkPlan,
} from '../jsonExportAssembler'

function planWith(sections: { section: string; total: number }[], pageSize = 10): ExportChunkPlan {
  const totalItems = sections.reduce((n, s) => n + s.total, 0)
  return {
    targetChunks: 33,
    pageSize,
    totalItems,
    sections,
  }
}

describe('jsonExportAssembler', () => {
  it('estimates request count from section totals and pageSize', () => {
    const plan = planWith(
      [
        { section: 'libraries', total: 1 },
        { section: 'books', total: 25 },
        { section: 'authors', total: 0 },
      ],
      10,
    )
    // libraries: 1, books: 3, authors skipped → 4
    expect(estimateExportRequestCount(plan)).toBe(4)
  })

  it('appends chunk items and validates complete assembly', () => {
    const catalog = emptyCatalog()
    appendChunkItems(catalog, 'books', [{ title: 'A' }, { title: 'B' }])
    appendChunkItems(catalog, 'books', [{ title: 'C' }])
    expect(catalog.books).toHaveLength(3)

    const plan = planWith([
      { section: 'libraries', total: 0 },
      { section: 'authors', total: 0 },
      { section: 'users', total: 0 },
      { section: 'books', total: 3 },
      { section: 'loans', total: 0 },
      { section: 'photos', total: 0 },
      { section: 'favorites', total: 0 },
      { section: 'prices', total: 0 },
    ])
    expect(() => assertExportComplete(catalog, plan)).not.toThrow()
  })

  it('fails loudly when assembled counts mismatch the plan', () => {
    const catalog = emptyCatalog()
    appendChunkItems(catalog, 'books', [{ title: 'A' }])
    const plan = planWith([
      { section: 'libraries', total: 0 },
      { section: 'authors', total: 0 },
      { section: 'users', total: 0 },
      { section: 'books', total: 5 },
      { section: 'loans', total: 0 },
      { section: 'photos', total: 0 },
      { section: 'favorites', total: 0 },
      { section: 'prices', total: 0 },
    ])
    expect(() => assertExportComplete(catalog, plan)).toThrow(/Incomplete JSON export for "books"/)
  })

  it('assembles a single JSON blob with ordered sections', async () => {
    const catalog = emptyCatalog()
    catalog.libraries.push({ branchName: 'Main' })
    catalog.books.push({ title: 'Confessions' })
    const blob = assembleExportBlob(catalog)
    expect(blob).toBeInstanceOf(Blob)
    expect(blob.type).toBe('application/json')
    const parsed = JSON.parse(assembleExportJson(catalog)) as Record<string, unknown[]>
    expect(Object.keys(parsed)).toEqual([
      'libraries',
      'authors',
      'users',
      'books',
      'loans',
      'photos',
      'favorites',
      'prices',
    ])
    expect(parsed.libraries).toHaveLength(1)
    expect(parsed.books).toHaveLength(1)
  })

  it('runs sequential chunk fetches and stitches one downloadable file', async () => {
    const plan = planWith(
      [
        { section: 'libraries', total: 1 },
        { section: 'authors', total: 0 },
        { section: 'users', total: 0 },
        { section: 'books', total: 3 },
        { section: 'loans', total: 0 },
        { section: 'photos', total: 0 },
        { section: 'favorites', total: 0 },
        { section: 'prices', total: 0 },
      ],
      2,
    )

    const bookPages: ExportChunk[] = [
      {
        section: 'books',
        afterId: 0,
        limit: 2,
        nextAfterId: 2,
        total: 3,
        count: 2,
        hasMore: true,
        items: [{ title: 'A' }, { title: 'B' }],
      },
      {
        section: 'books',
        afterId: 2,
        limit: 2,
        nextAfterId: 3,
        total: 3,
        count: 1,
        hasMore: false,
        items: [{ title: 'C' }],
      },
    ]

    const fetchChunk = vi.fn(async (section: string, afterId: number) => {
      if (section === 'libraries') {
        return {
          section: 'libraries',
          afterId: 0,
          limit: 2,
          nextAfterId: 1,
          total: 1,
          count: 1,
          hasMore: false,
          items: [{ branchName: 'Main' }],
        } satisfies ExportChunk
      }
      if (section === 'books') {
        const page = bookPages.find((p) => p.afterId === afterId)
        if (!page) throw new Error(`unexpected afterId ${afterId}`)
        return page
      }
      throw new Error(`unexpected section ${section}`)
    })

    const progress: number[] = []
    const blob = await runChunkedJsonExport(
      {
        fetchPlan: async () => plan,
        fetchChunk,
      },
      (p) => progress.push(p.percentage),
    )

    expect(fetchChunk).toHaveBeenCalledTimes(3)
    expect(fetchChunk).toHaveBeenNthCalledWith(1, 'libraries', 0, 2)
    expect(fetchChunk).toHaveBeenNthCalledWith(2, 'books', 0, 2)
    expect(fetchChunk).toHaveBeenNthCalledWith(3, 'books', 2, 2)
    expect(blob).toBeInstanceOf(Blob)
    expect(blob.type).toBe('application/json')
    expect(blob.size).toBeGreaterThan(0)
    expect(progress.at(-1)).toBe(100)

    // Stitching contract: same items the chunker would put in the blob
    const catalog = emptyCatalog()
    catalog.libraries.push({ branchName: 'Main' })
    catalog.books.push({ title: 'A' }, { title: 'B' }, { title: 'C' })
    const parsed = JSON.parse(assembleExportJson(catalog)) as Record<string, unknown[]>
    expect(parsed.libraries).toEqual([{ branchName: 'Main' }])
    expect(parsed.books).toEqual([{ title: 'A' }, { title: 'B' }, { title: 'C' }])
  })

  it('throws when a chunk request reports incomplete data at the end', async () => {
    const plan = planWith(
      [
        { section: 'libraries', total: 0 },
        { section: 'authors', total: 0 },
        { section: 'users', total: 0 },
        { section: 'books', total: 2 },
        { section: 'loans', total: 0 },
        { section: 'photos', total: 0 },
        { section: 'favorites', total: 0 },
        { section: 'prices', total: 0 },
      ],
      10,
    )

    await expect(
      runChunkedJsonExport({
        fetchPlan: async () => plan,
        fetchChunk: async () =>
          ({
            section: 'books',
            afterId: 0,
            limit: 10,
            nextAfterId: 1,
            total: 2,
            count: 1,
            hasMore: false,
            items: [{ title: 'OnlyOne' }],
          }) satisfies ExportChunk,
      }),
    ).rejects.toThrow(/Incomplete JSON export for "books"/)
  })
})
