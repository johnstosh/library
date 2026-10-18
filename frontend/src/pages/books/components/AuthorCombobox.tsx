// (c) Copyright 2025 by Muczynski
import { useMemo, useState } from 'react'
import { Combobox, type ComboboxOption } from '@/components/ui/Combobox'
import { useAuthor, useCreateAuthor } from '@/api/authors'
import { AUTHOR_SEARCH_MIN_CHARS, useAuthorNameSearch } from '@/api/authorMatches'

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
  const [picked, setPicked] = useState<{ id: string; name: string } | null>(null)
  const search = useAuthorNameSearch(query)
  const selectedId = Number(value)
  const { data: selectedAuthor, isLoading: selectedLoading } = useAuthor(
    Number.isInteger(selectedId) && selectedId > 0 ? selectedId : 0,
  )
  const createAuthor = useCreateAuthor()

  const selectedName = picked?.id === value
    ? picked.name
    : selectedAuthor?.name

  const trimmed = query.trim()
  const searching = trimmed.length >= AUTHOR_SEARCH_MIN_CHARS
  const normalizedQuery = trimmed.toLowerCase()

  const options = useMemo((): ComboboxOption[] => {
    const matches: ComboboxOption[] = search.data.map((author) => ({
      value: author.id.toString(),
      label: author.name,
    }))

    if (!searching && value && selectedName) {
      matches.unshift({ value, label: selectedName })
    }

    const exactMatch = search.data.some(
      (author) => author.name.toLowerCase() === normalizedQuery,
    )
    if (searching && !search.error && !exactMatch) {
      matches.push({
        value: `__create__${trimmed}`,
        label: trimmed,
        isCreateOption: true,
      })
    }

    return matches
  }, [search.data, search.error, searching, normalizedQuery, trimmed, value, selectedName])

  const handleChange = async (selectedValue: string | null) => {
    if (!selectedValue) return

    if (selectedValue.startsWith('__create__')) {
      const newAuthorName = selectedValue.replace('__create__', '')
      try {
        const newAuthor = await createAuthor.mutateAsync({ name: newAuthorName })
        setPicked({ id: newAuthor.id.toString(), name: newAuthor.name })
        onChange(newAuthor.id.toString())
        setQuery('')
      } catch (error) {
        console.error('Failed to create author:', error)
      }
      return
    }

    const match = search.data.find((author) => author.id.toString() === selectedValue)
    if (match) {
      setPicked({ id: match.id.toString(), name: match.name })
    }
    onChange(selectedValue)
    setQuery('')
  }

  const helpText = search.error
    ? undefined
    : searching && !search.isFetching
      ? `Showing ${search.data.length} authors`
      : 'Type at least 3 characters'

  return (
    <Combobox
      label="Author"
      options={options}
      value={value}
      onChange={handleChange}
      query={query}
      onQueryChange={setQuery}
      placeholder="Search or create author..."
      required={required}
      isLoading={search.isFetching || selectedLoading || createAuthor.isPending}
      displayLabel={selectedName}
      error={search.error || undefined}
      helpText={helpText}
      disabled={createAuthor.isPending}
      data-test={dataTest}
    />
  )
}
