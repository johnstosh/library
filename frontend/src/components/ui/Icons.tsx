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

/** Script capital Y for Ypsilanti District Library. */
export function YdlIcon({ className }: IconProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center justify-center w-5 h-5 text-[17px] leading-none italic',
        className
      )}
      style={{ fontFamily: 'cursive, Georgia, "Times New Roman", serif' }}
      aria-hidden
    >
      Y
    </span>
  )
}

/** Analog clock for EMU Halle Library. */
export function EmuIcon({ className }: IconProps) {
  return <PiClock className={clsx(sizeClass, className)} />
}
