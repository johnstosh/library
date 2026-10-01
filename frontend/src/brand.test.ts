// (c) Copyright 2026 by Muczynski
import { describe, expect, it } from 'vitest'
import { libraryBrand } from './brand'

describe('libraryBrand', () => {
  it('uses the branch name and library system outside the dev install', () => {
    expect(libraryBrand('St. Martin de Porres', 'Sacred Heart Library System', 'library.muczynskifamily.com'))
      .toEqual({ name: 'St. Martin de Porres', subName: 'Sacred Heart Library System' })
    expect(libraryBrand('Test Library', 'Test Library System', 'localhost'))
      .toEqual({ name: 'Test Library', subName: 'Test Library System' })
  })

  it('uses library-dev and DEV on the dev install', () => {
    expect(libraryBrand('St. Martin de Porres', 'Sacred Heart Library System', 'library-dev.muczynskifamily.com'))
      .toEqual({ name: 'library-dev', subName: 'DEV' })
    expect(libraryBrand('St. Martin de Porres', 'Sacred Heart Library System', 'library-dev-abc123-uc.a.run.app'))
      .toEqual({ name: 'library-dev', subName: 'DEV' })
  })
})
