// (c) Copyright 2025 by Muczynski
import { useState, useMemo } from 'react'
import { Combobox, type ComboboxOption } from '@/components/ui/Combobox'
import { useAuthors, useCreateAuthor } from '@/api/authors'

interface AuthorComboboxProps {
  value: string // authorId
  onChange: (authorId: string) => void
  required?: boolean
  'data-test'?: string
}

export function AuthorCombobox({
  value,
  onChange,
  required,
  'data-test': dataTest,
}: AuthorComboboxProps) {
  const [query, setQuery] = useState('')
  const { data: authors, isLoading: authorsLoading } = useAuthors()
  const createAuthor = useCreateAuthor()

  // Filter authors based on query
  const filteredOptions = useMemo((): ComboboxOption[] => {
    if (!authors) return []

    const normalizedQuery = query.toLowerCase().trim()

    // Filter authors by partial name match
    const filtered: ComboboxOption[] = authors
      .filter((author) => author.name.toLowerCase().includes(normalizedQuery))
      .map((author) => ({
        value: author.id.toString(),
        label: author.name,
      }))

    // Check if query exactly matches any author (case-insensitive)
    const exactMatch = authors.some(
      (author) => author.name.toLowerCase() === normalizedQuery
    )

    // Add "Create" option if query doesn't exactly match and has content
    if (normalizedQuery && !exactMatch) {
      filtered.push({
        value: `__create__${query.trim()}`,
        label: query.trim(),
        isCreateOption: true,
      })
    }

    return filtered
  }, [authors, query])

  const handleChange = async (selectedValue: string | null) => {
    if (!selectedValue) return

    // Check if this is a create option
    if (selectedValue.startsWith('__create__')) {
      const newAuthorName = selectedValue.replace('__create__', '')
      try {
        const newAuthor = await createAuthor.mutateAsync({ name: newAuthorName })
        onChange(newAuthor.id.toString())
        setQuery('')
      } catch (error) {
        console.error('Failed to create author:', error)
      }
    } else {
      onChange(selectedValue)
      setQuery('')
    }
  }

  return (
    <Combobox
      label="Author"
      options={filteredOptions}
      value={value}
      onChange={handleChange}
      query={query}
      onQueryChange={setQuery}
      placeholder="Search or create author..."
      required={required}
      isLoading={authorsLoading || createAuthor.isPending}
      data-test={dataTest}
    />
  )
}
