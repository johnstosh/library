// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { BookFormPage } from './components/BookFormPage'

export function BookNewPage() {
  const navigate = useNavigate()

  const handleSuccess = () => {
    navigate('/books')
  }

  const handleCancel = () => {
    navigate('/books')
  }

  return (
    <div className="max-w-4xl mx-auto">
      <BookFormPage
        title="Add New Book"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
