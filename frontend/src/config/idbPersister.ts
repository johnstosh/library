// (c) Copyright 2025 by Muczynski
import { openDB } from 'idb'
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister'
import type { DehydratedState, Query } from '@tanstack/react-query'

const DB_NAME = 'library-query-cache'
const STORE_NAME = 'queryCache'
const CACHE_KEY = 'LIBRARY_ENTITY_CACHE'

// Max age: 7 days. On cold start, cached book/author detail data is displayed
// immediately while the summaries check runs and only changed records are refetched.
export const PERSISTED_CACHE_MAX_AGE = 7 * 24 * 60 * 60 * 1000

// Thin async-storage wrapper around idb so createAsyncStoragePersister can use IndexedDB.
// The persister stores the entire dehydrated cache as a single serialized blob under CACHE_KEY.
const idbStorage = {
  getItem: async (key: string): Promise<string | null> => {
    try {
      const db = await openDB(DB_NAME, 1, {
        upgrade(db) {
          db.createObjectStore(STORE_NAME)
        },
      })
      return (await db.get(STORE_NAME, key)) ?? null
    } catch {
      return null
    }
  },
  setItem: async (key: string, value: string): Promise<void> => {
    try {
      const db = await openDB(DB_NAME, 1, {
        upgrade(db) {
          db.createObjectStore(STORE_NAME)
        },
      })
      await db.put(STORE_NAME, value, key)
    } catch {
      // Ignore write failures (e.g. private browsing quota exceeded)
    }
  },
  removeItem: async (key: string): Promise<void> => {
    try {
      const db = await openDB(DB_NAME, 1, {
        upgrade(db) {
          db.createObjectStore(STORE_NAME)
        },
      })
      await db.delete(STORE_NAME, key)
    } catch {
      // Ignore
    }
  },
}

export const idbPersister = createAsyncStoragePersister({
  storage: idbStorage,
  key: CACHE_KEY,
})

// Only persist books.detail and authors.detail queries.
// Summaries, lists, settings, photos, etc. are intentionally excluded —
// they either change frequently or are cheap to re-fetch.
export function shouldPersistQuery(query: Query): boolean {
  const key = query.queryKey as unknown[]
  return (
    (key[0] === 'books' && key[1] === 'detail') ||
    (key[0] === 'authors' && key[1] === 'detail')
  )
}

// Passed to PersistQueryClientProvider as dehydrateOptions.
export const persistDehydrateOptions: { shouldDehydrateQuery: (query: Query) => boolean } = {
  shouldDehydrateQuery: shouldPersistQuery,
}

// Type helper for the rehydrate options passed alongside dehydrateOptions.
// Queries restored from IndexedDB will be stale immediately (staleTime = 5 min default),
// so they display instantly but get background-refreshed via the summaries → by-ids flow.
export type PersistedDehydratedState = DehydratedState
