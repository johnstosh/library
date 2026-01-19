// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { UserFormPage } from './components/UserFormPage'

export function UserNewPage() {
  const navigate = useNavigate()

  const handleSuccess = () => {
    navigate('/users')
  }

  const handleCancel = () => {
    navigate('/users')
  }

  return (
    <div className="max-w-4xl mx-auto">
      <UserFormPage
        title="Add New User"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
