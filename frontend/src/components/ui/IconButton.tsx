// (c) Copyright 2025 by Muczynski
import type { ButtonHTMLAttributes, MouseEvent, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { clsx } from 'clsx'

export type IconButtonTone =
  | 'neutral'
  | 'primary'
  | 'danger'
  | 'success'
  | 'warning'
  | 'info'
  | 'accent'

const TONE_CLASSES: Record<IconButtonTone, string> = {
  neutral: 'text-gray-600 hover:text-gray-900 hover:bg-gray-100',
  primary: 'text-primary-600 hover:text-primary-800 hover:bg-primary-50',
  danger: 'text-red-600 hover:text-red-800 hover:bg-red-50',
  success: 'text-green-600 hover:text-green-800 hover:bg-green-50',
  warning: 'text-amber-600 hover:text-amber-800 hover:bg-amber-50',
  info: 'text-teal-600 hover:text-teal-800 hover:bg-teal-50',
  accent: 'text-purple-600 hover:text-purple-800 hover:bg-purple-50',
}

export const TEXT_LINK_CLASS = 'text-primary-600 hover:text-primary-800'
export const TEXT_LINK_UNDERLINE_CLASS = 'text-primary-600 hover:text-primary-800 underline'

export interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: ReactNode
  label: string
  tone?: IconButtonTone
  to?: string
  href?: string
  'data-test'?: string
}

export function IconButton({
  icon,
  label,
  tone = 'neutral',
  to,
  href,
  onClick,
  disabled,
  className,
  type = 'button',
  'data-test': dataTest,
  ...props
}: IconButtonProps) {
  const classes = clsx(
    'inline-flex items-center justify-center rounded-md p-1.5 transition-colors',
    'focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-primary-500',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    disabled && 'opacity-50 cursor-not-allowed pointer-events-none',
    TONE_CLASSES[tone],
    className
  )

  const handleClick = (event: MouseEvent<HTMLElement>) => {
    if (disabled) {
      event.preventDefault()
      event.stopPropagation()
      return
    }
    onClick?.(event as MouseEvent<HTMLButtonElement>)
  }

  if (href) {
    return (
      <a
        href={disabled ? undefined : href}
        target="_blank"
        rel="noopener noreferrer"
        className={classes}
        title={label}
        aria-label={label}
        aria-disabled={disabled || undefined}
        data-test={dataTest}
        onClick={handleClick}
      >
        {icon}
      </a>
    )
  }

  if (to) {
    return (
      <Link
        to={to}
        className={classes}
        title={label}
        aria-label={label}
        aria-disabled={disabled || undefined}
        tabIndex={disabled ? -1 : undefined}
        data-test={dataTest}
        onClick={handleClick}
      >
        {icon}
      </Link>
    )
  }

  return (
    <button
      type={type}
      className={classes}
      title={label}
      aria-label={label}
      data-test={dataTest}
      onClick={onClick}
      disabled={disabled}
      {...props}
    >
      {icon}
    </button>
  )
}
