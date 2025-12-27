// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { LibraryDto, LibraryStatisticsDto } from '@/types/dtos'

// Hook to get all libraries
export function useLibraries() {
  return useQuery({
    queryKey: queryKeys.libraries.list(),
    queryFn: () => api.get<LibraryDto[]>('/libraries'),
    staleTime: 1000 * 60 * 10, // 10 minutes - libraries don't change often
  })
}

// Hook to get library statistics
export function useLibraryStatistics() {
  return useQuery({
    queryKey: ['libraries', 'statistics'],
    queryFn: () => api.get<LibraryStatisticsDto[]>('/libraries/statistics'),
    staleTime: 1000 * 60 * 2, // 2 minutes - statistics change more frequently
  })
}

// Hook to get a single library
export function useLibrary(id: number) {
  return useQuery({
    queryKey: queryKeys.libraries.detail(id),
    queryFn: () => api.get<LibraryDto>(`/libraries/${id}`),
    enabled: !!id,
  })
}

// Hook to create a library
export function useCreateLibrary() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (library: Partial<LibraryDto>) => api.post<LibraryDto>('/libraries', library),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.libraries.all })
    },
  })
}

// Hook to update a library
export function useUpdateLibrary() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, library }: { id: number; library: Partial<LibraryDto> }) =>
      api.put<LibraryDto>(`/libraries/${id}`, library),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(queryKeys.libraries.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.libraries.all })
    },
  })
}

// Hook to delete a library
export function useDeleteLibrary() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/libraries/${id}`),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.libraries.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.libraries.all })
    },
  })
}
