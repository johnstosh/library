// (c) Copyright 2025 by Muczynski
import { create } from 'zustand'

interface TableState {
  selectedIds: Set<number>
  selectAll: boolean
}

type TableName = 'booksTable' | 'authorsTable' | 'usersTable' | 'loansTable'
type FilterFeature = 'books' | 'authors'

interface UiState {
  // Table selection state per feature
  booksTable: TableState
  authorsTable: TableState
  usersTable: TableState
  loansTable: TableState

  // Filter state per feature
  booksFilter: 'all' | 'most-recent' | 'without-loc'
  authorsFilter: 'all' | 'without-description' | 'zero-books' | 'without-grokipedia' | 'most-recent'
  loansShowAll: boolean

  // Actions
  setSelectedIds: (table: TableName, ids: Set<number>) => void
  toggleSelectAll: (table: TableName) => void
  clearSelection: (table: TableName) => void
  setFilter: (feature: FilterFeature, filter: string) => void
  setLoansShowAll: (showAll: boolean) => void
  toggleRowSelection: (table: TableName, id: number) => void
}

export const useUiStore = create<UiState>((set) => ({
  // Initial state
  booksTable: { selectedIds: new Set(), selectAll: false },
  authorsTable: { selectedIds: new Set(), selectAll: false },
  usersTable: { selectedIds: new Set(), selectAll: false },
  loansTable: { selectedIds: new Set(), selectAll: false },

  booksFilter: 'most-recent',
  authorsFilter: 'most-recent',
  loansShowAll: false,

  // Actions
  setSelectedIds: (table, ids) =>
    set((state) => ({
      [table]: { ...state[table], selectedIds: ids },
    })),

  toggleSelectAll: (table) =>
    set((state) => ({
      [table]: { ...state[table], selectAll: !state[table].selectAll },
    })),

  clearSelection: (table) =>
    set(() => ({
      [table]: { selectedIds: new Set(), selectAll: false },
    })),

  setFilter: (feature, filter) =>
    set({ [`${feature}Filter`]: filter }),

  setLoansShowAll: (showAll) => set({ loansShowAll: showAll }),

  toggleRowSelection: (table, id) =>
    set((state) => {
      const tableState = state[table]
      const newSelectedIds = new Set(tableState.selectedIds)

      if (newSelectedIds.has(id)) {
        newSelectedIds.delete(id)
      } else {
        newSelectedIds.add(id)
      }

      return {
        [table]: {
          ...tableState,
          selectedIds: newSelectedIds,
          selectAll: false, // Uncheck select-all when individual rows are toggled
        },
      }
    }),
}))

// Helper hooks for specific tables
export const useBooksTableSelection = () => useUiStore((state) => state.booksTable)
export const useAuthorsTableSelection = () => useUiStore((state) => state.authorsTable)
export const useUsersTableSelection = () => useUiStore((state) => state.usersTable)
export const useLoansTableSelection = () => useUiStore((state) => state.loansTable)

// Helper hooks for filters
export const useBooksFilter = () => useUiStore((state) => state.booksFilter)
export const useAuthorsFilter = () => useUiStore((state) => state.authorsFilter)
export const useLoansShowAll = () => useUiStore((state) => state.loansShowAll)
