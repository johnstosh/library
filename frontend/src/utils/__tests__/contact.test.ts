// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { isValidOptionalEmail, isValidOptionalPhone } from '../contact'

describe('isValidOptionalEmail', () => {
  it('allows blank', () => {
    expect(isValidOptionalEmail('')).toBe(true)
    expect(isValidOptionalEmail('   ')).toBe(true)
  })

  it('accepts a typical address', () => {
    expect(isValidOptionalEmail('pat@example.com')).toBe(true)
  })

  it('rejects an invalid address', () => {
    expect(isValidOptionalEmail('not-an-email')).toBe(false)
  })
})

describe('isValidOptionalPhone', () => {
  it('allows blank', () => {
    expect(isValidOptionalPhone('')).toBe(true)
    expect(isValidOptionalPhone('  ')).toBe(true)
  })

  it('accepts formatted numbers', () => {
    expect(isValidOptionalPhone('(555) 123-4567')).toBe(true)
    expect(isValidOptionalPhone('+1 555 123 4567')).toBe(true)
  })

  it('rejects too few digits or letters', () => {
    expect(isValidOptionalPhone('123')).toBe(false)
    expect(isValidOptionalPhone('call-me')).toBe(false)
  })
})
