// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { homePathForUser } from '@/stores/authStore'
import type { CurrentUser } from '@/stores/authStore'

function user(authority: CurrentUser['authority']): CurrentUser {
  return { id: 1, username: 'test', authority }
}

describe('homePathForUser', () => {
  it('sends librarians to the books catalog', () => {
    expect(homePathForUser(user('LIBRARIAN'))).toBe('/books')
  })

  it('sends patrons to Search', () => {
    expect(homePathForUser(user('USER'))).toBe('/search')
  })

  it('sends unknown or missing users to Search', () => {
    expect(homePathForUser(null)).toBe('/search')
    expect(homePathForUser(undefined)).toBe('/search')
  })
})
