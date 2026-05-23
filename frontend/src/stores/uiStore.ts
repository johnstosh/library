// (c) Copyright 2025 by Muczynski
import { create } from 'zustand'

interface TableState {
  selectedIds: Set<number>
  selectAll: boolean
}

type TableName = 'booksTable' | 'authorsTable' | 'usersTable' | 'loansTable'
type FilterFeature = 'authors'

/**
 * Independent boolean chip filters for the Books page.
 * All active chips are AND-ed together — a book must satisfy every active chip.
 *
 * Row 1 (search-style chips, client-side): inLibrary, electronic, freeText, audio
 * Row 2 (books-specific chips, client-side): mostRecent, withoutLoc, threeLetterLoc,
 *   withoutGrokipedia, withoutGenres, notActiveStatus, withoutFreeTextUrls
 */
export interface BooksChips {
  // Row 1: search-page–style type filters
  inLibrary: boolean
  electronic: boolean
  freeText: boolean
  audio: boolean
  // Row 2: books-specific filters
  mostRecent: boolean
  withoutLoc: boolean
  threeLetterLoc: boolean
  withoutGrokipedia: boolean
  withoutGenres: boolean
  notActiveStatus: boolean
  withoutFreeTextUrls: boolean
}

const defaultBooksChips: BooksChips = {
  inLibrary: false,
  electronic: false,
  freeText: false,
  audio: false,
  mostRecent: true,
  withoutLoc: false,
  threeLetterLoc: false,
  withoutGrokipedia: false,
  withoutGenres: false,
  notActiveStatus: false,
  withoutFreeTextUrls: false,
}

interface UiState {
  // Table selection state per feature
  booksTable: TableState
  authorsTable: TableState
  usersTable: TableState
  loansTable: TableState

  // Books chip filter state (all AND-combined, client-side)
  booksChips: BooksChips

  // Authors filter
  authorsFilter: 'all' | 'without-description' | 'zero-books' | 'without-grokipedia' | 'most-recent'
  loansShowAll: boolean

  // Label filter state for books
  booksLabelFilter: string[]

  // Actions
  setSelectedIds: (table: TableName, ids: Set<number>) => void
  toggleSelectAll: (table: TableName) => void
  clearSelection: (table: TableName) => void
  setFilter: (feature: FilterFeature, filter: string) => void
  setLoansShowAll: (showAll: boolean) => void
  toggleRowSelection: (table: TableName, id: number) => void
  toggleBooksLabel: (label: string) => void
  clearBooksLabels: () => void
  toggleBooksChip: (chip: keyof BooksChips) => void
  clearBooksChips: () => void
}

export const useUiStore = create<UiState>((set) => ({
  // Initial state
  booksTable: { selectedIds: new Set(), selectAll: false },
  authorsTable: { selectedIds: new Set(), selectAll: false },
  usersTable: { selectedIds: new Set(), selectAll: false },
  loansTable: { selectedIds: new Set(), selectAll: false },

  booksChips: { ...defaultBooksChips },
  authorsFilter: 'most-recent',
  loansShowAll: false,
  booksLabelFilter: [],

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

  setFilter: (feature, filter) => set({ [`${feature}Filter`]: filter }),

  setLoansShowAll: (showAll) => set({ loansShowAll: showAll }),

  toggleBooksLabel: (label) =>
    set((state) => {
      const current = state.booksLabelFilter
      const next = current.includes(label) ? current.filter((l) => l !== label) : [...current, label]
      return { booksLabelFilter: next }
    }),

  clearBooksLabels: () => set({ booksLabelFilter: [] }),

  toggleBooksChip: (chip) =>
    set((state) => ({
      booksChips: { ...state.booksChips, [chip]: !state.booksChips[chip] },
    })),

  clearBooksChips: () => set({ booksChips: { ...defaultBooksChips } }),

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
export const useBooksChips = () => useUiStore((state) => state.booksChips)
export const useAuthorsFilter = () => useUiStore((state) => state.authorsFilter)
export const useLoansShowAll = () => useUiStore((state) => state.loansShowAll)
export const useBooksLabelFilter = () => useUiStore((state) => state.booksLabelFilter)
