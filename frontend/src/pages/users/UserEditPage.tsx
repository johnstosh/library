// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useUser } from '@/api/users'
import { UserFormPage } from './components/UserFormPage'
import { Spinner } from '@/components/progress/Spinner'

export function UserEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const userId = id ? parseInt(id, 10) : 0
  const { data: user, isLoading } = useUser(userId)

  const handleSuccess = () => {
    navigate(`/users/${userId}`)
  }

  const handleCancel = () => {
    navigate(`/users/${userId}`)
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">User Not Found</h1>
          <p className="text-gray-600 mb-4">The user you're looking for doesn't exist.</p>
          <button
            onClick={() => navigate('/users')}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Users
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <UserFormPage
        title="Edit User"
        user={user}
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
