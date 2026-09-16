// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { AuthorBooksTable } from './AuthorBooksTable'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { useAuthorBooks, useCreateAuthor, useUpdateAuthor } from '@/api/authors'
import { useLookupSingleAuthorGrokipedia, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { useIsLibrarian } from '@/stores/authStore'
import { GrokipediaIcon } from '@/components/ui/Icons'
import type { AuthorDto } from '@/types/dtos'

interface AuthorFormPageProps {
  title: string
  author?: AuthorDto
  onSuccess: () => void
  onCancel: () => void
}

export function AuthorFormPage({ title, author, onSuccess, onCancel }: AuthorFormPageProps) {
  const isEditing = !!author
  const { data: authorBooks = [], isLoading: booksLoading } = useAuthorBooks(author?.id ?? 0)

  const [formData, setFormData] = useState({
    name: '',
    birthDate: '',
    deathDate: '',
    religiousAffiliation: '',
    birthCountry: '',
    nationality: '',
    briefBiography: '',
    grokipediaUrl: '',
  })
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])

  const createAuthor = useCreateAuthor()
  const updateAuthor = useUpdateAuthor()
  const lookupGrokipediaQuick = useLookupSingleAuthorGrokipedia()
  const lookupGrokipediaSlow = useLookupSingleAuthorGrokipedia()
  const isLibrarian = useIsLibrarian()

  useEffect(() => {
    if (author) {
      setFormData({
        name: author.name,
        birthDate: author.dateOfBirth || '',
        deathDate: author.dateOfDeath || '',
        religiousAffiliation: author.religiousAffiliation || '',
        birthCountry: author.birthCountry || '',
        nationality: author.nationality || '',
        briefBiography: author.briefBiography || '',
        grokipediaUrl: author.grokipediaUrl || '',
      })
    }
  }, [author])

  // Warn user about unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault()
        e.returnValue = ''
      }
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const handleFieldChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value })
    setHasUnsavedChanges(true)
  }

  const handleGrokipediaLookup = async (slow: boolean) => {
    if (!author?.id) return

    setError('')

    const lookup = slow ? lookupGrokipediaSlow : lookupGrokipediaQuick
    try {
      const result = await lookup.mutateAsync({ authorId: author.id, slow })
      setGrokipediaResults([result])
      setShowGrokipediaResults(true)
      if (result.grokipediaUrl != null) {
        setFormData({ ...formData, grokipediaUrl: result.grokipediaUrl })
        setHasUnsavedChanges(true)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to lookup Grokipedia URL')
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.name || !formData.name.trim()) {
      setError('Name is required')
      return
    }

    try {
      const authorData = {
        name: formData.name,
        dateOfBirth: formData.birthDate || undefined,
        dateOfDeath: formData.deathDate || undefined,
        religiousAffiliation: formData.religiousAffiliation || undefined,
        birthCountry: formData.birthCountry || undefined,
        nationality: formData.nationality || undefined,
        briefBiography: formData.briefBiography || undefined,
        grokipediaUrl: formData.grokipediaUrl || undefined,
      }

      if (isEditing) {
        await updateAuthor.mutateAsync({ id: author.id, author: authorData })
      } else {
        await createAuthor.mutateAsync(authorData)
      }

      setHasUnsavedChanges(false)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      if (window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
        onCancel()
      }
    } else {
      onCancel()
    }
  }

  const isLoading = createAuthor.isPending || updateAuthor.isPending
  const isLookupPending = lookupGrokipediaQuick.isPending || lookupGrokipediaSlow.isPending

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
        {error && <ErrorMessage message={error} />}

        <Input
          label="Name"
          value={formData.name}
          onChange={(e) => handleFieldChange('name', e.target.value)}
          required
          data-test="author-name"
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Birth Date"
            type="date"
            value={formData.birthDate}
            onChange={(e) => handleFieldChange('birthDate', e.target.value)}
            data-test="author-birth-date"
          />

          <Input
            label="Death Date"
            type="date"
            value={formData.deathDate}
            onChange={(e) => handleFieldChange('deathDate', e.target.value)}
            data-test="author-death-date"
          />
        </div>

        <Textarea
          label="Religious Affiliation"
          value={formData.religiousAffiliation}
          onChange={(e) => handleFieldChange('religiousAffiliation', e.target.value)}
          rows={3}
          data-test="author-religious-affiliation"
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Textarea
            label="Birth Country"
            value={formData.birthCountry}
            onChange={(e) => handleFieldChange('birthCountry', e.target.value)}
            rows={3}
            data-test="author-birth-country"
          />

          <Textarea
            label="Nationality"
            value={formData.nationality}
            onChange={(e) => handleFieldChange('nationality', e.target.value)}
            rows={3}
            data-test="author-nationality"
          />
        </div>

        <Textarea
          label="Brief Biography"
          value={formData.briefBiography}
          onChange={(e) => handleFieldChange('briefBiography', e.target.value)}
          rows={4}
          data-test="author-biography"
        />

        <div className="flex flex-wrap items-end gap-2">
          <div className="flex-1 min-w-[12rem]">
            <Input
              label="Grokipedia URL"
              type="url"
              value={formData.grokipediaUrl}
              onChange={(e) => handleFieldChange('grokipediaUrl', e.target.value)}
              placeholder="https://grokipedia.com/page/..."
              data-test="author-grokipedia-url"
            />
          </div>
          {isEditing && isLibrarian && (
            <>
              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={() => handleGrokipediaLookup(false)}
                isLoading={lookupGrokipediaQuick.isPending}
                disabled={isLookupPending || isLoading}
                leftIcon={<GrokipediaIcon />}
                data-test="author-field-lookup-grokipedia-quick"
                className="mb-0"
              >
                Quick lookup
              </Button>
              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={() => handleGrokipediaLookup(true)}
                isLoading={lookupGrokipediaSlow.isPending}
                disabled={isLookupPending || isLoading}
                leftIcon={<GrokipediaIcon />}
                data-test="author-field-lookup-grokipedia-slow"
                className="mb-0"
              >
                Slow lookup
              </Button>
            </>
          )}
        </div>

        {/* Books Section - Only show when editing */}
        {isEditing && author && (
          <div className="pt-6 border-t border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900 mb-4" data-test="author-books-heading">
              Books by {author.name}
            </h2>
            <AuthorBooksTable books={authorBooks} isLoading={booksLoading} />
          </div>
        )}
      </form>

      {/* Footer */}
      <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={handleCancel}
            disabled={isLoading}
            data-test="author-form-cancel"
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isLoading}
            data-test="author-form-submit"
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </div>
      </div>

      <GrokipediaLookupResultsModal
        isOpen={showGrokipediaResults}
        onClose={() => setShowGrokipediaResults(false)}
        results={grokipediaResults}
        entityType="author"
      />
    </div>
  )
}
