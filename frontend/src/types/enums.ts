// (c) Copyright 2025 by Muczynski

export const BookStatus = {
  AVAILABLE: 'AVAILABLE',
  CHECKED_OUT: 'CHECKED_OUT',
  LOST: 'LOST',
  DAMAGED: 'DAMAGED',
} as const

export type BookStatus = (typeof BookStatus)[keyof typeof BookStatus]

export const UserAuthority = {
  LIBRARIAN: 'LIBRARIAN',
  USER: 'USER',
} as const

export type UserAuthority = (typeof UserAuthority)[keyof typeof UserAuthority]
