// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { feastDayToWire, formatFeastDay } from '@/utils/feastDay'

describe('feast day', () => {
  it('shows August 11 and March 19 without a year', () => {
    expect(formatFeastDay('--08-11')).toBe('August 11')
    expect(formatFeastDay('--03-19')).toBe('March 19')
    expect(formatFeastDay('2000-08-11')).toBe('August 11')
    expect(formatFeastDay('2000-08-11')).not.toContain('2000')
  })

  it('keeps February 29 and rejects a day that month does not have', () => {
    expect(formatFeastDay('--02-29')).toBe('February 29')
    expect(feastDayToWire('February 29')).toBe('--02-29')
    expect(formatFeastDay('--02-30')).toBe('')
    expect(() => feastDayToWire('February 30')).toThrow(/August 11/)
  })

  it('turns a typed month and day into a yearless value and leaves a blank field empty', () => {
    expect(feastDayToWire('August 11')).toBe('--08-11')
    expect(feastDayToWire('  march 19 ')).toBe('--03-19')
    expect(feastDayToWire('')).toBeUndefined()
    expect(feastDayToWire('   ')).toBeUndefined()
    expect(() => feastDayToWire('August 11, 2000')).toThrow(/year/)
  })
})
