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
  it('renders a vector A with teal and amber strokes', () => {
    render(<AclaIcon />)

    const icon = screen.getByTestId('acla-icon')
    expect(icon.tagName.toLowerCase()).toBe('svg')

    const paths = icon.querySelectorAll('path')
    expect(paths).toHaveLength(3)
    expect(paths[0]).toHaveAttribute('stroke', '#0F766E')
    expect(paths[1]).toHaveAttribute('stroke', '#0F766E')
    expect(paths[2]).toHaveAttribute('stroke', '#D97706')
  })
})
