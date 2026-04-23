// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useLoan, useReturnBook, useDeleteLoan } from '@/api/loans'
import { formatDate, parseISODateSafe } from '@/utils/formatters'
import { Spinner } from '@/components/progress/Spinner'
import { ThrottledThumbnail } from '@/components/ui/ThrottledThumbnail'
import { getThumbnailUrl, getPhotoUrl } from '@/api/photos'
import { PiArrowLeft, PiCheckCircle, PiTrash, PiCamera, PiPencil } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useIsLibrarian } from '@/stores/authStore'

export function LoanViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const loanId = id ? parseInt(id, 10) : 0
  const { data: loan, isLoading } = useLoan(loanId)
  const returnBook = useReturnBook()
  const deleteLoan = useDeleteLoan()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showReturnConfirm, setShowReturnConfirm] = useState(false)
  const [error, setError] = useState('')
  const isLibrarian = useIsLibrarian()

  const handleReturn = async () => {
    try {
      await returnBook.mutateAsync(loanId)
      setShowReturnConfirm(false)
      navigate('/loans')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to return book')
      setShowReturnConfirm(false)
    }
  }

  const handleDelete = async () => {
    try {
      await deleteLoan.mutateAsync(loanId)
      navigate('/loans')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete loan')
      setShowDeleteConfirm(false)
    }
  }

  const handleBack = () => {
    navigate('/loans')
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
            onClick={handleBack}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Loans
          </button>
        </div>
      </div>
    )
  }

  const isOverdue = !loan.returnDate && parseISODateSafe(loan.dueDate) < new Date()
  const isReturned = !!loan.returnDate

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={handleBack}
          leftIcon={<PiArrowLeft />}
          data-test="back-to-loans"
        >
          Back to Loans
        </Button>
      </div>

      <h2 className="text-2xl font-bold text-gray-900 mb-4">Loan Details</h2>

      <div className="bg-white rounded-lg shadow">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900" data-test="loan-book-title">
                {loan.bookTitle}
              </h1>
              <p className="text-gray-600 mt-1">Borrowed by: {loan.userName}</p>
            </div>
            <div className="flex gap-3">
              {isLibrarian && (
                <Button
                  variant="secondary"
                  onClick={() => navigate(`/loans/${loanId}/edit`)}
                  leftIcon={<PiPencil />}
                  data-test="loan-view-edit"
                >
                  Edit
                </Button>
              )}
              {isLibrarian && !isReturned && (
                <Button
                  variant="primary"
                  onClick={() => setShowReturnConfirm(true)}
                  isLoading={returnBook.isPending}
                  leftIcon={<PiCheckCircle />}
                  data-test="loan-view-return"
                >
                  Return Book
                </Button>
              )}
              {isLibrarian && (
                <Button
                  variant="danger"
                  onClick={() => setShowDeleteConfirm(true)}
                  leftIcon={<PiTrash />}
                  data-test="loan-view-delete"
                >
                  Delete
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

          {/* Return Confirmation */}
          {showReturnConfirm && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <p className="text-blue-900 font-semibold mb-3">
                Mark this book as returned?
              </p>
              <div className="flex gap-3">
                <Button
                  variant="primary"
                  onClick={handleReturn}
                  isLoading={returnBook.isPending}
                  data-test="confirm-return-loan"
                >
                  Yes, Return
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowReturnConfirm(false)}
                  data-test="cancel-return-loan"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* Delete Confirmation */}
          {showDeleteConfirm && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <p className="text-red-900 font-semibold mb-3">
                Are you sure you want to delete this loan?
              </p>
              <p className="text-red-700 mb-4">This action cannot be undone.</p>
              <div className="flex gap-3">
                <Button
                  variant="danger"
                  onClick={handleDelete}
                  isLoading={deleteLoan.isPending}
                  data-test="confirm-delete-loan"
                >
                  Yes, Delete
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowDeleteConfirm(false)}
                  data-test="cancel-delete-loan"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* Status Badge */}
          {isReturned ? (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <p className="text-green-900 font-semibold">
                This book has been returned
              </p>
            </div>
          ) : isOverdue ? (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <p className="text-red-900 font-semibold">
                This loan is overdue
              </p>
            </div>
          ) : (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <p className="text-blue-900 font-semibold">
                This loan is active
              </p>
            </div>
          )}

          {/* Loan Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Book</p>
                <p className="text-gray-900">{loan.bookTitle}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Borrower</p>
                <p className="text-gray-900">{loan.userName}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Checkout Date</p>
                <p className="text-gray-900">{formatDate(loan.loanDate)}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Due Date</p>
                <p className={
                  isOverdue
                    ? 'text-red-600 font-medium'
                    : 'text-gray-900'
                }>
                  {formatDate(loan.dueDate)}
                </p>
              </div>
              {loan.returnDate && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Return Date</p>
                  <p className="text-green-600 font-medium">{formatDate(loan.returnDate)}</p>
                </div>
              )}
            </div>
          </div>

          {/* Checkout Card Photo */}
          {loan.photoId && (
            <div className="bg-gray-50 rounded-lg p-6">
              <div className="flex items-center gap-2 mb-4">
                <PiCamera className="w-5 h-5 text-gray-600" />
                <h3 className="text-lg font-semibold text-gray-900">Checkout Card Photo</h3>
              </div>
              <div className="flex justify-center">
                <a
                  href={getPhotoUrl(loan.photoId)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-block"
                >
                  <ThrottledThumbnail
                    photoId={loan.photoId}
                    url={getThumbnailUrl(loan.photoId, 400, loan.photoChecksum)}
                    alt="Checkout card photo"
                    className="w-full max-w-md min-h-[200px] rounded border border-gray-300 hover:opacity-90 transition-opacity cursor-pointer"
                    respectOrientation
                  />
                </a>
              </div>
              <p className="text-sm text-gray-500 text-center mt-2">
                Click to view full size
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
