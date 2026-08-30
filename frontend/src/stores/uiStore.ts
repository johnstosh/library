// (c) Copyright 2025 by Muczynski
import { create } from 'zustand'
import {
  defaultBookChipFilters,
  isOtherBookChipActive,
  type BookChipFilters,
} from '@/utils/bookChipFilters'
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

/**
 * Independent boolean chip filters for the Books page.
 * All active chips are AND-ed together — a book must satisfy every active chip.
 *
 * Row 1: hasYdlAudio, hasYdlBook, hasYdlEbook, hasEmuAudio, hasEmuBook, hasEmuEbook
 * Row 2: inLibrary, electronic, freeText, audio
 * Row 3: mostRecent, withoutLoc, withoutGrokipedia, withGrokipedia,
 *   withoutGenres, notActiveStatus, withoutFreeTextUrls
 */
export type BooksChips = BookChipFilters
export type AuthorsChips = AuthorChipFilters
export type LoansChips = LoanChipFilters
export type UsersChips = UserChipFilters
export type ApplicationsChips = ApplicationChipFilters

// Most Recent Day starts on so the books list hits GET /books/most-recent-day
// instead of GET /books/summaries. That filter endpoint is a much smaller query
// and is the fast path for the default books list. Search keeps the shared
// defaultBookChipFilters (all chips off).
const defaultBooksChips: BooksChips = { ...defaultBookChipFilters, mostRecent: true }
// Most Recent Day starts on so the authors list hits GET /authors/most-recent-day
// instead of GET /authors/summaries. That filter endpoint is a much smaller query
// and is the fast path for the default authors list.
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

  // Books chip filter state (all AND-combined, client-side)
  booksChips: BooksChips

  authorsChips: AuthorsChips
  loansChips: LoansChips
  usersChips: UsersChips
  usersSearchQuery: string
  applicationsChips: ApplicationsChips
  applicationsSearchQuery: string

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

  booksChips: { ...defaultBooksChips },
  authorsChips: { ...defaultAuthorsChips },
  loansChips: { ...defaultLoansChips },
  usersChips: { ...defaultUsersChips },
  usersSearchQuery: '',
  applicationsChips: { ...defaultApplicationsChips },
  applicationsSearchQuery: '',
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
      const nextLabels = current.includes(label)
        ? current.filter((l) => l !== label)
        : [...current, label]
      const othersOn = nextLabels.length > 0 || isOtherBookChipActive(state.booksChips)
      // Most Recent Day cannot be combined with other filters (client-side AND
      // on the small most-recent-day result set is misleading). Restore it when
      // nothing else is on — that is the books page default fast path.
      return {
        booksLabelFilter: nextLabels,
        booksChips: { ...state.booksChips, mostRecent: !othersOn },
      }
    }),

  clearBooksLabels: () =>
    set((state) => ({
      booksLabelFilter: [],
      booksChips: {
        ...state.booksChips,
        mostRecent: !isOtherBookChipActive(state.booksChips),
      },
    })),

  toggleBooksChip: (chip) =>
    set((state) => {
      const labelsOn = state.booksLabelFilter.length > 0
      if (chip === 'mostRecent' && (isOtherBookChipActive(state.booksChips) || labelsOn)) {
        return { booksChips: { ...state.booksChips, mostRecent: false } }
      }
      const next = { ...state.booksChips, [chip]: !state.booksChips[chip] }
      const othersOn = isOtherBookChipActive(next) || labelsOn
      next.mostRecent = othersOn ? false : chip === 'mostRecent' ? next.mostRecent : true
      return { booksChips: next }
    }),

  clearBooksChips: () => set({ booksChips: { ...defaultBooksChips } }),

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
export const useBooksChips = () => useUiStore((state) => state.booksChips)
export const useAuthorsChips = () => useUiStore((state) => state.authorsChips)
export const useLoansChips = () => useUiStore((state) => state.loansChips)
export const useUsersChips = () => useUiStore((state) => state.usersChips)
export const useUsersSearchQuery = () => useUiStore((state) => state.usersSearchQuery)
export const useApplicationsChips = () => useUiStore((state) => state.applicationsChips)
export const useApplicationsSearchQuery = () => useUiStore((state) => state.applicationsSearchQuery)
export const useBooksLabelFilter = () => useUiStore((state) => state.booksLabelFilter)
