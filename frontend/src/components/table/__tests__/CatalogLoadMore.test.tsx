// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CatalogLoadMore } from '../CatalogLoadMore'

function nearBottom(scrollY: number) {
  Object.defineProperty(window, 'innerHeight', { configurable: true, value: 800 })
  Object.defineProperty(window, 'scrollY', { configurable: true, writable: true, value: scrollY })
  Object.defineProperty(document.documentElement, 'scrollHeight', {
    configurable: true,
    value: scrollY + 800 + 100,
  })
}

function scroll() {
  fireEvent.scroll(window)
}

function Harness() {
  const [loading, setLoading] = useState(false)
  const [pages, setPages] = useState(1)
  return (
    <>
      <div data-test="pages">{pages}</div>
      <CatalogLoadMore
        hasMore={pages < 5}
        isLoadingMore={loading}
        shownCount={pages * 100}
        onLoadMore={() => {
          setLoading(true)
          setPages((count) => count + 1)
        }}
      />
      <button type="button" data-test="settle" onClick={() => setLoading(false)}>
        Settle
      </button>
    </>
  )
}

describe('CatalogLoadMore', () => {
  it('loads one page per scroll and waits until that page settles', () => {
    nearBottom(0)
    render(<Harness />)

    for (let i = 0; i < 8; i++) scroll()
    expect(screen.getByTestId('pages')).toHaveTextContent('2')

    nearBottom(40)
    for (let i = 0; i < 8; i++) scroll()
    expect(screen.getByTestId('pages')).toHaveTextContent('2')

    fireEvent.click(screen.getByTestId('settle'))
    nearBottom(40)
    for (let i = 0; i < 8; i++) scroll()
    expect(screen.getByTestId('pages')).toHaveTextContent('2')

    nearBottom(80)
    scroll()
    expect(screen.getByTestId('pages')).toHaveTextContent('3')
    scroll()
    expect(screen.getByTestId('pages')).toHaveTextContent('3')
  })

  it('does not auto-load while the current page is still loading', () => {
    nearBottom(200)
    const onLoadMore = () => {
      throw new Error('load more')
    }
    render(
      <CatalogLoadMore hasMore isLoadingMore shownCount={100} onLoadMore={onLoadMore} />,
    )
    expect(() => {
      for (let i = 0; i < 5; i++) scroll()
    }).not.toThrow()
  })
})
