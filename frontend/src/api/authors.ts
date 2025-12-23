// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { AuthorDto, BookDto } from '@/types/dtos'

// Hook to get all authors
export function useAuthors(filter?: 'all' | 'without-description' | 'zero-books') {
  return useQuery({
    queryKey: queryKeys.authors.list(filter),
    queryFn: async () => {
      const authors = await api.get<AuthorDto[]>('/authors')

      // Client-side filtering until backend supports it
      if (filter === 'without-description') {
        return authors.filter((a) => !a.briefBiography)
      } else if (filter === 'zero-books') {
        return authors.filter((a) => a.bookCount === 0)
      }

      return authors
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
