// (c) Copyright 2025 by Muczynski
import { clsx } from 'clsx'
import {
  PiArchive,
  PiArrowLeft,
  PiBook,
  PiBooks,
  PiBookOpen,
  PiCheckCircle,
  PiClock,
  PiCopy,
  PiEye,
  PiHeadphones,
  PiPencil,
  PiTrash,
  PiSparkle,
  PiUser,
} from 'react-icons/pi'

interface IconProps {
  className?: string
}

const sizeClass = 'w-5 h-5'

export function ViewIcon({ className }: IconProps) {
  return <PiEye className={clsx(sizeClass, className)} />
}

export function BookIcon({ className }: IconProps) {
  return <PiBook className={clsx(sizeClass, className)} />
}

export function EditIcon({ className }: IconProps) {
  return <PiPencil className={clsx(sizeClass, className)} />
}

export function DeleteIcon({ className }: IconProps) {
  return <PiTrash className={clsx(sizeClass, className)} />
}

export function CopyIcon({ className }: IconProps) {
  return <PiCopy className={clsx(sizeClass, className)} />
}

export function AuthorIcon({ className }: IconProps) {
  return <PiUser className={clsx(sizeClass, className)} />
}

export function BooksIcon({ className }: IconProps) {
  return <PiBooks className={clsx(sizeClass, className)} />
}

export function LocIcon({ className }: IconProps) {
  return <PiArchive className={clsx(sizeClass, className)} />
}

export function FreeTextIcon({ className }: IconProps) {
  return <PiBookOpen className={clsx(sizeClass, className)} />
}

export function FreeAudioIcon({ className }: IconProps) {
  return <PiHeadphones className={clsx(sizeClass, className)} />
}

export function ReturnIcon({ className }: IconProps) {
  return <PiCheckCircle className={clsx(sizeClass, className)} />
}

export function BackIcon({ className }: IconProps) {
  return <PiArrowLeft className={clsx(sizeClass, className)} />
}

export function AiIcon({ className }: IconProps) {
  return <PiSparkle className={clsx(sizeClass, className)} />
}

export function GrokipediaIcon({ className }: IconProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center justify-center w-5 h-5 rounded text-[11px] font-bold leading-none border border-current',
        className
      )}
      aria-hidden
    >
      G
    </span>
  )
}

/**
 * Script capital Y for Ypsilanti District Library.
 * Drawn as an SVG (not a font glyph) so it matches on web and phone:
 * red left arm, yellow right arm, blue stem.
 */
export function YdlIcon({ className }: IconProps) {
  return (
    <svg
      className={clsx(sizeClass, className)}
      viewBox="0 0 24 24"
      fill="none"
      overflow="visible"
      aria-hidden
      data-test="ydl-icon"
    >
      <path
        d="M4.5 8C4.2 4.6 8.6 3.4 10 6.9C10.9 8.9 11.5 10.2 12 11.2"
        stroke="#E03131"
        strokeWidth="2.85"
        strokeLinecap="round"
      />
      <path
        d="M20.4 4.2C18.6 2.8 15.4 4.8 14.4 7.2C13.6 8.8 12.8 10.2 12 11.2"
        stroke="#E6B422"
        strokeWidth="2.85"
        strokeLinecap="round"
      />
      <path
        d="M12 11.2C12.5 15 11.9 18.8 9.9 21C8.4 22.2 6 21.4 5.7 19.2"
        stroke="#2563EB"
        strokeWidth="2.85"
        strokeLinecap="round"
      />
    </svg>
  )
}

/** Analog clock for EMU Halle Library. */
export function EmuIcon({ className }: IconProps) {
  return <PiClock className={clsx(sizeClass, className)} />
}

/**
 * Allegheny County Library Association mark: three folded blue
 * book-pillars (light face + navy face) and two green wrapping swooshes.
 * Drawn as SVG so it matches on web and phone at toolbar size.
 */
export function AclaIcon({ className }: IconProps) {
  return (
    <svg
      className={clsx(sizeClass, className)}
      viewBox="0 0 24 24"
      fill="none"
      overflow="visible"
      aria-hidden
      data-test="acla-icon"
    >
      <polygon fill="#1973B1" points="5.25,4.63 7.88,6.75 7.88,21.50 5.25,19.50" />
      <polygon fill="#214098" points="8.38,6.50 11.00,4.63 11.00,19.38 8.38,21.50" />
      <polygon fill="#1973B1" points="11.38,2.38 14.00,4.38 14.00,19.13 11.38,17.25" />
      <polygon fill="#214098" points="14.50,4.25 17.00,2.25 17.00,17.13 14.50,19.13" />
      <polygon fill="#1973B1" points="17.50,4.50 20.13,6.63 20.13,21.38 17.50,19.50" />
      <polygon fill="#214098" points="20.63,6.63 23.25,4.63 23.25,19.38 20.63,21.50" />
      <path
        d="M 0.7 12.55 C 0.0 9.35, 7.6 9.9, 16.25 12.2"
        stroke="#8CC449"
        strokeWidth="1.4"
        strokeLinecap="round"
      />
      <path
        d="M 1.55 13.5 C 4.0 12.5, 9.5 13.4, 13.9 13.92"
        stroke="#8CC449"
        strokeWidth="1.05"
        strokeLinecap="round"
      />
    </svg>
  )
}
