// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useBranch } from '@/api/branches'
import { BranchFormPage } from './components/BranchFormPage'
import { Spinner } from '@/components/progress/Spinner'

export function BranchEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const libraryId = id ? parseInt(id, 10) : 0
  const { data: branch, isLoading } = useBranch(libraryId)

  const handleSuccess = () => {
    navigate(`/branches/${libraryId}`)
  }

  const handleCancel = () => {
    navigate(`/branches/${libraryId}`)
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!branch) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Branch Not Found</h1>
          <p className="text-gray-600 mb-4">The branch you're looking for doesn't exist.</p>
          <button
            onClick={() => navigate('/branches')}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Branches
          </button>
        </div>
      </div>
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
