// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { YdlIcon } from '../Icons'

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
