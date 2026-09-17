// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyBookBindingFilter,
  bindingsFromSearchParams,
  bookBindingLabel,
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
