// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { LibraryCardDesignDto } from '@/types/dtos'

export interface AppliedDto {
  id: number
  name: string
  status?: 'PENDING' | 'APPROVED' | 'REJECTED'
}

export interface RegistrationRequest {
  username: string
  password: string
  authority: string
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
      api.post('/application/public/register', data, { requireAuth: false }),
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

export function useLibraryCardDesigns() {
  return useQuery({
    queryKey: ['library-card-designs'],
    queryFn: () => api.get<LibraryCardDesignDto[]>('/library-card/designs'),
    staleTime: Infinity,
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

// Function to print all library card designs as a multi-page PDF
export async function printAllLibraryCards(): Promise<Blob> {
  const response = await fetch('/api/library-card/print-all', {
    credentials: 'include',
  })
  if (!response.ok) {
    throw new Error('Failed to generate all library card PDFs')
  }
  return response.blob()
}
