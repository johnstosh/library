// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useBook } from '@/api/books'
import { BookFormPage } from './components/BookFormPage'
import { PageLoading } from '@/components/progress/PageLoading'
import { EntityNotFound } from '@/components/ui/EntityNotFound'

export function BookEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const bookId = id ? parseInt(id, 10) : 0
  const { data: book, isLoading } = useBook(bookId)

  const handleSuccess = () => {
    navigate(`/books/${bookId}`)
  }

  const handleCancel = () => {
    navigate(`/books/${bookId}`)
  }

  if (isLoading) {
    return <PageLoading />
  }

  if (!book) {
    return (
      <EntityNotFound
        title="Book Not Found"
        entityLabel="book"
        onBack={() => navigate('/books')}
        backLabel="Return to Books"
      />
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <BookFormPage
        title="Edit Book"
        book={book}
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
