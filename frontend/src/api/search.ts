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
    filters.withoutGrokipedia ? '&filterWithoutGrokipedia=true' : '',
    filters.withGrokipedia ? '&filterWithGrokipedia=true' : '',
    filters.withoutGenres ? '&filterWithoutGenres=true' : '',
    filters.notActiveStatus ? '&filterNotActiveStatus=true' : '',
    filters.withoutFreeTextUrls ? '&filterWithoutFreeTextUrls=true' : '',
    filters.hasYdlAudio ? '&filterYdlAudio=true' : '',
    filters.hasYdlBook ? '&filterYdlBook=true' : '',
    filters.hasYdlEbook ? '&filterYdlEbook=true' : '',
    filters.hasEmuAudio ? '&filterEmuAudio=true' : '',
    filters.hasEmuBook ? '&filterEmuBook=true' : '',
    filters.hasEmuEbook ? '&filterEmuEbook=true' : '',
  ].join('')
}

export function useSearch(
  query: string,
  bookPage = 0,
  authorPage = 0,
  size = 20,
  filters: SearchFilters = defaultSearchFilters,
  enabled = true,
  selectedLabels?: string[],
) {
  const hasLabels = selectedLabels != null && selectedLabels.length > 0
  const labelsParam = hasLabels ? `&labels=${encodeURIComponent((selectedLabels ?? []).join(','))}` : ''
  const filterParams = chipQueryParams(filters)
  return useQuery({
    queryKey: ['search', query, bookPage, authorPage, size, filters, selectedLabels ?? []],
    queryFn: () =>
      api.get<SearchResponse>(
        `/search?query=${encodeURIComponent(query)}&bookPage=${bookPage}&authorPage=${authorPage}&size=${size}${filterParams}${labelsParam}`,
        { requireAuth: false },
      ),
    enabled,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}
