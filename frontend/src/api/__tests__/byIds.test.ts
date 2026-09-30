// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../client'
import { BY_IDS_BATCH_SIZE, postByIdsInBatches } from '../byIds'

vi.mock('../client', () => ({
  api: {
    post: vi.fn(),
  },
}))

afterEach(() => {
  vi.clearAllMocks()
})

describe('postByIdsInBatches', () => {
  it('posts at most 100 ids and waits for each batch', async () => {
    const ids = Array.from({ length: BY_IDS_BATCH_SIZE + 1 }, (_, index) => index + 1)
    const seen: number[][] = []
    vi.mocked(api.post).mockImplementation(async (_path, body) => {
      const batch = body as number[]
      seen.push(batch)
      return batch.map((id) => ({ id }))
    })

    const rows = await postByIdsInBatches<{ id: number }>('/books/by-ids', ids)

    expect(seen).toEqual([ids.slice(0, BY_IDS_BATCH_SIZE), [BY_IDS_BATCH_SIZE + 1]])
    expect(rows).toHaveLength(ids.length)
    expect(api.post).toHaveBeenNthCalledWith(1, '/books/by-ids', seen[0])
    expect(api.post).toHaveBeenNthCalledWith(2, '/books/by-ids', seen[1])
  })
})
