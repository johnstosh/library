// (c) Copyright 2025 by Muczynski
import {
  Combobox as HeadlessCombobox,
  ComboboxInput,
  ComboboxButton,
  ComboboxOptions,
  ComboboxOption as HeadlessComboboxOption,
} from '@headlessui/react'
import { clsx } from 'clsx'
import { PiCaretDown, PiCheck, PiSpinnerGap } from 'react-icons/pi'

export interface ComboboxOption {
  value: string
  label: string
  isCreateOption?: boolean
}

export interface ComboboxProps {
  label?: string
  options: ComboboxOption[]
  value: string
  onChange: (value: string | null) => void
  query: string
  onQueryChange: (query: string) => void
  placeholder?: string
  required?: boolean
  isLoading?: boolean
  error?: string
  helpText?: string
  disabled?: boolean
  'data-test'?: string
}

export function Combobox({
  label,
  options,
  value,
  onChange,
  query,
  onQueryChange,
  placeholder = 'Search...',
  required,
  isLoading,
  error,
  helpText,
  disabled,
  'data-test': dataTest,
}: ComboboxProps) {
  const selectedOption = options.find((opt) => opt.value === value)

  return (
    <div className="w-full" data-test={dataTest}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
          {isLoading && (
            <span className="ml-2 inline-block">
              <PiSpinnerGap className="animate-spin h-4 w-4 text-gray-500 inline" />
            </span>
          )}
        </label>
      )}
      <HeadlessCombobox
        value={value}
        onChange={onChange}
        disabled={disabled || isLoading}
      >
        <div className="relative">
          <ComboboxInput
            className={clsx(
              'block w-full px-3 py-2 border rounded-md shadow-sm min-h-[44px] pr-10',
              'focus:ring-blue-500 focus:border-blue-500 focus:outline-none focus:ring-1',
              'disabled:bg-gray-100 disabled:cursor-not-allowed',
              error ? 'border-red-300' : 'border-gray-300'
            )}
            displayValue={() => selectedOption?.label ?? ''}
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder={isLoading ? 'Loading...' : placeholder}
            data-test={dataTest ? `${dataTest}-input` : undefined}
          />
          <ComboboxButton className="absolute inset-y-0 right-0 flex items-center pr-3">
            <PiCaretDown className="h-5 w-5 text-gray-400" aria-hidden="true" />
          </ComboboxButton>
        </div>

        <ComboboxOptions
          className={clsx(
            'absolute z-50 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1',
            'shadow-lg ring-1 ring-black/5 focus:outline-none text-sm'
          )}
        >
          {options.length === 0 && query !== '' && !isLoading && (
            <div className="px-4 py-2 text-gray-500">No results found</div>
          )}
          {options.map((option) => (
            <HeadlessComboboxOption
              key={option.value}
              value={option.value}
              className={({ focus, selected }) =>
                clsx(
                  'relative cursor-pointer select-none py-2 pl-10 pr-4',
                  focus ? 'bg-blue-600 text-white' : 'text-gray-900',
                  selected && !focus && 'bg-blue-50',
                  option.isCreateOption && 'font-medium'
                )
              }
              data-test={
                dataTest
                  ? option.isCreateOption
                    ? `${dataTest}-create`
                    : `${dataTest}-option`
                  : undefined
              }
            >
              {({ selected, focus }) => (
                <>
                  <span className={clsx('block truncate', selected && 'font-semibold')}>
                    {option.isCreateOption ? `Create "${option.label}"` : option.label}
                  </span>
                  {selected && (
                    <span
                      className={clsx(
                        'absolute inset-y-0 left-0 flex items-center pl-3',
                        focus ? 'text-white' : 'text-blue-600'
                      )}
                    >
                      <PiCheck className="h-5 w-5" aria-hidden="true" />
                    </span>
                  )}
                </>
              )}
            </HeadlessComboboxOption>
          ))}
        </ComboboxOptions>
      </HeadlessCombobox>
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
      {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
    </div>
  )
}
