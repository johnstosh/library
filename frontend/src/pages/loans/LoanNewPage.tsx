// (c) Copyright 2025 by Muczynski
import { useNavigate, useSearchParams } from 'react-router-dom'
import { LoanFormPage } from './components/LoanFormPage'

export function LoanNewPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const handleSuccess = () => {
    navigate('/loans')
  }

  const handleCancel = () => {
    navigate('/loans')
  }

  // Read initial filter values from query params (from checkout card transcription)
  const initialFilters = {
    title: searchParams.get('title') || '',
    author: searchParams.get('author') || '',
    locNumber: searchParams.get('locNumber') || '',
    borrower: searchParams.get('borrower') || '',
    checkoutDate: searchParams.get('checkoutDate') || '',
    dueDate: searchParams.get('dueDate') || '',
    hasPhoto: searchParams.get('hasPhoto') === 'true',
  }

  // Check for capture mode (checkout by photo/camera)
  const captureMode = searchParams.get('captureMode') as 'file' | 'camera' | null

  return (
    <div className="max-w-6xl mx-auto">
      <LoanFormPage
        title="Checkout Book"
        onSuccess={handleSuccess}
        onCancel={handleCancel}
        initialFilters={initialFilters}
        captureMode={captureMode}
      />
    </div>
  )
}
