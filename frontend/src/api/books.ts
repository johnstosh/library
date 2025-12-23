// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BookDto } from '@/types/dtos'

// Hook to get all books (simple version for now)
export function useBooks(filter?: 'all' | 'most-recent' | 'without-loc') {
  return useQuery({
    queryKey: queryKeys.books.list(filter),
    queryFn: async () => {
      if (filter === 'most-recent') {
        return api.get<BookDto[]>('/books/most-recent-day')
      } else if (filter === 'without-loc') {
        return api.get<BookDto[]>('/books/without-loc')
      } else {
        return api.get<BookDto[]>('/books')
      }
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

// Hook to get a single book
export function useBook(id: number) {
  return useQuery({
    queryKey: queryKeys.books.detail(id),
    queryFn: () => api.get<BookDto>(`/books/${id}`),
    enabled: !!id,
  })
}

// Hook to create a book
export function useCreateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (book: Partial<BookDto>) => api.post<BookDto>('/books', book),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Hook to update a book
export function useUpdateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, book }: { id: number; book: Partial<BookDto> }) =>
      api.put<BookDto>(`/books/${id}`, book),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(queryKeys.books.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Hook to delete a book
export function useDeleteBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/books/${id}`),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.books.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Hook to delete multiple books
export function useDeleteBooks() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) => api.post('/books/delete-bulk', ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}
