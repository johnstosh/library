// (c) Copyright 2025 by Muczynski
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { runGrokJob } from './grokJobs'
import { postByIdsInBatches } from './byIds'
import { queryKeys } from '@/config/queryClient'
import type { AuthorChipFilters } from '@/utils/authorChipFilters'
import type { AuthorAvailabilityDto, AuthorDto, AuthorEnrichmentResultDto, AuthorNameNormalizationResultDto, AuthorSummaryDto, BookDto, BulkDeleteResultDto } from '@/types/dtos'

const AUTHOR_FILTER_KEYS: (keyof AuthorChipFilters)[] = [
  'hasYdlBook',
  'hasYdlEbook',
  'hasYdlAudio',
  'hasEmuBook',
  'hasEmuEbook',
  'hasEmuAudio',
  'hasAclaBook',
  'hasAclaEbook',
  'hasAclaAudio',
  'mostRecent',
  'withoutDescription',
  'withoutGrokipedia',
  'withGrokipedia',
  'zeroBooks',
  'notCanonical',
  'withoutPhotos',
  'withPhotos',
  'withoutBirthDate',
  'withoutDeathDate',
]

/** Authors page filters. Every active chip is sent to GET /authors/filtered-summaries. */
export interface AuthorListFilters {
  chips: AuthorChipFilters
  favoriteLists?: readonly string[]
  /** Name or alternate name. Blank returns every author that passes the chips. */
  q?: string
}

/** Query string for GET /authors/filtered-summaries. Names match the controller. */
export function authorListFilterQuery(filters: AuthorListFilters): string {
  const params = new URLSearchParams()
  const q = (filters.q ?? '').trim()
  if (q) params.set('q', q)
  for (const key of AUTHOR_FILTER_KEYS) {
    if (filters.chips[key]) params.set(key, 'true')
  }
  if (filters.favoriteLists && filters.favoriteLists.length > 0) {
    params.set('favoriteLists', filters.favoriteLists.join(','))
  }
  return params.toString()
}

function authorLastName(name?: string): string {
  if (!name || !name.trim()) return ''
  const parts = name.trim().split(/\s+/)
  return parts[parts.length - 1].toLowerCase()
}

function orderedAuthorSummaries(summaries: AuthorSummaryDto[]): AuthorSummaryDto[] {
  return [...summaries].sort((a, b) => {
    const byName = authorLastName(a.name).localeCompare(authorLastName(b.name))
    if (byName !== 0) return byName
    return a.id - b.id
  })
}

function useAuthorCatalog(
  summariesEndpoint: string,
  summariesQueryKey: readonly unknown[],
  cacheScope?: string,
  pageSize?: number,
) {
  const queryClient = useQueryClient()
  const windowKey = pageSize ? summariesEndpoint : ''
  const [loadedCount, setLoadedCount] = useState(pageSize ?? 0)
  const totalRef = useRef(0)
  const pendingRef = useRef(false)

  useEffect(() => {
    if (!pageSize) return
    pendingRef.current = false
    setLoadedCount(pageSize)
  }, [windowKey, pageSize])

  // Step 1: Fetch summaries (ID + lastModified) from appropriate endpoint
  const { data: summaries, isLoading: summariesLoading, isFetching: summariesFetching, error: summariesError } = useQuery({
    queryKey: summariesQueryKey,
    queryFn: () => api.get<AuthorSummaryDto[]>(summariesEndpoint),
    staleTime: 0, // Always check for fresh data when filter changes
    refetchOnMount: true, // Always refetch when component mounts or filter changes
  })

  if (summaries) totalRef.current = summaries.length

  const visibleSummaries = useMemo(() => {
    if (!summaries) return []
    const ordered = pageSize ? orderedAuthorSummaries(summaries) : summaries
    return pageSize ? ordered.slice(0, loadedCount) : ordered
  }, [summaries, pageSize, loadedCount])

  // Step 2: Determine which visible authors need fetching based on cache
  const authorsToFetch = useMemo(() => {
    return visibleSummaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<AuthorDto>(queryKeys.authors.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [visibleSummaries, queryClient])

  // Step 3: Batch fetch changed authors using /authors/by-ids
  const { data: fetchedAuthors, isLoading: fetchingAuthors, isFetching: detailsFetching, error: detailsError } = useQuery({
    queryKey: queryKeys.authors.byIds(authorsToFetch, cacheScope),
    queryFn: () => postByIdsInBatches<AuthorDto>('/authors/by-ids', authorsToFetch),
    enabled: summaries !== undefined && authorsToFetch.length > 0,
  })

  useEffect(() => {
    pendingRef.current = detailsFetching
  }, [detailsFetching, loadedCount])

  // Populate individual author caches when authors are fetched
  React.useEffect(() => {
    fetchedAuthors?.forEach((author) => {
      queryClient.setQueryData(queryKeys.authors.detail(author.id), author)
    })
  }, [fetchedAuthors, queryClient])

  // Step 4: Get all authors for display
  // IMPORTANT: We must use fetchedAuthors directly here, not rely on the cache.
  // The cache is populated by a useEffect which runs AFTER this useMemo,
  // so reading from cache would return stale/missing data on first render.
  const allAuthors = useMemo(() => {
    if (!summaries) return []

    // Unwindowed lists wait for every batch. A windowed page keeps the rows
    // already on screen while the next page is requested.
    if (!pageSize && authorsToFetch.length > 0 && !fetchedAuthors) {
      return []
    }

    // Build a map of newly fetched authors for quick lookup
    const fetchedAuthorsMap = new Map<number, AuthorDto>()
    fetchedAuthors?.forEach((author) => {
      fetchedAuthorsMap.set(author.id, author)
    })

    const authors = visibleSummaries
      .map((summary) => {
        const fetched = fetchedAuthorsMap.get(summary.id)
        if (fetched) return fetched
        return queryClient.getQueryData<AuthorDto>(queryKeys.authors.detail(summary.id))
      })
      .filter((author): author is AuthorDto => author !== undefined)

    if (pageSize) return authors
    return authors.sort((a, b) => authorLastName(a.name).localeCompare(authorLastName(b.name)))
  }, [summaries, visibleSummaries, queryClient, fetchedAuthors, authorsToFetch, pageSize])

  const isLoadingMore = Boolean(pageSize) && detailsFetching && loadedCount > pageSize!
  const total = totalRef.current
  const hasMore = Boolean(pageSize) && summaries !== undefined && loadedCount < total
  const loadMore = () => {
    if (!pageSize || !hasMore || summariesFetching || detailsFetching || pendingRef.current) return
    pendingRef.current = true
    setLoadedCount((count) => count + pageSize)
  }

  return {
    data: allAuthors,
    total,
    hasMore,
    loadMore,
    isLoadingMore,
    isLoading: summariesLoading || (fetchingAuthors && !isLoadingMore),
    isFetching: summariesFetching || (detailsFetching && !isLoadingMore),
    error: summariesError || detailsError,
  }
}

// Hook to get all authors with optimized lastModified caching.
// Preset filters stay on their existing endpoints. The Authors page uses useFilteredAuthors.
export function useAuthors(filter?: 'all' | 'without-description' | 'zero-books' | 'without-grokipedia' | 'most-recent') {
  const endpoint = (() => {
    switch (filter) {
      case 'without-description': return '/authors/without-description'
      case 'zero-books': return '/authors/zero-books'
      case 'without-grokipedia': return '/authors/without-grokipedia'
      case 'most-recent': return '/authors/most-recent-day'
      case 'all':
      default: return '/authors/summaries'
    }
  })()
  const summariesQueryKey = filter
    ? queryKeys.authors.filterSummaries(filter)
    : queryKeys.authors.summaries()
  return useAuthorCatalog(endpoint, summariesQueryKey, filter)
}

export function useFilteredAuthors(filters: AuthorListFilters, options?: { pageSize?: number }) {
  const query = authorListFilterQuery(filters)
  return useAuthorCatalog(
    query ? `/authors/filtered-summaries?${query}` : '/authors/filtered-summaries',
    queryKeys.authors.filteredSummaries(query),
    query ? `filtered:${query}` : 'filtered',
    options?.pageSize,
  )
}

export function useMostRecentAuthorSummaries(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.authors.filterSummaries('most-recent'),
    queryFn: () => api.get<AuthorSummaryDto[]>('/authors/most-recent-day'),
    enabled,
    staleTime: 0,
    refetchOnMount: true,
  })
}

export function useAuthorCount() {
  return useQuery({
    queryKey: queryKeys.authors.count(),
    queryFn: () => api.get<{ count: number }>('/authors/count'),
    staleTime: 30 * 1000,
  })
}

export function useAuthorAvailability() {
  return useQuery({
    queryKey: queryKeys.authors.availability(),
    queryFn: () => api.get<AuthorAvailabilityDto[]>('/authors/availability'),
    staleTime: 30 * 1000,
    refetchOnMount: true,
  })
}

// Hook to get a single author
export function useAuthor(id: number) {
  return useQuery({
    queryKey: queryKeys.authors.detail(id),
    queryFn: () => api.get<AuthorDto>(`/authors/${id}`),
    enabled: !!id,
  })
}

// Hook to get books by author
export function useAuthorBooks(id: number) {
  return useQuery({
    queryKey: queryKeys.authors.books(id),
    queryFn: () => api.get<BookDto[]>(`/authors/${id}/books`),
    enabled: !!id,
  })
}

// Hook to create an author
export function useCreateAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (author: Partial<AuthorDto>) => api.post<AuthorDto>('/authors', author),
    onSuccess: (newAuthor) => {
      // Immediately populate the new author's detail cache so AuthorCombobox can
      // display the author name without waiting for a full summaries + byIds refetch.
      queryClient.setQueryData(queryKeys.authors.detail(newAuthor.id), newAuthor)

      // Append the new author summary to the existing summaries cache so useAuthors()
      // picks it up on the next render (avoids the blank-display window during refetch).
      const existingSummaries = queryClient.getQueryData<AuthorSummaryDto[]>(queryKeys.authors.summaries())
      if (existingSummaries) {
        queryClient.setQueryData(queryKeys.authors.summaries(), [
          ...existingSummaries,
          { id: newAuthor.id, lastModified: newAuthor.lastModified },
        ])
      }

      // Invalidate so background refetch confirms the server state
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to update an author
export function useUpdateAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, author }: { id: number; author: Partial<AuthorDto> }) =>
      api.put<AuthorDto>(`/authors/${id}`, author),
    onSuccess: (data, variables) => {
      // Update the detail cache and invalidate summaries
      queryClient.setQueryData(queryKeys.authors.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to delete an author
export function useDeleteAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/authors/${id}`),
    onSuccess: (_, id) => {
      // Remove from cache and invalidate summaries
      queryClient.removeQueries({ queryKey: queryKeys.authors.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

/** Rewrite selected authors into canonical given-name-then-family-name form. */
export function useNormalizeAuthorNamesBulk() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) =>
      api.post<AuthorNameNormalizationResultDto[]>('/authors/normalize-names-bulk', ids),
    onSuccess: (results) => {
      for (const result of results) {
        if (result.mergedIntoAuthorId) {
          queryClient.removeQueries({ queryKey: queryKeys.authors.detail(result.authorId) })
          continue
        }
        if (result.updatedAuthor) {
          queryClient.setQueryData(queryKeys.authors.detail(result.authorId), result.updatedAuthor)
        }
      }
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.favorites.all })
    },
  })
}

// Hook to delete multiple authors
export function useDeleteAuthors() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) => api.post<BulkDeleteResultDto>('/authors/delete-bulk', ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

/**
 * Fill blank catalog fields for a single author using Grok.
 */
export function useGenerateAuthorMissingData() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) =>
      runGrokJob<AuthorEnrichmentResultDto>(`/authors/${id}/generate-missing/start`),
    onSuccess: (data, id) => {
      if (data.updatedAuthor) {
        queryClient.setQueryData(queryKeys.authors.detail(id), data.updatedAuthor)
      }
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

/**
 * Fill blank catalog fields for multiple authors with progress tracking.
 * Processes authors sequentially so the toolbar can show n/total.
 */
export function useGenerateAuthorsMissingDataWithProgress(
  onProgress?: (completed: number, total: number) => void
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (ids: number[]) => {
      const results: AuthorEnrichmentResultDto[] = []
      const total = ids.length

      for (let i = 0; i < ids.length; i++) {
        const id = ids[i]
        try {
          const result = await runGrokJob<AuthorEnrichmentResultDto>(`/authors/${id}/generate-missing/start`)
          results.push(result)
          if (result.updatedAuthor) {
            queryClient.setQueryData(queryKeys.authors.detail(id), result.updatedAuthor)
          }
        } catch (error) {
          results.push({
            authorId: id,
            name: `Author ${id}`,
            success: false,
            skipped: false,
            filledFields: [],
            errorMessage: error instanceof Error ? error.message : 'Unknown error',
          })
        }
        onProgress?.(i + 1, total)
      }

      return results
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}
