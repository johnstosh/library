// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { GlobalSettingsDto } from '@/types/dtos'

// Global Settings API

export function useGlobalSettings() {
  return useQuery({
    queryKey: ['global-settings'],
    queryFn: () => api.get<GlobalSettingsDto>('/global-settings'),
  })
}

export function useUpdateGlobalSettings() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (settings: Partial<GlobalSettingsDto>) =>
      api.put<GlobalSettingsDto>('/global-settings', settings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['global-settings'] })
    },
  })
}

export function useSsoStatus() {
  return useQuery({
    queryKey: ['sso-status'],
    queryFn: () => api.get<{ ssoConfigured: boolean }>('/global-settings/sso-status', { requireAuth: false }),
  })
}

// User Settings API (password change)

export function useChangePassword() {
  return useMutation({
    mutationFn: (data: { currentPassword: string; newPassword: string }) =>
      api.put('/users/change-password', data),
  })
}
