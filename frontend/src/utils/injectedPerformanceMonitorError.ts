// (c) Copyright 2025 by Muczynski

/**
 * Chromium's live performance monitor (DevTools Performance panel / former Web
 * Vitals extension) injects a minified anonymous script into the page. Its
 * `reportAllChanges` timer reads `entries[0].startTime` after the entry has
 * already been dropped, which surfaces as:
 *
 *   Uncaught TypeError: Cannot read properties of undefined (reading 'startTime')
 *       at et.reportAllChanges (<anonymous>:2:19429)
 *
 * The script runs in the page's renderer, so the browser reports it as an
 * uncaught app error even though no library code is on the stack. The same
 * column numbers appear on unrelated sites (for example Zabbix ZBX-28096).
 */

const START_TIME_MESSAGE =
  /Cannot read properties of undefined \(reading ['"]startTime['"]\)/

const REPORT_ALL_CHANGES_FRAME =
  /\breportAllChanges \((?:<anonymous>|anonymous):2:\d+\)/

export function isInjectedPerformanceMonitorError(
  message?: string | null,
  error?: { message?: string; stack?: string } | null,
  filename?: string | null,
): boolean {
  // Bundled app files are never the Chromium monitor. Other filenames (empty,
  // "<anonymous>", or the page URL Chromium fills in on synthetic ErrorEvents)
  // are allowed so the stack frame is the deciding signal.
  if (filename && /\/assets\/.+\.(?:js|mjs)(?:\?|$)/.test(filename)) {
    return false
  }

  const text = `${message ?? ''} ${error?.message ?? ''}`
  if (!START_TIME_MESSAGE.test(text)) {
    return false
  }

  const stack = typeof error?.stack === 'string' ? error.stack : ''
  return REPORT_ALL_CHANGES_FRAME.test(stack)
}

let installed = false

export function installInjectedPerformanceMonitorErrorGuard(): void {
  if (typeof window === 'undefined' || installed) {
    return
  }
  installed = true

  window.addEventListener(
    'error',
    (event: ErrorEvent) => {
      if (
        isInjectedPerformanceMonitorError(event.message, event.error, event.filename)
      ) {
        event.preventDefault()
      }
    },
    true,
  )
}
