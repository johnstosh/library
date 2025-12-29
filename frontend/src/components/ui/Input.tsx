// (c) Copyright 2025 by Muczynski
import { forwardRef } from 'react'
import type { InputHTMLAttributes } from 'react'
import { clsx } from 'clsx'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: string
  hideLabel?: boolean
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helpText, hideLabel, className, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && !hideLabel && (
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {label}
            {props.required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <input
          ref={ref}
          className={clsx(
            'block w-full px-3 py-2 border rounded-md shadow-sm',
            'focus:ring-blue-500 focus:border-blue-500',
            'disabled:bg-gray-100 disabled:cursor-not-allowed',
            error ? 'border-red-300' : 'border-gray-300',
            className
          )}
          {...props}
        />
        {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
        {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
      </div>
    )
  }
)

Input.displayName = 'Input'
