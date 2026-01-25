// (c) Copyright 2025 by Muczynski
import React, { useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { AuthorDto, AuthorSummaryDto, BookDto } from '@/types/dtos'

// Hook to get all authors with optimized lastModified caching
export function useAuthors(filter?: 'all' | 'without-description' | 'zero-books' | 'without-grokipedia' | 'most-recent') {
  const queryClient = useQueryClient()

  // For filter endpoints, use direct fetch (not cacheable in the same way)
  const filterEndpoint = filter && filter !== 'all' ? getFilterEndpoint(filter) : null

  // Step 1: Fetch summaries (id + lastModified) for all authors
  const { data: summaries, isLoading: summariesLoading } = useQuery({
    queryKey: queryKeys.authors.summaries(),
    queryFn: () => api.get<AuthorSummaryDto[]>('/authors/summaries'),
    staleTime: 0, // Always check for fresh data
    refetchOnMount: true,
    enabled: !filterEndpoint, // Only use caching for 'all' filter
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
  const { data: fetchedAuthors, isLoading: fetchingAuthors } = useQuery({
    queryKey: queryKeys.authors.byIds(authorsToFetch),
    queryFn: async () => {
      if (authorsToFetch.length > 0) {
        return api.post<AuthorDto[]>('/authors/by-ids', authorsToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && authorsToFetch.length > 0 && !filterEndpoint,
  })

  // Populate individual author caches when authors are fetched
  React.useEffect(() => {
    fetchedAuthors?.forEach((author) => {
      queryClient.setQueryData(queryKeys.authors.detail(author.id), author)
    })
  }, [fetchedAuthors, queryClient])

  // Step 4: Get all authors for display (cached approach)
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

  // For filter endpoints, use direct query
  const directQuery = useQuery({
    queryKey: queryKeys.authors.list(filter),
    queryFn: () => api.get<AuthorDto[]>(filterEndpoint!),
    enabled: !!filterEndpoint,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })

  // Return appropriate data based on filter
  if (filterEndpoint) {
    return {
      data: directQuery.data,
      isLoading: directQuery.isLoading,
    }
  }

  return {
    data: allAuthors,
    isLoading: summariesLoading || fetchingAuthors,
  }
}

function getFilterEndpoint(filter: string): string | null {
  switch (filter) {
    case 'without-description':
      return '/authors/without-description'
    case 'zero-books':
      return '/authors/zero-books'
    case 'without-grokipedia':
      return '/authors/without-grokipedia'
    case 'most-recent':
      return '/authors/most-recent-day'
    default:
      return null
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
