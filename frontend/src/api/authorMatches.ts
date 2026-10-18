// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from './client'
import { CHECKOUT_SEARCH_MIN_CHARS } from './checkoutBooks'

/** Must match AuthorNameMatchService.MIN_CHARS, which matches the checkout gate. */
export const AUTHOR_SEARCH_MIN_CHARS = CHECKOUT_SEARCH_MIN_CHARS

export interface AuthorNameMatch {
  id: number
  name: string
}

/**
 * Blank until the trimmed name has 3 characters. The book form must not call
 * the server before that.
 */
export function authorSearchTerm(name: string): string | null {
  const trimmed = name.trim()
  if (trimmed.length < AUTHOR_SEARCH_MIN_CHARS) {
    return null
  }
  return trimmed
}

export function authorMatchPath(name: string): string {
  const params = new URLSearchParams()
  params.set('name', name)
  return `/authors/name-matches?${params.toString()}`
}

export async function fetchAuthorNameMatches(name: string): Promise<AuthorNameMatch[]> {
  return api.get<AuthorNameMatch[]>(authorMatchPath(name))
}

interface AppliedSearch {
  term: string
  authors: AuthorNameMatch[]
  error: string
}

/**
 * Author search for the book form. Nothing is requested until the name has
 * 3 characters. A newer query waits until the request already in flight
 * finishes, then one follow-up runs with the latest text.
 */
export function useAuthorNameSearch(name: string) {
  const term = useMemo(() => authorSearchTerm(name) ?? '', [name])
  const [applied, setApplied] = useState<AppliedSearch>({ term: '', authors: [], error: '' })
  const [flightTerm, setFlightTerm] = useState<string | null>(null)

  const termRef = useRef(term)
  termRef.current = term
  const runningRef = useRef(false)
  const againRef = useRef(false)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
    }
  }, [])

  useEffect(() => {
    if (!term) {
      againRef.current = false
      return
    }
    againRef.current = true
    void drain()

    async function drain() {
      if (runningRef.current) return
      runningRef.current = true
      try {
        while (againRef.current && mountedRef.current) {
          againRef.current = false
          const startTerm = termRef.current
          if (!startTerm) break
          if (mountedRef.current) setFlightTerm(startTerm)
          try {
            const authors = await fetchAuthorNameMatches(startTerm)
            if (!mountedRef.current) break
            if (termRef.current === startTerm) {
              setApplied({ term: startTerm, authors, error: '' })
            }
          } catch (err) {
            if (!mountedRef.current) break
            if (termRef.current === startTerm) {
              const message = err instanceof Error ? err.message : 'Could not load authors'
              setApplied({ term: startTerm, authors: [], error: message })
            }
          } finally {
            if (mountedRef.current && termRef.current === startTerm) {
              setFlightTerm(null)
            }
          }
          if (termRef.current && termRef.current !== startTerm) {
            againRef.current = true
          }
        }
      } finally {
        runningRef.current = false
        if (againRef.current && termRef.current && mountedRef.current) {
          void drain()
        }
      }
    }
  }, [term])

  const matches = Boolean(term) && applied.term === term
  return {
    data: matches ? applied.authors : [],
    isFetching: Boolean(term) && (flightTerm !== null || !matches),
    error: matches ? applied.error : '',
  }
}
