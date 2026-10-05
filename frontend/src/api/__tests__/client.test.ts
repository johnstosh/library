// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { parseApiErrorMessage } from '../client'

describe('parseApiErrorMessage', () => {
  it('extracts message from a JSON error object', () => {
    expect(
      parseApiErrorMessage('{"error":"BUSINESS_RULE_VIOLATION","message":"Username already exists"}')
    ).toBe('Username already exists')
  })

  it('unwraps a JSON string body', () => {
    expect(parseApiErrorMessage('"A user named \'Jane Doe\' already exists"')).toBe(
      "A user named 'Jane Doe' already exists"
    )
  })

  it('uses the raw body when the response is not JSON', () => {
    expect(parseApiErrorMessage('A user named \'Jane Doe\' already exists')).toBe(
      "A user named 'Jane Doe' already exists"
    )
  })

  it('falls back when the body is empty', () => {
    expect(parseApiErrorMessage('', 'Failed to approve')).toBe('Failed to approve')
  })

  it('reads the plain Grok out-of-credits message from the 402 body', () => {
    const message =
      'Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.'
    expect(
      parseApiErrorMessage(JSON.stringify({ error: message, message, code: 'GROK_CREDITS_EXHAUSTED' }))
    ).toBe(message)
  })
})
