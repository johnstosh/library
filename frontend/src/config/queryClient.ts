// (c) Copyright 2025 by Muczynski
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes default
      gcTime: 1000 * 60 * 30, // 30 minutes garbage collection
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnMount: 'always',
    },
    mutations: {
      retry: 0,
    },
  },
})

// Query key factory pattern
export const queryKeys = {
  books: {
    all: ['books'] as const,
    summaries: () => [...queryKeys.books.all, 'summaries'] as const,
    filterSummaries: (filter: string) => [...queryKeys.books.all, 'filterSummaries', filter] as const,
    byIds: (ids: number[], filter?: string) => [...queryKeys.books.all, 'byIds', ids.join(','), filter] as const,
    list: (filter?: string) => [...queryKeys.books.all, 'list', filter] as const,
    detail: (id: number) => [...queryKeys.books.all, 'detail', id] as const,
    photos: (id: number) => [...queryKeys.books.all, id, 'photos'] as const,
  },
  authors: {
    all: ['authors'] as const,
    summaries: () => [...queryKeys.authors.all, 'summaries'] as const,
    filterSummaries: (filter: string) => [...queryKeys.authors.all, 'filterSummaries', filter] as const,
    byIds: (ids: number[], filter?: string) => [...queryKeys.authors.all, 'byIds', ids.join(','), filter] as const,
    list: (filter?: string) => [...queryKeys.authors.all, 'list', filter] as const,
    detail: (id: number) => [...queryKeys.authors.all, 'detail', id] as const,
    photos: (id: number) => [...queryKeys.authors.all, id, 'photos'] as const,
    books: (id: number) => [...queryKeys.authors.all, id, 'books'] as const,
  },
  branches: {
    all: ['branches'] as const,
    list: () => [...queryKeys.branches.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.branches.all, 'detail', id] as const,
  },
  loans: {
    all: ['loans'] as const,
    list: (showAll?: boolean) => [...queryKeys.loans.all, 'list', showAll] as const,
    detail: (id: number) => [...queryKeys.loans.all, 'detail', id] as const,
  },
  users: {
    all: ['users'] as const,
    me: () => [...queryKeys.users.all, 'me'] as const,
    list: () => [...queryKeys.users.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.users.all, 'detail', id] as const,
  },
  photos: {
    all: ['photos'] as const,
    book: (bookId: number) => [...queryKeys.photos.all, 'book', bookId] as const,
    author: (authorId: number) => [...queryKeys.photos.all, 'author', authorId] as const,
    image: (id: number, checksum: string) =>
      [...queryKeys.photos.all, 'image', id, checksum] as const,
    thumbnail: (id: number, checksum: string, width: number) =>
      [...queryKeys.photos.all, 'thumbnail', id, checksum, width] as const,
  },
  libraryCards: {
    all: ['library-cards'] as const,
    me: () => [...queryKeys.libraryCards.all, 'me'] as const,
    designs: () => [...queryKeys.libraryCards.all, 'designs'] as const,
    applications: () => [...queryKeys.libraryCards.all, 'applications'] as const,
  },
  settings: {
    all: ['settings'] as const,
    global: () => [...queryKeys.settings.all, 'global'] as const,
  },
}
