// (c) Copyright 2025 by Muczynski
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

export interface ImportExportStats {
  libraries: number
  authors: number
  books: number
  users: number
  loans: number
}

// Photo Export Types
export interface PhotoExportStatsDto {
  total: number
  exported: number
  imported: number
  pendingExport: number
  pendingImport: number
  failed: number
  inProgress: number
  completed: number
  pending: number
  albumName?: string
  albumId?: string
}

export interface PhotoExportInfoDto {
  id: number
  caption?: string
  exportStatus: string
  exportedAt?: string
  permanentId?: string
  exportErrorMessage?: string
  contentType?: string
  hasImage: boolean
  checksum?: string
  bookTitle?: string
  bookId?: number
  bookLocNumber?: string
  bookDateAdded?: string
  bookAuthorName?: string
  authorName?: string
  authorId?: number
}

export interface PhotoExportResponseDto {
  message: string
  photoId?: number
  stats?: PhotoExportStatsDto
}

export interface PhotoVerifyResultDto {
  valid: boolean
  message: string
  filename?: string
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

// Export photos as ZIP
export async function exportPhotos(): Promise<Blob> {
  const response = await fetch('/api/photo-export', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to export photos')
  }

  return response.blob()
}

// Photo Export Status Hooks

// Get photo export statistics
export function usePhotoExportStats() {
  return useQuery({
    queryKey: ['photo-export-stats'],
    queryFn: () => api.get<PhotoExportStatsDto>('/photo-export/stats'),
  })
}

// Get all photos with export info
export function usePhotoExportList() {
  return useQuery({
    queryKey: ['photo-export-list'],
    queryFn: () => api.get<PhotoExportInfoDto[]>('/photo-export/photos'),
  })
}

// Export single photo to Google Photos
export function useExportSinglePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoExportResponseDto>(`/photo-export/export/${photoId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photo-export-stats'] })
      queryClient.invalidateQueries({ queryKey: ['photo-export-list'] })
    },
  })
}

// Import single photo from Google Photos
export function useImportSinglePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoExportResponseDto>(`/photo-export/import/${photoId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photo-export-stats'] })
      queryClient.invalidateQueries({ queryKey: ['photo-export-list'] })
    },
  })
}

// Verify photo permanent ID
export function useVerifyPhoto() {
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoVerifyResultDto>(`/photo-export/verify/${photoId}`)
    },
  })
}

// Unlink photo (remove permanent ID)
export function useUnlinkPhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoExportResponseDto>(`/photo-export/unlink/${photoId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photo-export-stats'] })
      queryClient.invalidateQueries({ queryKey: ['photo-export-list'] })
    },
  })
}

// Delete photo
export function useDeletePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.delete(`/photos/${photoId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photo-export-stats'] })
      queryClient.invalidateQueries({ queryKey: ['photo-export-list'] })
    },
  })
}
