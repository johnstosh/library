// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { AclaIcon, YdlIcon } from '../Icons'

describe('YdlIcon', () => {
  it('renders a vector script Y with red, yellow, and blue strokes', () => {
    render(<YdlIcon />)

    const icon = screen.getByTestId('ydl-icon')
    expect(icon.tagName.toLowerCase()).toBe('svg')

    const paths = icon.querySelectorAll('path')
    expect(paths).toHaveLength(3)
    expect(paths[0]).toHaveAttribute('stroke', '#E03131')
    expect(paths[1]).toHaveAttribute('stroke', '#E6B422')
    expect(paths[2]).toHaveAttribute('stroke', '#2563EB')
  })
})

describe('AclaIcon', () => {
  it('renders three folded blue book-pillars and two green wrapping swooshes', () => {
    render(<AclaIcon />)

    const icon = screen.getByTestId('acla-icon')
    expect(icon.tagName.toLowerCase()).toBe('svg')

    const faces = icon.querySelectorAll('polygon')
    expect(faces).toHaveLength(6)
    expect(faces[0]).toHaveAttribute('fill', '#1973B1')
    expect(faces[1]).toHaveAttribute('fill', '#214098')
    expect(faces[2]).toHaveAttribute('fill', '#1973B1')
    expect(faces[3]).toHaveAttribute('fill', '#214098')
    expect(faces[4]).toHaveAttribute('fill', '#1973B1')
    expect(faces[5]).toHaveAttribute('fill', '#214098')

    const swooshes = icon.querySelectorAll('path')
    expect(swooshes).toHaveLength(2)
    expect(swooshes[0]).toHaveAttribute('stroke', '#8CC449')
    expect(swooshes[1]).toHaveAttribute('stroke', '#8CC449')
  })
})
