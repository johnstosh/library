// (c) Copyright 2025 by Muczynski
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import svg from '../../public/images/paper-books.svg?raw'

const css = readFileSync(resolve(__dirname, '../index.css'), 'utf8')

const bookPath =
  'M208,24H72A32,32,0,0,0,40,56V224a8,8,0,0,0,8,8H192a8,8,0,0,0,0-16H56a16,16,0,0,1,16-16H208a8,8,0,0,0,8-8V32A8,8,0,0,0,208,24Zm-8,160H72a31.82,31.82,0,0,0-16,4.29V56A16,16,0,0,1,72,40H200Z'
const booksPath =
  'M231.65,194.55,198.46,36.75a16,16,0,0,0-19-12.39L132.65,34.42a16.08,16.08,0,0,0-12.3,19l33.19,157.8A16,16,0,0,0,169.16,224a16.25,16.25,0,0,0,3.38-.36l46.81-10.06A16.09,16.09,0,0,0,231.65,194.55ZM136,50.15c0-.06,0-.09,0-.09l46.8-10,3.33,15.87L139.33,66Zm6.62,31.47,46.82-10.05,3.34,15.9L146,97.53Zm6.64,31.57,46.82-10.06,13.3,63.24-46.82,10.06ZM216,197.94l-46.8,10-3.33-15.87L212.67,182,216,197.85C216,197.91,216,197.94,216,197.94ZM104,32H56A16,16,0,0,0,40,48V208a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V48A16,16,0,0,0,104,32ZM56,48h48V64H56Zm0,32h48v96H56Zm48,128H56V192h48v16Z'

describe('paper-books background', () => {
  it('tiles on a paper-colored field', () => {
    expect(svg).toContain('fill="#F4EBDA"')
    expect(svg).toContain('width="480"')
    expect(svg).toContain('height="480"')
    expect(css).toContain("url('/images/paper-books.svg')")
    expect(css).toContain('background-size: 480px 480px')
  })

  it('draws the Book Details outline icon instead of solid rectangles', () => {
    expect(svg).toContain('id="book"')
    expect(svg).toContain(bookPath)
    expect(svg).toContain('href="#book"')
    expect(svg).not.toContain('width="17" height="28"')
  })

  it('sometimes uses the Open in Books stacked-books outline', () => {
    expect(svg).toContain('id="books"')
    expect(svg).toContain(booksPath)
    expect(svg).toContain('href="#books"')
    const bookUses = (svg.match(/href="#book"/g) || []).length
    const booksUses = (svg.match(/href="#books"/g) || []).length
    expect(bookUses).toBeGreaterThanOrEqual(6)
    expect(booksUses).toBeGreaterThanOrEqual(6)
  })

  it('paints the books in the library primary colors', () => {
    expect(svg).toContain('fill="#6B2D3C"')
    expect(svg).toContain('fill="#1F4D3A"')
    expect(svg).toContain('fill="#2C2825"')
  })

  it('scatters icons at varied rotations and sizes', () => {
    const rotations = [...svg.matchAll(/rotate\((-?\d+)\)/g)].map((match) => Number(match[1]))
    const sizes = [...svg.matchAll(/width="(\d+)" height="\1"/g)]
      .map((match) => Number(match[1]))
      .filter((size) => size < 100)

    expect(rotations.length).toBeGreaterThan(0)
    expect(rotations.every((angle) => angle >= -45 && angle <= 45)).toBe(true)
    expect(new Set(rotations).size).toBeGreaterThanOrEqual(12)
    expect(rotations.some((angle) => Math.abs(angle) % 45 !== 0)).toBe(true)
    expect(new Set(sizes).size).toBeGreaterThanOrEqual(5)
    expect(Math.max(...sizes) - Math.min(...sizes)).toBeGreaterThanOrEqual(10)
  })
})
