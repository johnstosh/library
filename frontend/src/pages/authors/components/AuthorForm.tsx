// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useCreateAuthor, useUpdateAuthor } from '@/api/authors'
import type { AuthorDto } from '@/types/dtos'

interface AuthorFormProps {
  isOpen: boolean
  onClose: () => void
  author?: AuthorDto | null
}

export function AuthorForm({ isOpen, onClose, author }: AuthorFormProps) {
  const isEditing = !!author

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

  const createAuthor = useCreateAuthor()
  const updateAuthor = useUpdateAuthor()

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
    } else {
      setFormData({
        name: '',
        birthDate: '',
        deathDate: '',
        religiousAffiliation: '',
        birthCountry: '',
        nationality: '',
        briefBiography: '',
        grokipediaUrl: '',
      })
    }
    setError('')
  }, [author, isOpen])

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
        await updateAuthor.mutateAsync({ id: author!.id, author: authorData })
      } else {
        await createAuthor.mutateAsync(authorData)
      }

      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const isLoading = createAuthor.isPending || updateAuthor.isPending

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Edit Author' : 'Add New Author'}
      size="lg"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={isLoading} data-test="author-form-cancel">
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
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorMessage message={error} />}

        <Input
          label="Name"
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
          data-test="author-name"
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Birth Date"
            type="date"
            value={formData.birthDate}
            onChange={(e) => setFormData({ ...formData, birthDate: e.target.value })}
            data-test="author-birth-date"
          />

          <Input
            label="Death Date"
            type="date"
            value={formData.deathDate}
            onChange={(e) => setFormData({ ...formData, deathDate: e.target.value })}
            data-test="author-death-date"
          />
        </div>

        <Textarea
          label="Religious Affiliation"
          value={formData.religiousAffiliation}
          onChange={(e) => setFormData({ ...formData, religiousAffiliation: e.target.value })}
          rows={3}
          data-test="author-religious-affiliation"
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Textarea
            label="Birth Country"
            value={formData.birthCountry}
            onChange={(e) => setFormData({ ...formData, birthCountry: e.target.value })}
            rows={3}
            data-test="author-birth-country"
          />

          <Textarea
            label="Nationality"
            value={formData.nationality}
            onChange={(e) => setFormData({ ...formData, nationality: e.target.value })}
            rows={3}
            data-test="author-nationality"
          />
        </div>

        <Textarea
          label="Brief Biography"
          value={formData.briefBiography}
          onChange={(e) => setFormData({ ...formData, briefBiography: e.target.value })}
          rows={4}
          data-test="author-biography"
        />

        <Input
          label="Grokipedia URL"
          value={formData.grokipediaUrl}
          onChange={(e) => setFormData({ ...formData, grokipediaUrl: e.target.value })}
          placeholder="https://grokipedia.example.com/author/123"
          data-test="author-grokipedia-url"
        />
      </form>
    </Modal>
  )
}
