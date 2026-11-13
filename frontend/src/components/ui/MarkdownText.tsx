// (c) Copyright 2025 by Muczynski
import type { Components } from 'react-markdown'
import Markdown from 'react-markdown'
import remarkBreaks from 'remark-breaks'
import remarkGfm from 'remark-gfm'
import { TEXT_LINK_UNDERLINE_CLASS } from '@/components/ui/IconButton'

const remarkPlugins = [remarkGfm, remarkBreaks]

/** http(s) only. javascript:, data:, and other schemes are dropped. */
function allowUrl(url: string): string {
  try {
    const parsed = new URL(url)
    if (parsed.protocol === 'http:' || parsed.protocol === 'https:') return url
  } catch {
    // not an absolute URL
  }
  return ''
}

const components: Components = {
  a: ({ href, children }) => {
    const safe = href ? allowUrl(href) : ''
    if (!safe) return <span>{children}</span>
    return (
      <a href={safe} target="_blank" rel="noopener noreferrer" className={TEXT_LINK_UNDERLINE_CLASS}>
        {children}
      </a>
    )
  },
  img: ({ src, alt }) => {
    const safe = typeof src === 'string' ? allowUrl(src) : ''
    if (!safe) return null
    return <img src={safe} alt={alt ?? ''} className="max-w-full" />
  },
  // One level down so the book title stays the only page h1, and smaller than text-2xl.
  h1: ({ children }) => <h2 className="text-lg font-bold text-gray-900">{children}</h2>,
  h2: ({ children }) => <h3 className="text-base font-bold text-gray-900">{children}</h3>,
  h3: ({ children }) => <h4 className="text-sm font-bold text-gray-900">{children}</h4>,
  h4: ({ children }) => <h5 className="text-sm font-bold text-gray-900">{children}</h5>,
  h5: ({ children }) => <h6 className="text-sm font-bold text-gray-900">{children}</h6>,
  h6: ({ children }) => <p className="text-sm font-bold text-gray-900">{children}</p>,
  ul: ({ children }) => <ul className="list-disc space-y-1 pl-5">{children}</ul>,
  ol: ({ children }) => <ol className="list-decimal space-y-1 pl-5">{children}</ol>,
  strong: ({ children }) => <strong className="font-bold">{children}</strong>,
  em: ({ children }) => <em className="italic">{children}</em>,
  blockquote: ({ children }) => (
    <blockquote className="border-l-2 border-primary-300 pl-3 text-charcoal-700">{children}</blockquote>
  ),
}

export function MarkdownText({ markdown, dataTest }: { markdown: string; dataTest?: string }) {
  return (
    <div className="space-y-6 break-words text-gray-900" data-test={dataTest}>
      {/* skipHtml drops raw HTML. Do not add rehype-raw. */}
      <Markdown remarkPlugins={remarkPlugins} skipHtml urlTransform={allowUrl} components={components}>
        {markdown}
      </Markdown>
    </div>
  )
}
