// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { emuCatalogSearchUrl, stripCopySuffix, ydlCatalogSearchUrl } from '../bookTitle'

describe('stripCopySuffix', () => {
  it('removes a canonical ", c. N" suffix', () => {
    expect(stripCopySuffix('Gather Comprehensive, c. 2')).toBe('Gather Comprehensive')
  })

  it('removes ", c.2" without a space after the period', () => {
    expect(stripCopySuffix('Gather Comprehensive, c.2')).toBe('Gather Comprehensive')
  })

  it('removes ", c N" without a period', () => {
    expect(stripCopySuffix('101 Things to Do with a Baby, c 1')).toBe('101 Things to Do with a Baby')
  })

  it('is case-insensitive and trims surrounding whitespace', () => {
    expect(stripCopySuffix('  Way of the Cross, C. 3  ')).toBe('Way of the Cross')
  })

  it('leaves a title without a copy suffix unchanged', () => {
    expect(stripCopySuffix('The Spiritual Exercises')).toBe('The Spiritual Exercises')
  })

  it('does not strip a copy number in the middle of the title', () => {
    expect(stripCopySuffix('Volume 1, c. 2 extra')).toBe('Volume 1, c. 2 extra')
  })

  it('returns the original when stripping would leave the title empty', () => {
    expect(stripCopySuffix(', c. 2')).toBe(', c. 2')
  })
})

describe('ydlCatalogSearchUrl', () => {
  it('quotes the title without the copy suffix', () => {
    const url = ydlCatalogSearchUrl('Gather Comprehensive, c. 2')
    expect(url).toContain(encodeURIComponent('"Gather Comprehensive"'))
    expect(url).not.toContain('c.%202')
    expect(url).not.toContain('c. 2')
  })
})

describe('emuCatalogSearchUrl', () => {
  it('quotes the title without the copy suffix', () => {
    const url = emuCatalogSearchUrl('Gather Comprehensive, c. 2')
    expect(url).toContain(encodeURIComponent('any,contains,"Gather Comprehensive"'))
    expect(url).not.toContain('c.%202')
  })
})
