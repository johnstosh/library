// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { authorNeedsCanonicalName, toCanonicalAuthorName } from '@/utils/canonicalAuthorName'
import { titleNeedsChicagoCase, toChicagoTitleCase } from '@/utils/chicagoTitleCase'
import { applyChipFilters, defaultBookChipFilters } from '@/utils/bookChipFilters'
import type { BookDto } from '@/types/dtos'

const DOMINIC = 'The History of St. Dominic: Founder of the Friars Preachers'

describe('Chicago title case', () => {
  it('rewrites lower, upper, RDA, and every-word caps, including the subtitle', () => {
    expect(toChicagoTitleCase('the history of st. dominic: founder of the friars preachers')).toBe(DOMINIC)
    expect(toChicagoTitleCase('THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS')).toBe(DOMINIC)
    expect(toChicagoTitleCase('The history of St. Dominic : founder of the Friars Preachers')).toBe(DOMINIC)
    expect(toChicagoTitleCase('The History Of St. Dominic: Founder Of The Friars Preachers')).toBe(DOMINIC)
    expect(toChicagoTitleCase(DOMINIC)).toBe(DOMINIC)
    expect(titleNeedsChicagoCase(DOMINIC)).toBe(false)
    expect(titleNeedsChicagoCase('THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS')).toBe(true)
  })

  it('capitalizes the first subtitle word and lowers short prepositions', () => {
    expect(toChicagoTitleCase('of mice and men')).toBe('Of Mice and Men')
    expect(toChicagoTitleCase('gone with the wind')).toBe('Gone with the Wind')
    expect(toChicagoTitleCase('foo: the bar')).toBe('Foo: The Bar')
    expect(toChicagoTitleCase('world war ii')).toBe('World War II')
    expect(toChicagoTitleCase('the mix of things')).toBe('The Mix of Things')
    expect(toChicagoTitleCase('the lord of the rings, c. 2')).toBe('The Lord of the Rings, c. 2')
    expect(toChicagoTitleCase('THE LORD OF THE RINGS, C. 2')).toBe('The Lord of the Rings, c. 2')
  })
})

describe('canonical author names', () => {
  it('inverts commas, strips years, and expands parenthetical initials', () => {
    expect(toCanonicalAuthorName('Simpson, Richard')).toBe('Richard Simpson')
    expect(toCanonicalAuthorName('Simpson, Richard, 1920-1995')).toBe('Richard Simpson')
    expect(toCanonicalAuthorName('Richard Simpson (1920-1995)')).toBe('Richard Simpson')
    expect(toCanonicalAuthorName('King, Martin Luther, Jr.')).toBe('Martin Luther King Jr.')
    expect(toCanonicalAuthorName('Johnson, B. J.-P. (Barney John-Paul)')).toBe('Barney John Paul Johnson')
    expect(toCanonicalAuthorName('Tolkien, J. R. R. (John Ronald Reuel), 1892-1973')).toBe(
      'John Ronald Reuel Tolkien',
    )
    expect(toCanonicalAuthorName('Johnson, B. J.')).toBe('B. J. Johnson')
    expect(toCanonicalAuthorName('B. J.-P. Johnson')).toBe('B. J.-P. Johnson')
    expect(authorNeedsCanonicalName('Simpson, Richard')).toBe(true)
    expect(authorNeedsCanonicalName('Richard Simpson')).toBe(false)
    expect(authorNeedsCanonicalName('B. J. Johnson')).toBe(false)
  })
})

describe('naming chips', () => {
  function book(overrides: Partial<BookDto>): BookDto {
    return {
      id: 1,
      title: 'The History of St. Dominic: Founder of the Friars Preachers',
      author: 'Richard Simpson',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      ...overrides,
    }
  }

  it('keeps only books that fail the selected naming rule', () => {
    const chicago = book({ id: 1 })
    const lower = book({ id: 2, title: 'the history of st. dominic: founder of the friars preachers' })
    const inverted = book({ id: 3, author: 'Simpson, Richard' })

    expect(applyChipFilters([chicago, lower, inverted], {
      ...defaultBookChipFilters,
      titleNotChicago: true,
    }).map((row) => row.id)).toEqual([2])

    expect(applyChipFilters([chicago, lower, inverted], {
      ...defaultBookChipFilters,
      authorNotCanonical: true,
    }).map((row) => row.id)).toEqual([3])
  })
})
