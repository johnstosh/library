// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyBookBindingFilter,
  bindingsFromSearchParams,
  bookBindingLabel,
  bookBindingDisplay,
  normalizeBookBinding,
} from '@/utils/bookBinding'

describe('bookBindingLabel', () => {
  it('labels each binding and treats missing as Unknown', () => {
    expect(bookBindingLabel('HARDCOVER')).toBe('Hardcover')
    expect(bookBindingLabel('LIBRARY_BINDING')).toBe('Library Binding')
    expect(bookBindingLabel('OTHER')).toBe('Other')
    expect(bookBindingLabel(null)).toBe('Unknown')
    expect(bookBindingLabel('')).toBe('Unknown')
  })
})

describe('bookBindingDisplay', () => {
  it('shows "Electronic resource" for electronic books regardless of binding', () => {
    expect(bookBindingDisplay(null, true)).toBe('Electronic resource')
    expect(bookBindingDisplay('HARDCOVER', true)).toBe('Electronic resource')
    expect(bookBindingDisplay('UNKNOWN', true)).toBe('Electronic resource')
  })

  it('falls back to binding label for non-electronic books (object form not supported in this impl)', () => {
    expect(bookBindingDisplay('HARDCOVER', false)).toBe('Hardcover')
    expect(bookBindingDisplay(null, false)).toBe('Unknown')
    expect(bookBindingDisplay('LIBRARY_BINDING', false)).toBe('Library Binding')
    expect(bookBindingDisplay(null)).toBe('Unknown') // electronicResource defaults to false
  })
})

describe('bindingsFromSearchParams', () => {
  it('parses unique known bindings and ignores junk', () => {
    expect(
      bindingsFromSearchParams(
        new URLSearchParams('binding=HARDCOVER,UNKNOWN,bogus,hardcover'),
      ),
    ).toEqual(['HARDCOVER', 'UNKNOWN'])
    expect(bindingsFromSearchParams(new URLSearchParams('q=narnia'))).toEqual([])
  })
})

describe('normalizeBookBinding', () => {
  it('maps blank and unknown tokens to UNKNOWN', () => {
    expect(normalizeBookBinding(null)).toBe('UNKNOWN')
    expect(normalizeBookBinding('library_binding')).toBe('LIBRARY_BINDING')
  })
})

describe('applyBookBindingFilter', () => {
  it('ORs selected bindings and treats missing as Unknown', () => {
    const books = [
      { id: 1, binding: 'HARDCOVER' },
      { id: 2, binding: 'LIBRARY_BINDING' },
      { id: 3, binding: null },
      { id: 4, binding: '' },
    ]
    expect(
      applyBookBindingFilter(books, ['HARDCOVER', 'UNKNOWN']).map((book) => book.id),
    ).toEqual([1, 3, 4])
  })
})
