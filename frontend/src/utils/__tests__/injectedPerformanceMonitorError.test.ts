// (c) Copyright 2025 by Muczynski
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  installInjectedPerformanceMonitorErrorGuard,
  isInjectedPerformanceMonitorError,
} from '../injectedPerformanceMonitorError'

const CHROME_MESSAGE = "Cannot read properties of undefined (reading 'startTime')"
const CONSOLE_MESSAGE = `Uncaught TypeError: ${CHROME_MESSAGE}`

function chromiumStack(brackets = true): string {
  const loc = brackets ? '<anonymous>:2:19429' : 'anonymous:2:19429'
  return [
    `TypeError: ${CHROME_MESSAGE}`,
    `    at et.reportAllChanges (${loc})`,
    `    at ${brackets ? '<anonymous>' : 'anonymous'}:2:13070`,
    `    at ${brackets ? '<anonymous>' : 'anonymous'}:2:331`,
    '    at d (anonymous:2:6141)',
  ].join('\n')
}

describe('isInjectedPerformanceMonitorError', () => {
  it('matches the Chromium console stack from issue 322', () => {
    expect(
      isInjectedPerformanceMonitorError(CONSOLE_MESSAGE, {
        message: CHROME_MESSAGE,
        stack: chromiumStack(false),
      }),
    ).toBe(true)
  })

  it('matches the DevTools <anonymous> frame form', () => {
    expect(
      isInjectedPerformanceMonitorError(CHROME_MESSAGE, {
        message: CHROME_MESSAGE,
        stack: chromiumStack(true),
      }),
    ).toBe(true)
  })

  it('still matches when Chromium fills the page URL as filename', () => {
    expect(
      isInjectedPerformanceMonitorError(
        CONSOLE_MESSAGE,
        { message: CHROME_MESSAGE, stack: chromiumStack(true) },
        'http://localhost:8080/login',
      ),
    ).toBe(true)
  })

  it('rejects the same TypeError from application code', () => {
    expect(
      isInjectedPerformanceMonitorError(CHROME_MESSAGE, {
        message: CHROME_MESSAGE,
        stack: `TypeError: ${CHROME_MESSAGE}\n    at loadBooks (http://localhost/assets/index.js:12:5)`,
      }, 'http://localhost/assets/index.js'),
    ).toBe(false)
  })

  it('rejects other TypeErrors from anonymous scripts', () => {
    expect(
      isInjectedPerformanceMonitorError(
        "Cannot read properties of undefined (reading 'foo')",
        {
          message: "Cannot read properties of undefined (reading 'foo')",
          stack: chromiumStack(true).replaceAll('startTime', 'foo'),
        },
      ),
    ).toBe(false)
  })

  it('rejects startTime errors without a reportAllChanges frame', () => {
    expect(
      isInjectedPerformanceMonitorError(CHROME_MESSAGE, {
        message: CHROME_MESSAGE,
        stack: `TypeError: ${CHROME_MESSAGE}\n    at <anonymous>:2:10`,
      }),
    ).toBe(false)
  })
})

describe('installInjectedPerformanceMonitorErrorGuard', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('prevents the Chromium monitor TypeError and leaves real errors alone', () => {
    installInjectedPerformanceMonitorErrorGuard()

    const injectedError = new Error(CHROME_MESSAGE)
    injectedError.stack = chromiumStack(true)
    const injected = new ErrorEvent('error', {
      message: CONSOLE_MESSAGE,
      filename: '',
      lineno: 2,
      colno: 19429,
      error: injectedError,
      cancelable: true,
    })
    window.dispatchEvent(injected)
    expect(injected.defaultPrevented).toBe(true)

    const appError = new Error("Cannot read properties of undefined (reading 'title')")
    appError.stack = `${appError.message}\n    at BookViewPage (http://localhost/assets/BookViewPage.js:40:12)`
    const real = new ErrorEvent('error', {
      message: appError.message,
      filename: 'http://localhost/assets/BookViewPage.js',
      lineno: 40,
      colno: 12,
      error: appError,
      cancelable: true,
    })
    window.dispatchEvent(real)
    expect(real.defaultPrevented).toBe(false)
  })
})
