// (c) Copyright 2025 by Muczynski
import { forwardRef } from 'react'
import type { InputHTMLAttributes } from 'react'
import { clsx } from 'clsx'

export interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, error, className, ...props }, ref) => {
    return (
      <div className="flex items-center">
        <input
          ref={ref}
          type="checkbox"
          className={clsx(
            'h-4 w-4 rounded border-gray-300 text-blue-600',
            'focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50',
            error && 'border-red-300',
            className
          )}
          {...props}
        />
        {label && (
          <label className="ml-2 block text-sm text-gray-700">
            {label}
            {error && <span className="text-red-500 ml-1">{error}</span>}
          </label>
        )}
      </div>
    )
  }
)

Checkbox.displayName = 'Checkbox'
