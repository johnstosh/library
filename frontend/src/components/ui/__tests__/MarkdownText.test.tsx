// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MarkdownText } from '../MarkdownText'

const review = [
  'Kathleen O\'Meara wrote this as *Grace Ramsay*.',
  '',
  '# Review: *Are You My Wife?*',
  '',
  'A **good** novel of renunciation.',
  '',
  '## Synopsis',
  '',
  '- renunciation',
  '- confession',
  '',
  'Text: https://www.gutenberg.org/ebooks/56022',
  '',
  '[Archive](https://archive.org/details/example_book)',
  '',
  '[bad](javascript:alert(1))',
  '',
  '<script>alert(1)</script>',
  '<img src="https://evil.example/x.png" alt="evil" />',
].join('\n')

describe('MarkdownText', () => {
  it('renders headings, emphasis, lists, and links without raw HTML', () => {
    const { container } = render(<MarkdownText markdown={review} dataTest="book-detailed-description" />)
    const root = screen.getByTestId('book-detailed-description')

    const title = screen.getByRole('heading', { level: 2, name: 'Review: Are You My Wife?' })
    expect(title).toHaveClass('text-lg')
    expect(title.querySelector('em')?.textContent).toBe('Are You My Wife?')
    expect(screen.getByRole('heading', { level: 3, name: 'Synopsis' })).toHaveClass('text-base')
    expect(screen.getByText('good').tagName).toBe('STRONG')
    expect(screen.getByText('Grace Ramsay').tagName).toBe('EM')
    const items = container.querySelectorAll('li')
    expect(items).toHaveLength(2)
    expect(items[0]?.textContent).toBe('renunciation')
    expect(items[1]?.textContent).toBe('confession')
    expect(items[0]?.parentElement).toHaveClass('list-disc')
    expect(root.textContent).not.toContain('#')
    expect(root.textContent).not.toContain('*')

    const gutenberg = screen.getByRole('link', { name: 'https://www.gutenberg.org/ebooks/56022' })
    expect(gutenberg).toHaveAttribute('href', 'https://www.gutenberg.org/ebooks/56022')
    expect(gutenberg).toHaveAttribute('target', '_blank')
    expect(gutenberg).toHaveAttribute('rel', 'noopener noreferrer')

    const archive = screen.getByRole('link', { name: 'Archive' })
    expect(archive).toHaveAttribute('href', 'https://archive.org/details/example_book')
    expect(archive).toHaveAttribute('target', '_blank')
    expect(archive).toHaveAttribute('rel', 'noopener noreferrer')

    expect(screen.queryByRole('link', { name: 'bad' })).toBeNull()
    expect(screen.getByText('bad')).toBeInTheDocument()
    expect(container.querySelector('script')).toBeNull()
    expect(container.querySelector('img')).toBeNull()
    expect(container.innerHTML.toLowerCase()).not.toContain('<script')
    expect(container.innerHTML.toLowerCase()).not.toContain('<img')
    expect(container.textContent).not.toContain('alert(1)')
    expect(container.textContent).not.toContain('evil')
  })

  it('keeps plain paragraphs and single line breaks', () => {
    const { container } = render(
      <MarkdownText
        markdown={'From a Catholic perspective, this book is one paragraph.\n\nIt continues after a blank line.'}
      />,
    )
    const paragraphs = container.querySelectorAll('p')
    expect(paragraphs).toHaveLength(2)
    expect(paragraphs[0]?.textContent).toBe('From a Catholic perspective, this book is one paragraph.')
    expect(paragraphs[1]?.textContent).toBe('It continues after a blank line.')
    expect(container.querySelector('h2, h3, h4')).toBeNull()

    const broken = render(<MarkdownText markdown={'Line one.\nLine two.'} />)
    expect(broken.container.querySelectorAll('p')).toHaveLength(1)
    expect(broken.container.querySelector('br')).toBeTruthy()
    expect(broken.container.querySelector('p')?.textContent).toBe('Line one.\nLine two.')
  })
})
