// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useUser } from '@/api/users'
import { UserFormPage } from './components/UserFormPage'
import { PageLoading } from '@/components/progress/PageLoading'
import { EntityNotFound } from '@/components/ui/EntityNotFound'

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
    return <PageLoading />
  }

  if (!user) {
    return (
      <EntityNotFound
        title="User Not Found"
        entityLabel="user"
        onBack={() => navigate('/users')}
        backLabel="Return to Users"
      />
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
