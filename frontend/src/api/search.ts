// (c) Copyright 2025 by Muczynski
import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { BookDto, AuthorDto } from '@/types/dtos'

export interface SearchResponse {
  books: BookDto[]
  authors: AuthorDto[]
  bookPage: {
    totalPages: number
    totalElements: number
    currentPage: number
    pageSize: number
  }
  authorPage: {
    totalPages: number
    totalElements: number
    currentPage: number
    pageSize: number
  }
}

export type SearchType = 'ONLINE' | 'ALL' | 'IN_LIBRARY'

export function useSearch(query: string, page = 0, size = 20, searchType: SearchType = 'IN_LIBRARY') {
  return useQuery({
    queryKey: ['search', query, page, size, searchType],
    queryFn: () =>
      api.get<SearchResponse>(`/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}&searchType=${searchType}`, {
        requireAuth: false,
      }),
    enabled: query.trim().length > 0,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}
