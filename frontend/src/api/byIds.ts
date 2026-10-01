// (c) Copyright 2025 by Muczynski
import { api } from './client'

/** Must match com.muczynski.library.service.ByIds.MAX_BATCH. */
export const BY_IDS_BATCH_SIZE = 100

/** Books and Authors pages load one by-ids batch, then wait for scroll or Load more. */
export const CATALOG_PAGE_SIZE = BY_IDS_BATCH_SIZE

/**
 * POST ids to a by-ids endpoint, one batch after another.
 * A single request larger than the server limit is rejected.
 */
export async function postByIdsInBatches<T>(path: string, ids: number[]): Promise<T[]> {
  if (ids.length === 0) {
    return []
  }
  const rows: T[] = []
  for (let start = 0; start < ids.length; start += BY_IDS_BATCH_SIZE) {
    const batch = ids.slice(start, start + BY_IDS_BATCH_SIZE)
    const fetched = await api.post<T[]>(path, batch)
    rows.push(...fetched)
  }
  return rows
}
