// (c) Copyright 2025 by Muczynski
import type { BookStatus, UserAuthority } from './enums'

// Common
export interface IdDto {
  id: number
}

export interface SummaryDto {
  id: number
  lastModified: string
}

// Library DTOs
export interface LibraryDto {
  id: number
  name: string
  hostname: string
}

// Author DTOs
export interface AuthorDto {
  id: number
  firstName: string
  lastName: string
  birthDate?: string
  deathDate?: string
  briefBiography?: string
  bookCount?: number
}

export interface AuthorSummaryDto {
  id: number
  name: string
}

// Book DTOs
export interface BookDto {
  id: number
  title: string
  publicationYear?: number
  publisher?: string
  status: BookStatus
  locCallNumber?: string
  libraryId: number
  libraryName?: string
  authorId: number
  authorName?: string
  photoCount?: number
  lastModified: string
}

export interface BookSummaryDto {
  id: number
  lastModified: string
}

// Photo DTOs
export interface PhotoDto {
  id: number
  checksum: string
  displayOrder: number
  rotation: number
  bookId?: number
  authorId?: number
  dateTaken?: string
}

// User DTOs
export interface UserDto {
  id: number
  username: string
  authority: UserAuthority
  ssoSubjectId?: string
}

// Loan DTOs
export interface LoanDto {
  id: number
  bookId: number
  bookTitle?: string
  userId: number
  username?: string
  checkoutDate: string
  dueDate: string
  returnDate?: string
}

// Library Card DTOs
export interface LibraryCardDesignDto {
  id: number
  name: string
  description?: string
}

export interface ApplicationDto {
  id: number
  userId: number
  username?: string
  status: string
  appliedDate: string
  processedDate?: string
}

// Settings DTOs
export interface GlobalSettingsDto {
  id: number
  ssoEnabled: boolean
  googleClientId?: string
}
