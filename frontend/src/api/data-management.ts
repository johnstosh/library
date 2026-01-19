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

// Import response from backend
export interface ImportResponseDto {
  success: boolean
  message: string
  counts?: {
    libraries: number
    authors: number
    users: number
    books: number
    loans: number
    photos: number
  }
}

// Database statistics from the backend (total counts from database)
export interface DatabaseStatsDto {
  libraryCount: number
  bookCount: number
  authorCount: number
  userCount: number
  loanCount: number
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
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (file: File) => {
      const text = await file.text()
      let data
      try {
        data = JSON.parse(text)
      } catch (parseError) {
        console.error('Failed to parse import file as JSON:', parseError)
        throw new Error('Invalid JSON file format. Please check the file and try again.')
      }
      console.log('Importing data:', {
        libraries: data.libraries?.length || 0,
        authors: data.authors?.length || 0,
        users: data.users?.length || 0,
        books: data.books?.length || 0,
        loans: data.loans?.length || 0,
        photos: data.photos?.length || 0,
      })
      const response = await api.post<ImportResponseDto>('/import/json', data)
      console.log('Import response:', response)
      if (!response.success) {
        throw new Error(response.message || 'Import failed')
      }
      return response
    },
    onSuccess: () => {
      // Invalidate all queries to refresh data after import
      queryClient.invalidateQueries({ queryKey: ['database-stats'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
      queryClient.invalidateQueries({ queryKey: ['authors'] })
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['loans'] })
      queryClient.invalidateQueries({ queryKey: ['libraries'] })
    },
  })
}

// Get database statistics (total counts from database)
export function useDatabaseStats() {
  return useQuery({
    queryKey: ['database-stats'],
    queryFn: () => api.get<DatabaseStatsDto>('/import/stats'),
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

// Upload/replace photo image
export function useUploadPhotoImage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ photoId, file }: { photoId: number; file: File }) => {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(`/api/photos/${photoId}/crop`, {
        method: 'PUT',
        body: formData,
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error('Failed to upload photo image')
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photo-export-stats'] })
      queryClient.invalidateQueries({ queryKey: ['photo-export-list'] })
    },
  })
}
