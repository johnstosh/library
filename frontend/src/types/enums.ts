// (c) Copyright 2025 by Muczynski

export const BookStatus = {
  ACTIVE: 'ACTIVE',
  LOST: 'LOST',
  WITHDRAWN: 'WITHDRAWN',
  ON_ORDER: 'ON_ORDER',
} as const

export type BookStatus = (typeof BookStatus)[keyof typeof BookStatus]

export const UserAuthority = {
  LIBRARIAN: 'LIBRARIAN',
  USER: 'USER',
} as const

export type UserAuthority = (typeof UserAuthority)[keyof typeof UserAuthority]
