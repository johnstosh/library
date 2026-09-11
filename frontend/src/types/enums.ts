// (c) Copyright 2025 by Muczynski

export const BookStatus = {
  ACTIVE: 'ACTIVE',
  LOST: 'LOST',
  WITHDRAWN: 'WITHDRAWN',
  ON_ORDER: 'ON_ORDER',
  REQUESTED: 'REQUESTED',
} as const

export type BookStatus = (typeof BookStatus)[keyof typeof BookStatus]
export const ReadingDifficulty = {
  CHILDREN: 'children',
  ACCESSIBLE: 'accessible',
  MODERATE: 'moderate',
  DEMANDING: 'demanding',
  ADVANCED: 'advanced',
  UNSET: 'unset',
} as const

export type ReadingDifficulty = (typeof ReadingDifficulty)[keyof typeof ReadingDifficulty]

export const UserAuthority = {
  LIBRARIAN: 'LIBRARIAN',
  USER: 'USER',
} as const

export type UserAuthority = (typeof UserAuthority)[keyof typeof UserAuthority]

export const BookCoverType = {
  HARDCOVER: 'HARDCOVER',
  SOFTCOVER: 'SOFTCOVER',
} as const

export type BookCoverType = (typeof BookCoverType)[keyof typeof BookCoverType]
