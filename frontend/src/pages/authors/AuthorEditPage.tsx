// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useAuthor } from '@/api/authors'
import { AuthorFormPage } from './components/AuthorFormPage'
import { PageLoading } from '@/components/progress/PageLoading'
import { EntityNotFound } from '@/components/ui/EntityNotFound'

export function AuthorEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const authorId = id ? parseInt(id, 10) : 0
  const { data: author, isLoading } = useAuthor(authorId)

  const handleSuccess = () => {
    navigate(`/authors/${authorId}`)
  }

  const handleCancel = () => {
    navigate(`/authors/${authorId}`)
  }

  if (isLoading) {
    return <PageLoading />
  }

  if (!author) {
    return (
      <EntityNotFound
        title="Author Not Found"
        entityLabel="author"
        onBack={() => navigate('/authors')}
        backLabel="Return to Authors"
      />
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <AuthorFormPage
        title="Edit Author"
        author={author}
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
