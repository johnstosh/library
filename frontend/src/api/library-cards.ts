// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

export interface AppliedDto {
  id: number
  name: string
  password: string
}

export interface RegistrationRequest {
  username: string
  password: string
}

// Hook to get all library card applications (librarian only)
export function useApplications() {
  return useQuery({
    queryKey: ['applications'],
    queryFn: () => api.get<AppliedDto[]>('/applied'),
    staleTime: 1000 * 60 * 2, // 2 minutes
  })
}

// Hook to apply for a library card (public)
export function useApplyForCard() {
  return useMutation({
    mutationFn: (data: RegistrationRequest) =>
      api.post('/public/register', data, { requireAuth: false }),
  })
}

// Hook to approve an application (librarian only)
export function useApproveApplication() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.post(`/applied/${id}/approve`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
    },
  })
}

// Hook to delete an application (librarian only)
export function useDeleteApplication() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/applied/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
    },
  })
}

// Function to print library card PDF for current user
export async function printLibraryCard(): Promise<Blob> {
  const response = await fetch('/api/library-card/print', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to generate library card PDF')
  }

  return response.blob()
}
