// (c) Copyright 2025 by Muczynski
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import { useIsAuthenticated } from '@/stores/authStore'

export type FavoriteItemType = 'BOOK' | 'AUTHOR'

export interface FavoriteListMembershipDto {
  listName: string
  bookIds: number[]
  authorIds: number[]
}

export const PATRON_FAVORITE_LISTS = ['Have Read', 'Want to Read', 'Want to Recommend'] as const
export const LIBRARIAN_FAVORITE_LISTS = ['Needs Review', 'Need to Locate'] as const

export interface FavoriteSummaryDto {
  favoriteBookIds: number[]
  favoriteAuthorIds: number[]
  lists: FavoriteListMembershipDto[]
  availableLists?: string[]
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

export function favoriteListChips(
  lists: FavoriteListMembershipDto[] | undefined,
  mode: 'search' | 'books' | 'authors',
): { listName: string; count: number }[] {
  if (!lists) return []
  return lists
    .map((list) => {
      const count =
        mode === 'search'
          ? list.bookIds.length + list.authorIds.length
          : mode === 'books'
            ? list.bookIds.length
            : list.authorIds.length
      return { listName: list.listName, count }
    })
    .filter((list) => list.count > 0)
}

export function favoriteItemIdsForLists(
  lists: FavoriteListMembershipDto[] | undefined,
  selected: string[],
  field: 'bookIds' | 'authorIds',
): Set<number> {
  const wanted = new Set(selected)
  const ids = new Set<number>()
  if (!lists || wanted.size === 0) return ids
  for (const list of lists) {
    if (!wanted.has(list.listName)) continue
    for (const id of list[field]) ids.add(id)
  }
  return ids
}

export function listNameToTestId(listName: string): string {
  return listName
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

/**
 * Lists for the star editor, derived from the already-loaded summary so the
 * modal does not wait on GET /favorites/item.
 */
export function itemListsFromSummary(
  summary: FavoriteSummaryDto | undefined,
  itemType: FavoriteItemType,
  itemId: number,
  isLibrarian: boolean,
): { availableLists: string[]; selectedLists: string[] } {
  const selectedLists: string[] = []
  const idField = itemType === 'BOOK' ? 'bookIds' : 'authorIds'
  for (const list of summary?.lists ?? []) {
    if (list[idField].includes(itemId)) {
      selectedLists.push(list.listName)
    }
  }
  if (summary?.availableLists && summary.availableLists.length > 0) {
    return { availableLists: summary.availableLists, selectedLists }
  }
  const availableLists = [
    ...PATRON_FAVORITE_LISTS,
    ...(isLibrarian ? LIBRARIAN_FAVORITE_LISTS : []),
  ]
  const builtInLower = new Set(availableLists.map((name) => name.toLowerCase()))
  const librarianLower = new Set(
    LIBRARIAN_FAVORITE_LISTS.map((name) => name.toLowerCase()),
  )
  const seenCustom = new Set<string>()
  const custom: string[] = []
  for (const list of summary?.lists ?? []) {
    const name = list.listName
    if (!name) continue
    const lower = name.toLowerCase()
    if (builtInLower.has(lower) || seenCustom.has(lower)) continue
    if (!isLibrarian && librarianLower.has(lower)) continue
    seenCustom.add(lower)
    custom.push(name)
  }
  custom.sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
  return { availableLists: [...availableLists, ...custom], selectedLists }
}
