// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useLoan, useReturnBook, useDeleteLoan } from '@/api/loans'
import { formatDate } from '@/utils/formatters'
import { Spinner } from '@/components/progress/Spinner'
import { PiArrowLeft, PiCheckCircle, PiTrash } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'

export function LoanViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const loanId = id ? parseInt(id, 10) : 0
  const { data: loan, isLoading } = useLoan(loanId)
  const returnBook = useReturnBook()
  const deleteLoan = useDeleteLoan()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleReturn = async () => {
    try {
      await returnBook.mutateAsync(loanId)
      // Refresh the loan data after returning
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to return book')
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

  const isOverdue = !loan.returnDate && new Date(loan.dueDate) < new Date()
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
              {!isReturned && (
                <Button
                  variant="primary"
                  onClick={handleReturn}
                  isLoading={returnBook.isPending}
                  leftIcon={<PiCheckCircle />}
                  data-test="loan-view-return"
                >
                  Return Book
                </Button>
              )}
              <Button
                variant="danger"
                onClick={() => setShowDeleteConfirm(true)}
                leftIcon={<PiTrash />}
                data-test="loan-view-delete"
              >
                Delete
              </Button>
            </div>
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

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
            <div className="grid grid-cols-2 gap-4">
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
        </div>
      </div>
    </div>
  )
}
