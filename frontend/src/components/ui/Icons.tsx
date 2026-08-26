// (c) Copyright 2025 by Muczynski
import { clsx } from 'clsx'
import {
  PiArchive,
  PiArrowLeft,
  PiBook,
  PiBooks,
  PiBookOpen,
  PiCheckCircle,
  PiCopy,
  PiEye,
  PiPencil,
  PiTrash,
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

export function ReturnIcon({ className }: IconProps) {
  return <PiCheckCircle className={clsx(sizeClass, className)} />
}

export function BackIcon({ className }: IconProps) {
  return <PiArrowLeft className={clsx(sizeClass, className)} />
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
