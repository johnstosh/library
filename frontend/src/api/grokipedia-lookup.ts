// (c) Copyright 2025 by Muczynski
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface GrokipediaLookupResultDto {
  bookId?: number
  authorId?: number
  name: string
  success: boolean
  grokipediaUrl?: string
  errorMessage: string
}

// Lookup Grokipedia URLs for multiple books (bulk)
export function useLookupBulkBooksGrokipedia() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookIds: number[]) =>
      api.post<GrokipediaLookupResultDto[]>('/books/grokipedia-lookup-bulk', bookIds),
    onSuccess: () => {
      // Invalidate all book queries
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Lookup Grokipedia URLs for multiple authors (bulk)
export function useLookupBulkAuthorsGrokipedia() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (authorIds: number[]) =>
      api.post<GrokipediaLookupResultDto[]>('/authors/grokipedia-lookup-bulk', authorIds),
    onSuccess: () => {
      // Invalidate all author queries
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}
