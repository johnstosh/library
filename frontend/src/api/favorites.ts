// (c) Copyright 2025 by Muczynski
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import { useIsAuthenticated } from '@/stores/authStore'

export type FavoriteItemType = 'BOOK' | 'AUTHOR'

export interface FavoriteSummaryDto {
  favoriteBookIds: number[]
  favoriteAuthorIds: number[]
}

export interface FavoriteItemDto {
  itemType: FavoriteItemType
  itemId: number
  selectedLists: string[]
  availableLists: string[]
}

export interface FavoriteUpdateDto {
  itemType: FavoriteItemType
  itemId: number
  listNames: string[]
}

export interface FavoriteListCountDto {
  listName: string
  bookCount: number
  authorCount: number
}

export function useFavoriteSummary() {
  const isAuthenticated = useIsAuthenticated()
  return useQuery({
    queryKey: queryKeys.favorites.summary(),
    queryFn: () => api.get<FavoriteSummaryDto>('/favorites/summary'),
    enabled: isAuthenticated,
  })
}

export function useFavoriteItem(itemType: FavoriteItemType, itemId: number, enabled: boolean) {
  const isAuthenticated = useIsAuthenticated()
  return useQuery({
    queryKey: queryKeys.favorites.item(itemType, itemId),
    queryFn: () =>
      api.get<FavoriteItemDto>(`/favorites/item?itemType=${itemType}&itemId=${itemId}`),
    enabled: isAuthenticated && enabled && itemId > 0,
  })
}

export function useReplaceFavoriteItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (update: FavoriteUpdateDto) =>
      api.put<FavoriteItemDto>('/favorites/item', update),
    onSuccess: (data) => {
      queryClient.setQueryData(queryKeys.favorites.item(data.itemType, data.itemId), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.favorites.summary() })
      queryClient.invalidateQueries({ queryKey: queryKeys.favorites.stats() })
    },
  })
}

export function useFavoriteStats() {
  return useQuery({
    queryKey: queryKeys.favorites.stats(),
    queryFn: () => api.get<FavoriteListCountDto[]>('/import/favorite-stats'),
  })
}

export function listNameToTestId(listName: string): string {
  return listName
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}
