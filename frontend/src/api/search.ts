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

export interface SearchFilters {
  inLib: boolean
  elec: boolean
  freeText: boolean
  audio: boolean
}

export const defaultSearchFilters: SearchFilters = {
  inLib: false,
  elec: false,
  freeText: false,
  audio: false,
}

export function useSearch(
  query: string,
  page = 0,
  size = 20,
  filters: SearchFilters = defaultSearchFilters,
  enabled = true,
  selectedLabels?: string[],
) {
  const hasLabels = selectedLabels != null && selectedLabels.length > 0
  const labelsParam = hasLabels ? `&labels=${encodeURIComponent((selectedLabels ?? []).join(','))}` : ''
  const filterParams = [
    filters.inLib ? '&filterInLibrary=true' : '',
    filters.elec ? '&filterElectronic=true' : '',
    filters.freeText ? '&filterFreeText=true' : '',
    filters.audio ? '&filterAudio=true' : '',
  ].join('')
  return useQuery({
    queryKey: ['search', query, page, size, filters, selectedLabels ?? []],
    queryFn: () =>
      api.get<SearchResponse>(
        `/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}${filterParams}${labelsParam}`,
        { requireAuth: false },
      ),
    enabled,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}
