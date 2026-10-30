// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'

/** Matches Tailwind's `sm` breakpoint: true below 640px. */
const PHONE_QUERY = '(max-width: 639px)'

function phoneMatches(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  return window.matchMedia(PHONE_QUERY).matches
}

/** True when the viewport is a phone. Desktop, including tests without matchMedia, stays false. */
export function usePhoneLayout(): boolean {
  const [phone, setPhone] = useState(phoneMatches)

  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return
    const media = window.matchMedia(PHONE_QUERY)
    const onChange = () => setPhone(media.matches)
    onChange()
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  return phone
}
