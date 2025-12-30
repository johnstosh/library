// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { LoanFormPage } from './components/LoanFormPage'

export function LoanNewPage() {
  const navigate = useNavigate()

  const handleSuccess = () => {
    navigate('/loans')
  }

  const handleCancel = () => {
    navigate('/loans')
  }

  return (
    <div className="max-w-4xl mx-auto">
      <LoanFormPage
        title="Checkout Book"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
