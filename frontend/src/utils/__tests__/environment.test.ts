// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { isDevSite } from '@/utils/environment'

describe('isDevSite', () => {
  it('is true for the deployed development hostname', () => {
    expect(isDevSite('library-dev.muczynskifamily.com')).toBe(true)
    expect(isDevSite('library-dev-abc123-uc.a.run.app')).toBe(true)
  })

  it('is false for production and local hosts', () => {
    expect(isDevSite('library.muczynskifamily.com')).toBe(false)
    expect(isDevSite('localhost')).toBe(false)
    expect(isDevSite('127.0.0.1')).toBe(false)
    expect(isDevSite('')).toBe(false)
  })
})
