// (c) Copyright 2025 by Muczynski
import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { BookDto, AuthorDto } from '@/types/dtos'
import {
  defaultBookChipFilters,
  type BookChipFilters,
} from '@/utils/bookChipFilters'
import { joinFavoriteLists } from '@/utils/favoriteListParams'
import {
  DEFAULT_LIBRARY_CHECKED_OLDER_DAYS,
  libraryCheckedOlderCode,
} from '@/utils/bookFilterParams'

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
    filters.freeText ? '&filterFreeText=true' : '',
    filters.audio ? '&filterAudio=true' : '',
    filters.mostRecent ? '&filterMostRecent=true' : '',
    filters.withoutGrokipedia ? '&filterWithoutGrokipedia=true' : '',
    filters.withGrokipedia ? '&filterWithGrokipedia=true' : '',
    filters.withoutGenres ? '&filterWithoutGenres=true' : '',
    filters.withoutFreeTextUrls ? '&filterWithoutFreeTextUrls=true' : '',
    filters.hasYdlAudio ? '&filterYdlAudio=true' : '',
    filters.hasYdlBook ? '&filterYdlBook=true' : '',
    filters.hasYdlEbook ? '&filterYdlEbook=true' : '',
    filters.hasEmuAudio ? '&filterEmuAudio=true' : '',
    filters.hasEmuBook ? '&filterEmuBook=true' : '',
    filters.hasEmuEbook ? '&filterEmuEbook=true' : '',
    filters.hasAclaAudio ? '&filterAclaAudio=true' : '',
    filters.hasAclaBook ? '&filterAclaBook=true' : '',
    filters.hasAclaEbook ? '&filterAclaEbook=true' : '',
    filters.hasWccAudio ? '&filterWccAudio=true' : '',
    filters.hasWccBook ? '&filterWccBook=true' : '',
    filters.hasWccEbook ? '&filterWccEbook=true' : '',
    filters.hasMsuAudio ? '&filterMsuAudio=true' : '',
    filters.hasMsuBook ? '&filterMsuBook=true' : '',
    filters.hasMsuEbook ? '&filterMsuEbook=true' : '',
    filters.withoutYdlAudio ? '&filterWithoutYdlAudio=true' : '',
    filters.withoutYdlBook ? '&filterWithoutYdlBook=true' : '',
    filters.withoutYdlEbook ? '&filterWithoutYdlEbook=true' : '',
    filters.withoutEmuAudio ? '&filterWithoutEmuAudio=true' : '',
    filters.withoutEmuBook ? '&filterWithoutEmuBook=true' : '',
    filters.withoutEmuEbook ? '&filterWithoutEmuEbook=true' : '',
    filters.withoutAclaAudio ? '&filterWithoutAclaAudio=true' : '',
    filters.withoutAclaBook ? '&filterWithoutAclaBook=true' : '',
    filters.withoutAclaEbook ? '&filterWithoutAclaEbook=true' : '',
    filters.withoutWccAudio ? '&filterWithoutWccAudio=true' : '',
    filters.withoutWccBook ? '&filterWithoutWccBook=true' : '',
    filters.withoutWccEbook ? '&filterWithoutWccEbook=true' : '',
    filters.withoutMsuAudio ? '&filterWithoutMsuAudio=true' : '',
    filters.withoutMsuBook ? '&filterWithoutMsuBook=true' : '',
    filters.withoutMsuEbook ? '&filterWithoutMsuEbook=true' : '',
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
  selectedDifficulties?: string[],
  favoriteLists?: string[],
  selectedStatuses?: string[],
  libraryCheckedOlder?: string,
  libraryCheckedOlderDays?: number,
  libraryLookupError?: string,
) {
  const hasLabels = selectedLabels != null && selectedLabels.length > 0
  const labelsParam = hasLabels ? `&labels=${encodeURIComponent((selectedLabels ?? []).join(','))}` : ''
  const hasDifficulties = selectedDifficulties != null && selectedDifficulties.length > 0
  const difficultyParam = hasDifficulties
    ? `&readingDifficulty=${encodeURIComponent((selectedDifficulties ?? []).join(','))}`
    : ''
  const hasFavoriteLists = favoriteLists != null && favoriteLists.length > 0
  const favoriteParam = hasFavoriteLists
    ? `&favoriteLists=${encodeURIComponent(joinFavoriteLists(favoriteLists ?? []))}`
    : ''
  const hasStatuses = selectedStatuses != null && selectedStatuses.length > 0
  const statusParam = hasStatuses
    ? `&status=${encodeURIComponent((selectedStatuses ?? []).join(','))}`
    : ''
  const filterParams = chipQueryParams(filters)
  const library = libraryCheckedOlderCode(libraryCheckedOlder)
  const libraryDays = libraryCheckedOlderDays != null && libraryCheckedOlderDays >= 1
    ? libraryCheckedOlderDays
    : DEFAULT_LIBRARY_CHECKED_OLDER_DAYS
  const lookupError = libraryCheckedOlderCode(libraryLookupError)
  const libraryParam = [
    library
      ? `&libraryCheckedOlder=${encodeURIComponent(library)}&libraryCheckedOlderDays=${libraryDays}`
      : '',
    lookupError ? `&libraryLookupError=${encodeURIComponent(lookupError)}` : '',
  ].join('')
  return useQuery({
    queryKey: [
      'search',
      query,
      bookPage,
      authorPage,
      size,
      filters,
      selectedLabels ?? [],
      selectedDifficulties ?? [],
      favoriteLists ?? [],
      selectedStatuses ?? [],
      library,
      library ? libraryDays : 0,
      lookupError,
    ],
    queryFn: () =>
      api.get<SearchResponse>(
        `/search?query=${encodeURIComponent(query)}&bookPage=${bookPage}&authorPage=${authorPage}&size=${size}${filterParams}${labelsParam}${difficultyParam}${favoriteParam}${statusParam}${libraryParam}`,
        { requireAuth: false },
      ),
    enabled,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}
