// (c) Copyright 2025 by Muczynski
import { forwardRef } from 'react'
import type { ButtonHTMLAttributes, MouseEvent, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { clsx } from 'clsx'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
  fullWidth?: boolean
  leftIcon?: ReactNode
  rightIcon?: ReactNode
  /** Internal path: render as a React Router <Link> so it can be opened in a new tab. */
  to?: string
  'data-test'?: string
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      fullWidth = false,
      leftIcon,
      rightIcon,
      className,
      disabled,
      to,
      type,
      onClick,
      'data-test': dataTest,
      ...props
    },
    ref
  ) => {
    const isDisabled = Boolean(disabled || isLoading)
    const baseStyles =
      'inline-flex items-center justify-center font-medium rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'

    const variantStyles = {
      primary: 'bg-primary-600 text-white hover:bg-primary-700 focus:ring-primary-500',
      secondary: 'bg-charcoal-700 text-white hover:bg-charcoal-800 focus:ring-charcoal-500',
      danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
      ghost: 'bg-transparent text-gray-700 hover:bg-gray-100 focus:ring-gray-500',
      outline: 'border-2 border-gray-300 text-gray-700 hover:bg-gray-50 focus:ring-gray-500',
    }

    const sizeStyles = {
      sm: 'px-3 py-2 sm:py-1.5 text-sm min-h-[44px] sm:min-h-0',
      md: 'px-4 py-2.5 sm:py-2 text-base min-h-[44px] sm:min-h-0',
      lg: 'px-6 py-3.5 sm:py-3 text-lg min-h-[44px] sm:min-h-0',
    }

    const classes = clsx(
      baseStyles,
      variantStyles[variant],
      sizeStyles[size],
      fullWidth && 'w-full',
      to && isDisabled && 'opacity-50 cursor-not-allowed pointer-events-none',
      className
    )

    const content = (
      <>
        {isLoading && (
          <svg
            className="mr-2 h-4 w-4 shrink-0 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {!isLoading && leftIcon && <span className="mr-2 shrink-0">{leftIcon}</span>}
        <span className="min-w-0">{children}</span>
        {!isLoading && rightIcon && <span className="ml-2 shrink-0">{rightIcon}</span>}
      </>
    )

    if (to) {
      return (
        <Link
          to={to}
          className={classes}
          aria-disabled={isDisabled || undefined}
          tabIndex={isDisabled ? -1 : undefined}
          onClick={(event) => {
            if (isDisabled) {
              event.preventDefault()
              event.stopPropagation()
              return
            }
            onClick?.(event as unknown as MouseEvent<HTMLButtonElement>)
          }}
          data-test={dataTest}
        >
          {content}
        </Link>
      )
    }

    return (
      <button
        ref={ref}
        type={type}
        className={classes}
        disabled={isDisabled}
        onClick={onClick}
        data-test={dataTest}
        {...props}
      >
        {content}
      </button>
    )
  }
)

Button.displayName = 'Button'
