// (c) Copyright 2025 by Muczynski
import { useQuery } from '@tanstack/react-query'
import { api } from './client'

interface TestDataPageVisibility {
  showTestDataPage: boolean
}

export function useTestDataPageVisibility() {
  return useQuery({
    queryKey: ['test-data-page-visibility'],
    queryFn: () => api.get<TestDataPageVisibility>('/global-properties/test-data-page-visibility', { requireAuth: false }),
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
  })
}
