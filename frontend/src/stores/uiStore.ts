// (c) Copyright 2025 by Muczynski
import { create } from 'zustand'
import { type BookChipFilters } from '@/utils/bookChipFilters'
import {
  defaultAuthorChipFilters,
  isOtherAuthorChipActive,
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
import {
  defaultApplicationChipFilters,
  type ApplicationChipFilters,
} from '@/utils/applicationChipFilters'

interface TableState {
  selectedIds: Set<number>
  selectAll: boolean
}

type TableName = 'booksTable' | 'authorsTable' | 'usersTable' | 'loansTable'

export type BooksChips = BookChipFilters
export type AuthorsChips = AuthorChipFilters
export type LoansChips = LoanChipFilters
export type UsersChips = UserChipFilters
export type ApplicationsChips = ApplicationChipFilters

// Most Recent Day starts on so the authors list hits GET /authors/most-recent-day
// instead of GET /authors/summaries. That filter endpoint is a much smaller query
// and is the fast path for the default authors list. Books chips live in the
// /books URL (see bookFilterParams.ts), not in this store.
const defaultAuthorsChips: AuthorsChips = { ...defaultAuthorChipFilters, mostRecent: true }
const defaultLoansChips: LoansChips = { ...defaultLoanChipFilters }
const defaultUsersChips: UsersChips = { ...defaultUserChipFilters }
const defaultApplicationsChips: ApplicationsChips = { ...defaultApplicationChipFilters }

interface UiState {
  // Table selection state per feature
  booksTable: TableState
  authorsTable: TableState
  usersTable: TableState
  loansTable: TableState

  authorsChips: AuthorsChips
  loansChips: LoansChips
  usersChips: UsersChips
  usersSearchQuery: string
  applicationsChips: ApplicationsChips
  applicationsSearchQuery: string

  // Actions
  setSelectedIds: (table: TableName, ids: Set<number>) => void
  toggleSelectAll: (table: TableName) => void
  clearSelection: (table: TableName) => void
  toggleLoansChip: (chip: keyof LoansChips) => void
  clearLoansChips: () => void
  toggleRowSelection: (table: TableName, id: number) => void
  toggleAuthorsChip: (chip: keyof AuthorsChips) => void
  clearAuthorsChips: () => void
  toggleUsersChip: (chip: keyof UsersChips) => void
  clearUsersChips: () => void
  setUsersSearchQuery: (query: string) => void
  toggleApplicationsChip: (chip: keyof ApplicationsChips) => void
  clearApplicationsChips: () => void
  setApplicationsSearchQuery: (query: string) => void
}

export const useUiStore = create<UiState>((set) => ({
  // Initial state
  booksTable: { selectedIds: new Set(), selectAll: false },
  authorsTable: { selectedIds: new Set(), selectAll: false },
  usersTable: { selectedIds: new Set(), selectAll: false },
  loansTable: { selectedIds: new Set(), selectAll: false },

  authorsChips: { ...defaultAuthorsChips },
  loansChips: { ...defaultLoansChips },
  usersChips: { ...defaultUsersChips },
  usersSearchQuery: '',
  applicationsChips: { ...defaultApplicationsChips },
  applicationsSearchQuery: '',

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

  toggleAuthorsChip: (chip) =>
    set((state) => {
      if (chip === 'mostRecent' && isOtherAuthorChipActive(state.authorsChips)) {
        return { authorsChips: { ...state.authorsChips, mostRecent: false } }
      }
      const next = { ...state.authorsChips, [chip]: !state.authorsChips[chip] }
      const othersOn = isOtherAuthorChipActive(next)
      next.mostRecent = othersOn ? false : chip === 'mostRecent' ? next.mostRecent : true
      return { authorsChips: next }
    }),

  clearAuthorsChips: () => set({ authorsChips: { ...defaultAuthorsChips } }),

  toggleUsersChip: (chip) =>
    set((state) => ({
      usersChips: { ...state.usersChips, [chip]: !state.usersChips[chip] },
    })),

  clearUsersChips: () => set({ usersChips: { ...defaultUsersChips } }),

  setUsersSearchQuery: (query) => set({ usersSearchQuery: query }),

  toggleApplicationsChip: (chip) =>
    set((state) => ({
      applicationsChips: { ...state.applicationsChips, [chip]: !state.applicationsChips[chip] },
    })),

  clearApplicationsChips: () => set({ applicationsChips: { ...defaultApplicationsChips } }),

  setApplicationsSearchQuery: (query) => set({ applicationsSearchQuery: query }),

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
export const useAuthorsChips = () => useUiStore((state) => state.authorsChips)
export const useLoansChips = () => useUiStore((state) => state.loansChips)
export const useUsersChips = () => useUiStore((state) => state.usersChips)
export const useUsersSearchQuery = () => useUiStore((state) => state.usersSearchQuery)
export const useApplicationsChips = () => useUiStore((state) => state.applicationsChips)
export const useApplicationsSearchQuery = () => useUiStore((state) => state.applicationsSearchQuery)
