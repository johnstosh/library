// (c) Copyright 2025 by Muczynski
import React, { useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { AuthorDto, AuthorSummaryDto, BookDto } from '@/types/dtos'

// Hook to get all authors with optimized lastModified caching
export function useAuthors(filter?: 'all' | 'without-description' | 'zero-books' | 'without-grokipedia' | 'most-recent') {
  const queryClient = useQueryClient()

  // Determine the filter endpoint - all filter endpoints now return AuthorSummaryDto
  // For 'all' filter, we also use the summaries endpoint to enable caching
  const getFilterEndpoint = (f: typeof filter) => {
    switch (f) {
      case 'without-description': return '/authors/without-description'
      case 'zero-books': return '/authors/zero-books'
      case 'without-grokipedia': return '/authors/without-grokipedia'
      case 'most-recent': return '/authors/most-recent-day'
      case 'all':
      default: return '/authors/summaries'
    }
  }

  // Step 1: Fetch summaries (ID + lastModified) from appropriate endpoint
  // For all filters including 'all', we use the summaries endpoint to enable caching
  const filterEndpoint = getFilterEndpoint(filter)
  const { data: summaries, isLoading: summariesLoading } = useQuery({
    queryKey: filter ? queryKeys.authors.filterSummaries(filter) : queryKeys.authors.summaries(),
    queryFn: () => api.get<AuthorSummaryDto[]>(filterEndpoint),
    staleTime: 0, // Always check for fresh data when filter changes
    refetchOnMount: true, // Always refetch when component mounts or filter changes
  })

  // Step 2: Determine which authors need fetching based on cache
  const authorsToFetch = useMemo(() => {
    if (!summaries) return []

    return summaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<AuthorDto>(queryKeys.authors.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [summaries, queryClient])

  // Step 3: Batch fetch changed authors using /authors/by-ids
  // For all filters (including 'all'), we now use the optimized caching approach
  const { data: fetchedAuthors, isLoading: fetchingAuthors } = useQuery({
    queryKey: queryKeys.authors.byIds(authorsToFetch, filter),
    queryFn: async () => {
      if (authorsToFetch.length > 0) {
        // Only fetch authors that changed
        return api.post<AuthorDto[]>('/authors/by-ids', authorsToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && authorsToFetch.length > 0,
  })

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

    // Wait for fetchedAuthors to complete if we have authors to fetch
    if (authorsToFetch.length > 0 && !fetchedAuthors) {
      return []
    }

    // Build a map of newly fetched authors for quick lookup
    const fetchedAuthorsMap = new Map<number, AuthorDto>()
    fetchedAuthors?.forEach((author) => {
      fetchedAuthorsMap.set(author.id, author)
    })

    // Get authors: prefer freshly fetched authors, then fall back to cache
    const authors = summaries
      .map((summary) => {
        // First check if we just fetched this author
        const fetched = fetchedAuthorsMap.get(summary.id)
        if (fetched) return fetched
        // Otherwise check cache (for authors that didn't need refetching)
        return queryClient.getQueryData<AuthorDto>(queryKeys.authors.detail(summary.id))
      })
      .filter((author): author is AuthorDto => author !== undefined)

    // Sort by last name
    return authors.sort((a, b) => {
      const getLastName = (name: string | undefined) => {
        if (!name || !name.trim()) return ''
        const parts = name.trim().split(/\s+/)
        return parts[parts.length - 1].toLowerCase()
      }
      return getLastName(a.name).localeCompare(getLastName(b.name))
    })
  }, [summaries, queryClient, fetchedAuthors, authorsToFetch])

  return {
    data: allAuthors,
    isLoading: summariesLoading || fetchingAuthors,
  }
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
    onSuccess: () => {
      // Invalidate summaries to trigger re-fetch
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

// Hook to delete multiple authors
export function useDeleteAuthors() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) => api.post('/authors/delete-bulk', ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}
