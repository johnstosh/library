// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface BookLocStatusDto {
  id: number
  title: string
  author: string
  publicationYear?: number
  locCallNumber?: string
  hasLoc: boolean
}

export interface LocLookupResultDto {
  bookId: number
  bookTitle: string
  success: boolean
  locCallNumber?: string
  message: string
}

// Get all books with LOC status
export function useAllBooksWithLocStatus() {
  return useQuery({
    queryKey: ['loc-lookup', 'all-books'],
    queryFn: () => api.get<BookLocStatusDto[]>('/loc-bulk-lookup/books'),
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

// Get books missing LOC numbers
export function useBooksWithMissingLoc() {
  return useQuery({
    queryKey: ['loc-lookup', 'missing-loc'],
    queryFn: () => api.get<BookLocStatusDto[]>('/loc-bulk-lookup/books/missing'),
    staleTime: 1000 * 60 * 5,
  })
}

// Get books from most recent date
export function useBooksFromMostRecent() {
  return useQuery({
    queryKey: ['loc-lookup', 'most-recent'],
    queryFn: () => api.get<BookLocStatusDto[]>('/loc-bulk-lookup/books/most-recent'),
    staleTime: 1000 * 60 * 5,
  })
}

// Lookup LOC for a single book
export function useLookupSingleBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookId: number) =>
      api.post<LocLookupResultDto>(`/loc-bulk-lookup/lookup/${bookId}`, {}),
    onSuccess: (data) => {
      // Invalidate book queries to refresh data
      queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(data.bookId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: ['loc-lookup'] })
    },
  })
}

// Lookup LOC for multiple books (bulk)
export function useLookupBulkBooks() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookIds: number[]) =>
      api.post<LocLookupResultDto[]>('/loc-bulk-lookup/lookup-bulk', bookIds),
    onSuccess: () => {
      // Invalidate all book queries
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: ['loc-lookup'] })
    },
  })
}

// Lookup LOC for all books missing LOC numbers
export function useLookupAllMissing() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => api.post<LocLookupResultDto[]>('/loc-bulk-lookup/lookup-all-missing', {}),
    onSuccess: () => {
      // Invalidate all book queries
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: ['loc-lookup'] })
    },
  })
}
