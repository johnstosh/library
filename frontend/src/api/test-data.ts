// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

interface TestDataResponse {
  success: boolean
  message: string
}

interface TestDataStats {
  books: number
  authors: number
  loans: number
}

export function useTestDataStats() {
  return useQuery({
    queryKey: ['test-data-stats'],
    queryFn: () => api.get<TestDataStats>('/test-data/stats', { requireAuth: false }),
    refetchInterval: 3000, // Refetch every 3 seconds when page is active
  })
}

export function useGenerateTestData() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (numBooks: number) =>
      api.post<TestDataResponse>('/test-data/generate', { numBooks }, { requireAuth: false }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-stats'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
      queryClient.invalidateQueries({ queryKey: ['authors'] })
    },
  })
}

export function useGenerateLoanData() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (numLoans: number) =>
      api.post<TestDataResponse>('/test-data/generate-loans', { numLoans }, { requireAuth: false }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-stats'] })
      queryClient.invalidateQueries({ queryKey: ['loans'] })
    },
  })
}

export function useDeleteAllTestData() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => api.delete('/test-data/delete-all', { requireAuth: false }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-stats'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
      queryClient.invalidateQueries({ queryKey: ['authors'] })
      queryClient.invalidateQueries({ queryKey: ['loans'] })
    },
  })
}

export function useTotalPurge() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => api.delete('/test-data/total-purge', { requireAuth: false }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-data-stats'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
      queryClient.invalidateQueries({ queryKey: ['authors'] })
      queryClient.invalidateQueries({ queryKey: ['loans'] })
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['libraries'] })
    },
  })
}
