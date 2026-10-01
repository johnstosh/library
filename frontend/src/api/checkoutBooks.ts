// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from './client'

/** Must match CheckoutMatchService.MIN_CHARS. The server applies the same gate. */
export const CHECKOUT_SEARCH_MIN_CHARS = 3

export interface CheckoutMatch {
  id: number
  title: string
  author?: string
  locNumber?: string
  status?: string
}

export interface CheckoutSearchTerms {
  title: string
  author: string
  locNumber: string
}

/**
 * Fields with fewer than 3 characters are omitted. Returns null when none remain,
 * which means the checkout form must not call the server.
 */
export function checkoutSearchTerms(filters: CheckoutSearchTerms): CheckoutSearchTerms | null {
  const title = filters.title.trim()
  const author = filters.author.trim()
  const locNumber = filters.locNumber.trim()
  const usable = {
    title: title.length >= CHECKOUT_SEARCH_MIN_CHARS ? title : '',
    author: author.length >= CHECKOUT_SEARCH_MIN_CHARS ? author : '',
    locNumber: locNumber.length >= CHECKOUT_SEARCH_MIN_CHARS ? locNumber : '',
  }
  if (!usable.title && !usable.author && !usable.locNumber) {
    return null
  }
  return usable
}

export function checkoutSearchKey(terms: CheckoutSearchTerms): string {
  return JSON.stringify(terms)
}

export function checkoutMatchPath(terms: CheckoutSearchTerms): string {
  const params = new URLSearchParams()
  if (terms.title) params.set('title', terms.title)
  if (terms.author) params.set('author', terms.author)
  if (terms.locNumber) params.set('locNumber', terms.locNumber)
  return `/books/checkout-matches?${params.toString()}`
}

export async function fetchCheckoutMatches(terms: CheckoutSearchTerms): Promise<CheckoutMatch[]> {
  return api.get<CheckoutMatch[]>(checkoutMatchPath(terms))
}

interface AppliedSearch {
  key: string
  books: CheckoutMatch[]
  error: string
}

/**
 * Checkout book search. Nothing is requested until a title, author, or call
 * number has 3 characters. A newer query waits until the request already in
 * flight finishes, then one follow-up runs with the latest text.
 */
export function useCheckoutBookSearch(filters: CheckoutSearchTerms) {
  const terms = useMemo(
    () => checkoutSearchTerms(filters),
    [filters.title, filters.author, filters.locNumber],
  )
  const key = terms ? checkoutSearchKey(terms) : ''
  const [applied, setApplied] = useState<AppliedSearch>({ key: '', books: [], error: '' })
  const [flightKey, setFlightKey] = useState<string | null>(null)

  const keyRef = useRef(key)
  keyRef.current = key
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
    if (!key) {
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
          const startKey = keyRef.current
          if (!startKey) break
          const startTerms = JSON.parse(startKey) as CheckoutSearchTerms
          if (mountedRef.current) setFlightKey(startKey)
          try {
            const books = await fetchCheckoutMatches(startTerms)
            if (!mountedRef.current) break
            if (keyRef.current === startKey) {
              setApplied({ key: startKey, books, error: '' })
            }
          } catch (err) {
            if (!mountedRef.current) break
            if (keyRef.current === startKey) {
              const message = err instanceof Error ? err.message : 'Could not load books'
              setApplied({ key: startKey, books: [], error: message })
            }
          } finally {
            if (mountedRef.current && keyRef.current === startKey) {
              setFlightKey(null)
            }
          }
          if (keyRef.current && keyRef.current !== startKey) {
            againRef.current = true
          }
        }
      } finally {
        runningRef.current = false
        if (againRef.current && keyRef.current && mountedRef.current) {
          void drain()
        }
      }
    }
  }, [key])

  const matches = Boolean(key) && applied.key === key
  return {
    data: matches ? applied.books : [],
    isFetching: Boolean(key) && (flightKey !== null || !matches),
    error: matches ? applied.error : '',
  }
}
