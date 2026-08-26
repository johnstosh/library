// (c) Copyright 2025 by Muczynski
import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { BookDto, AuthorDto } from '@/types/dtos'
import {
  defaultBookChipFilters,
  type BookChipFilters,
} from '@/utils/bookChipFilters'

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

export type SearchFilters = BookChipFilters

export const defaultSearchFilters: SearchFilters = { ...defaultBookChipFilters }

function chipQueryParams(filters: SearchFilters): string {
  return [
    filters.inLibrary ? '&filterInLibrary=true' : '',
    filters.electronic ? '&filterElectronic=true' : '',
    filters.freeText ? '&filterFreeText=true' : '',
    filters.audio ? '&filterAudio=true' : '',
    filters.mostRecent ? '&filterMostRecent=true' : '',
    filters.withoutLoc ? '&filterWithoutLoc=true' : '',
    filters.threeLetterLoc ? '&filterThreeLetterLoc=true' : '',
    filters.withoutGrokipedia ? '&filterWithoutGrokipedia=true' : '',
    filters.withoutGenres ? '&filterWithoutGenres=true' : '',
    filters.notActiveStatus ? '&filterNotActiveStatus=true' : '',
    filters.withoutFreeTextUrls ? '&filterWithoutFreeTextUrls=true' : '',
  ].join('')
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
  const filterParams = chipQueryParams(filters)
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
