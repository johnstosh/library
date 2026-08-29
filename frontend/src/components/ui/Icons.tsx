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
