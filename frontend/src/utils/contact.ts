// (c) Copyright 2025 by Muczynski

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_ALLOWED = /^[+0-9().\-\s]+$/

export function isValidOptionalEmail(email: string): boolean {
  const trimmed = email.trim()
  return trimmed === '' || EMAIL_PATTERN.test(trimmed)
}

export function isValidOptionalPhone(phone: string): boolean {
  const trimmed = phone.trim()
  if (trimmed === '') return true
  if (trimmed.length > 32) return false
  if (!PHONE_ALLOWED.test(trimmed)) return false
  const digits = (trimmed.match(/\d/g) || []).length
  return digits >= 7 && digits <= 15
}
