// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { loansNewPathFromBook } from '../loanCheckout'

describe('loansNewPathFromBook', () => {
  it('prefills book, author, call number, and patron', () => {
    expect(
      loansNewPathFromBook(
        { id: 7, title: 'Initial Book', author: 'Lewis', locNumber: 'PZ 7 .M16' },
        'testuser',
      ),
    ).toBe(
      '/loans/new?bookId=7&title=Initial+Book&author=Lewis&locNumber=PZ+7+.M16&borrower=testuser',
    )
  })

  it('omits blank optional fields', () => {
    expect(loansNewPathFromBook({ id: 3, title: 'Narnia' })).toBe(
      '/loans/new?bookId=3&title=Narnia',
    )
  })
})
