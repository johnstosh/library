// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { BranchDto, BranchStatisticsDto } from '@/types/dtos'

// Hook to get all branches
export function useBranches() {
  return useQuery({
    queryKey: queryKeys.branches.list(),
    queryFn: () => api.get<BranchDto[]>('/branches'),
    staleTime: 1000 * 60 * 10, // 10 minutes - branches don't change often
  })
}

// Hook to get branch statistics
export function useBranchStatistics() {
  return useQuery({
    queryKey: ['branches', 'statistics'],
    queryFn: () => api.get<BranchStatisticsDto[]>('/branches/statistics'),
    staleTime: 1000 * 60 * 2, // 2 minutes - statistics change more frequently
  })
}

// Hook to get a single branch
export function useBranch(id: number) {
  return useQuery({
    queryKey: queryKeys.branches.detail(id),
    queryFn: () => api.get<BranchDto>(`/branches/${id}`),
    enabled: !!id,
  })
}

// Hook to create a branch
export function useCreateBranch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (branch: Partial<BranchDto>) => api.post<BranchDto>('/branches', branch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.branches.all })
    },
  })
}

// Hook to update a branch
export function useUpdateBranch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, branch }: { id: number; branch: Partial<BranchDto> }) =>
      api.put<BranchDto>(`/branches/${id}`, branch),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(queryKeys.branches.detail(variables.id), data)
      queryClient.invalidateQueries({ queryKey: queryKeys.branches.all })
    },
  })
}

// Hook to delete a branch
export function useDeleteBranch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/branches/${id}`),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.branches.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.branches.all })
    },
  })
}
