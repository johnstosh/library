// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import svg from '../../public/images/paper-books.svg?raw'

describe('paper-books background', () => {
  it('tiles on a paper-colored field', () => {
    expect(svg).toContain('fill="#F4EBDA"')
  })

  it('draws small book icons at -45, 0, and 45 degrees', () => {
    expect(svg).toContain('rotate(-45)')
    expect(svg).toContain('rotate(0)')
    expect(svg).toContain('rotate(45)')
    expect((svg.match(/width="17" height="28"/g) || []).length).toBeGreaterThanOrEqual(3)
  })

  it('paints the books in the library primary colors', () => {
    expect(svg).toContain('fill="#6B2D3C"')
    expect(svg).toContain('fill="#1F4D3A"')
    expect(svg).toContain('fill="#2C2825"')
  })
})
