// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { AuthorFormPage } from './components/AuthorFormPage'

export function AuthorNewPage() {
  const navigate = useNavigate()

  const handleSuccess = () => {
    navigate('/authors')
  }

  const handleCancel = () => {
    navigate('/authors')
  }

  return (
    <div className="max-w-4xl mx-auto">
      <AuthorFormPage
        title="Add New Author"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
