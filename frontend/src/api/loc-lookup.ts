// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface BookLocStatusDto {
  id: number
  title: string
  authorName: string
  currentLocNumber?: string
  hasLocNumber: boolean
  publicationYear?: number
  firstPhotoId?: number
  firstPhotoChecksum?: string
  dateAdded?: string
}

export interface LocLookupResultDto {
  bookId: number
  success: boolean
  locNumber?: string
  errorMessage: string
  matchCount: number
}

// Get all books with LOC status
export function useAllBooksWithLocStatus() {
  return useQuery({
    queryKey: ['loc-lookup', 'all-books'],
    queryFn: () => api.get<BookLocStatusDto[]>('/loc-bulk-lookup/books'),
    staleTime: 1000 * 60 * 5, // 5 minutes
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
