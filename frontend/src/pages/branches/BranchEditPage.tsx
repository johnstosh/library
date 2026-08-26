// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useBranch } from '@/api/branches'
import { BranchFormPage } from './components/BranchFormPage'
import { PageLoading } from '@/components/progress/PageLoading'
import { EntityNotFound } from '@/components/ui/EntityNotFound'

export function BranchEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const branchId = id ? parseInt(id, 10) : 0
  const { data: branch, isLoading } = useBranch(branchId)

  const handleSuccess = () => {
    navigate(`/branches/${branchId}`)
  }

  const handleCancel = () => {
    navigate(`/branches/${branchId}`)
  }

  if (isLoading) {
    return <PageLoading />
  }

  if (!branch) {
    return (
      <EntityNotFound
        title="Branch Not Found"
        entityLabel="branch"
        onBack={() => navigate('/branches')}
        backLabel="Return to Branches"
      />
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <BranchFormPage
        title="Edit Branch"
        branch={branch}
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
