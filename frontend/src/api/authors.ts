// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { AuthorDto, BookDto } from '@/types/dtos'

// Hook to get all authors with server-side filtering
export function useAuthors(filter?: 'all' | 'without-description' | 'zero-books' | 'most-recent') {
  return useQuery({
    queryKey: queryKeys.authors.list(filter),
    queryFn: async () => {
      // Use backend filter endpoints for better performance
      if (filter === 'without-description') {
        return api.get<AuthorDto[]>('/authors/without-description')
      } else if (filter === 'zero-books') {
        return api.get<AuthorDto[]>('/authors/zero-books')
      } else if (filter === 'most-recent') {
        return api.get<AuthorDto[]>('/authors/most-recent-day')
      }

      return api.get<AuthorDto[]>('/authors')
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

// Hook to get a single author
export function useAuthor(id: number) {
  return useQuery({
    queryKey: queryKeys.authors.detail(id),
    queryFn: () => api.get<AuthorDto>(`/authors/${id}`),
    enabled: !!id,
  })
}

// Hook to get books by author
export function useAuthorBooks(id: number) {
  return useQuery({
    queryKey: queryKeys.authors.books(id),
    queryFn: () => api.get<BookDto[]>(`/authors/${id}/books`),
    enabled: !!id,
  })
}

// Hook to create an author
export function useCreateAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (author: Partial<AuthorDto>) => api.post<AuthorDto>('/authors', author),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to update an author
export function useUpdateAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, author }: { id: number; author: Partial<AuthorDto> }) =>
      api.put<AuthorDto>(`/authors/${id}`, author),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(queryKeys.authors.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to delete an author
export function useDeleteAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/authors/${id}`),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.authors.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Hook to delete multiple authors
export function useDeleteAuthors() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) => api.post('/authors/delete-bulk', ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}
