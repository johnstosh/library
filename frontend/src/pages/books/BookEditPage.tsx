// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useBook } from '@/api/books'
import { BookFormPage } from './components/BookFormPage'
import { Spinner } from '@/components/progress/Spinner'

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
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!book) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Book Not Found</h1>
          <p className="text-gray-600 mb-4">The book you're looking for doesn't exist.</p>
          <button
            onClick={() => navigate('/books')}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Books
          </button>
        </div>
      </div>
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
