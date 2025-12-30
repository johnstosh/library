// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { LibraryFormPage } from './components/LibraryFormPage'

export function LibraryNewPage() {
  const navigate = useNavigate()

  const handleSuccess = () => {
    navigate('/libraries')
  }

  const handleCancel = () => {
    navigate('/libraries')
  }

  return (
    <div className="max-w-4xl mx-auto">
      <LibraryFormPage
        title="Add New Library"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
