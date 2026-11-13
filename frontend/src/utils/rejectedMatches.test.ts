// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { appendHoldings, appendRejectedUrls, httpUrls, isLibriVox, liveUrlIsRejected, rejectsUrl } from './rejectedMatches'

describe('rejectedMatches', () => {
  it('matches a Grokipedia slug to the generated page', () => {
    const rows = [{ url: 'hermann_geiger', reason: 'different person' }]
    expect(rejectsUrl(rows, 'https://grokipedia.com/page/Hermann_Geiger')).toBe(true)
    expect(rejectsUrl(rows, 'https://grokipedia.com/page/Hermann_Geiger/')).toBe(true)
    expect(rejectsUrl(rows, 'https://grokipedia.com/page/Other_Person')).toBe(false)
  })

  it('sends LibriVox URLs to the audio list and other URLs to the text list', () => {
    const live = 'https://gutenberg.org/1 https://librivox.org/city-of-god'
    expect(httpUrls(live)).toEqual(['https://gutenberg.org/1', 'https://librivox.org/city-of-god'])
    expect(isLibriVox('https://librivox.org/city-of-god')).toBe(true)
    const text = [{ url: 'https://gutenberg.org/1', reason: 'wrong edition' }]
    const audio = [{ url: 'https://librivox.org/city-of-god', reason: 'wrong reading' }]
    expect(liveUrlIsRejected(live, text, 'text')).toBe(true)
    expect(liveUrlIsRejected(live, text, 'audio')).toBe(false)
    expect(liveUrlIsRejected(live, audio, 'audio')).toBe(true)
    expect(liveUrlIsRejected(live, audio, 'text')).toBe(false)
  })

  it('collapses a second add of the same URL', () => {
    const once = appendRejectedUrls([], ['https://example.com/a/'], 'dead link')
    const twice = appendRejectedUrls(once, ['https://example.com/a'], 'wrong edition')
    expect(twice).toHaveLength(1)
    expect(twice[0].reason).toBe('wrong edition')
    expect(twice[0].by).toBeUndefined()
  })

  it('collapses catalog keys for one library', () => {
    const once = appendHoldings([], 'YDL', [{ recordKey: 'Abc' }], 'wrong book')
    const twice = appendHoldings(once, 'YDL', [{ recordKey: 'abc' }], 'different edition')
    const other = appendHoldings(twice, 'EMU', [{ recordKey: 'abc' }], 'wrong book')
    expect(other).toHaveLength(2)
    expect(other[0].reason).toBe('different edition')
  })
})
