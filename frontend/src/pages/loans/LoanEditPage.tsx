// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { useLoan } from '@/api/loans'
import { LoanFormPage } from './components/LoanFormPage'
import { Spinner } from '@/components/progress/Spinner'

export function LoanEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const loanId = id ? parseInt(id, 10) : 0
  const { data: loan, isLoading } = useLoan(loanId)

  const handleSuccess = () => {
    navigate(`/loans/${loanId}`)
  }

  const handleCancel = () => {
    navigate(`/loans/${loanId}`)
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!loan) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Loan Not Found</h1>
          <p className="text-gray-600 mb-4">The loan you're looking for doesn't exist.</p>
          <button
            onClick={() => navigate('/loans')}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Loans
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <LoanFormPage
        title="View Loan"
        loan={loan}
        onSuccess={handleSuccess}
        onCancel={handleCancel}
      />
    </div>
  )
}
