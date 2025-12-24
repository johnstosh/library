// (c) Copyright 2025 by Muczynski
import { useMutation } from '@tanstack/react-query'
import { api } from './client'

export interface ImportExportStats {
  libraries: number
  authors: number
  books: number
  users: number
  loans: number
}

// Export JSON data
export async function exportJsonData(): Promise<Blob> {
  const response = await fetch('/api/import/json', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to export data')
  }

  const data = await response.json()
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json',
  })

  return blob
}

// Import JSON data
export function useImportJsonData() {
  return useMutation({
    mutationFn: async (file: File) => {
      const text = await file.text()
      const data = JSON.parse(text)
      return api.post<string>('/import/json', data)
    },
  })
}

// Export photos
export async function exportPhotos(): Promise<Blob> {
  const response = await fetch('/api/photo-export', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to export photos')
  }

  return response.blob()
}
