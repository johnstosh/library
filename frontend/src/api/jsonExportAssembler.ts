// (c) Copyright 2025 by Muczynski
/**
 * Chunked JSON export client: sequential GETs (~33) with section keyset paging,
 * then stitch into one downloadable catalog JSON object.
 */

export const EXPORT_SECTION_KEYS = [
  'libraries',
  'authors',
  'users',
  'books',
  'loans',
  'photos',
  'favorites',
  'prices',
] as const

export type ExportSectionKey = (typeof EXPORT_SECTION_KEYS)[number]

export interface ExportChunkPlanSection {
  section: string
  total: number
}

export interface ExportChunkPlan {
  targetChunks: number
  pageSize: number
  totalItems: number
  sections: ExportChunkPlanSection[]
}

export interface ExportChunk {
  section: string
  afterId: number
  limit: number
  nextAfterId: number | null
  total: number
  count: number
  hasMore: boolean
  items: unknown[]
}

export interface JsonExportProgress {
  percentage: number
  completedRequests: number
  totalRequests: number
  section: string
  isExporting: boolean
}

export function estimateExportRequestCount(plan: ExportChunkPlan): number {
  const pageSize = Math.max(1, plan.pageSize || 1)
  let total = 0
  for (const s of plan.sections) {
    if (s.total <= 0) continue
    total += Math.ceil(s.total / pageSize)
  }
  return Math.max(total, plan.totalItems > 0 ? 1 : 0)
}

export function emptyCatalog(): Record<ExportSectionKey, unknown[]> {
  return {
    libraries: [],
    authors: [],
    users: [],
    books: [],
    loans: [],
    photos: [],
    favorites: [],
    prices: [],
  }
}

/** Merge chunk items into the catalog; returns updated item count for the section. */
export function appendChunkItems(
  catalog: Record<ExportSectionKey, unknown[]>,
  section: string,
  items: unknown[],
): number {
  const key = normalizeSection(section)
  if (!key) {
    throw new Error(`Unknown export section: ${section}`)
  }
  catalog[key].push(...items)
  return catalog[key].length
}

export function normalizeSection(section: string): ExportSectionKey | null {
  const key = section === 'branches' ? 'libraries' : section
  return (EXPORT_SECTION_KEYS as readonly string[]).includes(key)
    ? (key as ExportSectionKey)
    : null
}

/**
 * Validate assembled catalog against the plan. Throws if any section is incomplete.
 */
export function assertExportComplete(
  catalog: Record<ExportSectionKey, unknown[]>,
  plan: ExportChunkPlan,
): void {
  const expected = new Map<string, number>()
  for (const s of plan.sections) {
    const key = normalizeSection(s.section)
    if (!key) continue
    expected.set(key, s.total)
  }
  for (const key of EXPORT_SECTION_KEYS) {
    const got = catalog[key].length
    const want = expected.get(key) ?? 0
    if (got !== want) {
      throw new Error(
        `Incomplete JSON export for "${key}": got ${got} item(s), expected ${want}`,
      )
    }
  }
}

export function assembleExportJson(
  catalog: Record<ExportSectionKey, unknown[]>,
): string {
  // Compact JSON (same as streaming export). Preserve section order.
  const ordered: Record<string, unknown[]> = {}
  for (const key of EXPORT_SECTION_KEYS) {
    ordered[key] = catalog[key]
  }
  return JSON.stringify(ordered)
}

export function assembleExportBlob(catalog: Record<ExportSectionKey, unknown[]>): Blob {
  return new Blob([assembleExportJson(catalog)], { type: 'application/json' })
}

export interface FetchExportChunkDeps {
  fetchPlan: () => Promise<ExportChunkPlan>
  fetchChunk: (section: string, afterId: number, limit: number) => Promise<ExportChunk>
}

/**
 * Run the full chunked export: plan → sequential section pages → one Blob.
 * Fails loudly if any request fails or assembled counts mismatch the plan.
 */
export async function runChunkedJsonExport(
  deps: FetchExportChunkDeps,
  onProgress?: (progress: Omit<JsonExportProgress, 'isExporting'>) => void,
): Promise<Blob> {
  const plan = await deps.fetchPlan()
  const catalog = emptyCatalog()
  const totalRequests = estimateExportRequestCount(plan)
  let completedRequests = 0

  const report = (section: string) => {
    const percentage =
      totalRequests > 0
        ? Math.min(100, (completedRequests / totalRequests) * 100)
        : 100
    onProgress?.({
      percentage,
      completedRequests,
      totalRequests,
      section,
    })
  }

  report('')

  if (plan.totalItems === 0 || totalRequests === 0) {
    // Empty catalog — still produce a valid empty-arrays document
    assertExportComplete(catalog, plan)
    onProgress?.({
      percentage: 100,
      completedRequests: 0,
      totalRequests: 0,
      section: '',
    })
    return assembleExportBlob(catalog)
  }

  const pageSize = Math.max(1, plan.pageSize || 1)

  for (const sectionPlan of plan.sections) {
    const sectionKey = normalizeSection(sectionPlan.section)
    if (!sectionKey) {
      throw new Error(`Unknown export section in plan: ${sectionPlan.section}`)
    }
    if (sectionPlan.total <= 0) {
      continue
    }

    let afterId = 0
    let guard = 0
    const maxPages = Math.ceil(sectionPlan.total / pageSize) + 5

    while (guard < maxPages) {
      guard += 1
      const chunk = await deps.fetchChunk(sectionKey, afterId, pageSize)
      if (chunk.section && normalizeSection(chunk.section) !== sectionKey) {
        throw new Error(
          `Export chunk section mismatch: expected ${sectionKey}, got ${chunk.section}`,
        )
      }
      appendChunkItems(catalog, sectionKey, chunk.items ?? [])
      completedRequests += 1
      report(sectionKey)

      if (!chunk.hasMore) {
        break
      }
      if (chunk.nextAfterId == null) {
        throw new Error(
          `Export chunk for "${sectionKey}" reported hasMore without nextAfterId`,
        )
      }
      if (chunk.nextAfterId === afterId) {
        throw new Error(
          `Export chunk for "${sectionKey}" did not advance afterId (stuck at ${afterId})`,
        )
      }
      afterId = chunk.nextAfterId
    }

    if (guard >= maxPages) {
      throw new Error(`Export for "${sectionKey}" exceeded max page guard`)
    }
  }

  assertExportComplete(catalog, plan)
  onProgress?.({
    percentage: 100,
    completedRequests,
    totalRequests,
    section: '',
  })
  return assembleExportBlob(catalog)
}
