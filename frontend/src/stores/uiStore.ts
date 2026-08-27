// (c) Copyright 2025 by Muczynski
import { create } from 'zustand'
import {
  defaultBookChipFilters,
  type BookChipFilters,
} from '@/utils/bookChipFilters'
import {
  defaultAuthorChipFilters,
  type AuthorChipFilters,
} from '@/utils/authorChipFilters'
import {
  defaultLoanChipFilters,
  type LoanChipFilters,
} from '@/utils/loanChipFilters'
import {
  defaultUserChipFilters,
  type UserChipFilters,
} from '@/utils/userChipFilters'

interface TableState {
  selectedIds: Set<number>
  selectAll: boolean
}

type TableName = 'booksTable' | 'authorsTable' | 'usersTable' | 'loansTable'

/**
 * Independent boolean chip filters for the Books page.
 * All active chips are AND-ed together — a book must satisfy every active chip.
 *
 * Row 1: inLibrary, electronic, freeText, audio
 * Row 2: mostRecent, withoutLoc, threeLetterLoc, withoutGrokipedia,
 *   withoutGenres, notActiveStatus, withoutFreeTextUrls
 */
export type BooksChips = BookChipFilters
export type AuthorsChips = AuthorChipFilters
export type LoansChips = LoanChipFilters
export type UsersChips = UserChipFilters

const defaultBooksChips: BooksChips = { ...defaultBookChipFilters }
const defaultAuthorsChips: AuthorsChips = { ...defaultAuthorChipFilters }
const defaultLoansChips: LoansChips = { ...defaultLoanChipFilters }
const defaultUsersChips: UsersChips = { ...defaultUserChipFilters }

interface UiState {
  // Table selection state per feature
  booksTable: TableState
  authorsTable: TableState
  usersTable: TableState
  loansTable: TableState

  // Books chip filter state (all AND-combined, client-side)
  booksChips: BooksChips

  authorsChips: AuthorsChips
  loansChips: LoansChips
  usersChips: UsersChips
  usersSearchQuery: string

  // Label filter state for books
  booksLabelFilter: string[]

  // Actions
  setSelectedIds: (table: TableName, ids: Set<number>) => void
  toggleSelectAll: (table: TableName) => void
  clearSelection: (table: TableName) => void
  toggleLoansChip: (chip: keyof LoansChips) => void
  clearLoansChips: () => void
  toggleRowSelection: (table: TableName, id: number) => void
  toggleBooksLabel: (label: string) => void
  clearBooksLabels: () => void
  toggleBooksChip: (chip: keyof BooksChips) => void
  clearBooksChips: () => void
  toggleAuthorsChip: (chip: keyof AuthorsChips) => void
  clearAuthorsChips: () => void
  toggleUsersChip: (chip: keyof UsersChips) => void
  clearUsersChips: () => void
  setUsersSearchQuery: (query: string) => void
}

export const useUiStore = create<UiState>((set) => ({
  // Initial state
  booksTable: { selectedIds: new Set(), selectAll: false },
  authorsTable: { selectedIds: new Set(), selectAll: false },
  usersTable: { selectedIds: new Set(), selectAll: false },
  loansTable: { selectedIds: new Set(), selectAll: false },

  booksChips: { ...defaultBooksChips },
  authorsChips: { ...defaultAuthorsChips },
  loansChips: { ...defaultLoansChips },
  usersChips: { ...defaultUsersChips },
  usersSearchQuery: '',
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

  toggleLoansChip: (chip) =>
    set((state) => ({
      loansChips: { ...state.loansChips, [chip]: !state.loansChips[chip] },
    })),

  clearLoansChips: () => set({ loansChips: { ...defaultLoansChips } }),

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

  toggleAuthorsChip: (chip) =>
    set((state) => ({
      authorsChips: { ...state.authorsChips, [chip]: !state.authorsChips[chip] },
    })),

  clearAuthorsChips: () => set({ authorsChips: { ...defaultAuthorsChips } }),

  toggleUsersChip: (chip) =>
    set((state) => ({
      usersChips: { ...state.usersChips, [chip]: !state.usersChips[chip] },
    })),

  clearUsersChips: () => set({ usersChips: { ...defaultUsersChips } }),

  setUsersSearchQuery: (query) => set({ usersSearchQuery: query }),

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
export const useAuthorsChips = () => useUiStore((state) => state.authorsChips)
export const useLoansChips = () => useUiStore((state) => state.loansChips)
export const useUsersChips = () => useUiStore((state) => state.usersChips)
export const useUsersSearchQuery = () => useUiStore((state) => state.usersSearchQuery)
export const useBooksLabelFilter = () => useUiStore((state) => state.booksLabelFilter)
