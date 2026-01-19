// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { BranchFormPage } from './components/BranchFormPage'

export function BranchNewPage() {
  const navigate = useNavigate()

  const handleSuccess = () => {
    navigate('/branches')
  }

  const handleCancel = () => {
    navigate('/branches')
  }

  return (
    <div className="max-w-4xl mx-auto">
      <BranchFormPage
        title="Add New Branch"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
