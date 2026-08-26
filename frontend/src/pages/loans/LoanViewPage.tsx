// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { EntityLink } from '@/components/ui/EntityLink'
import { Button } from '@/components/ui/Button'
import { useLoan, useReturnBook, useDeleteLoan } from '@/api/loans'
import { formatDate, parseISODateSafe } from '@/utils/formatters'
import { PageLoading } from '@/components/progress/PageLoading'
import { ThrottledThumbnail } from '@/components/ui/ThrottledThumbnail'
import { getThumbnailUrl, getPhotoUrl } from '@/api/photos'
import { PiCheckCircle, PiTrash, PiCamera, PiPencil } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useIsLibrarian } from '@/stores/authStore'
import { BackLink } from '@/components/ui/BackLink'
import { EntityNotFound } from '@/components/ui/EntityNotFound'
import { PageCard } from '@/components/ui/PageCard'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'

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
    return <PageLoading />
  }

  if (!loan) {
    return (
      <EntityNotFound
        title="Loan Not Found"
        entityLabel="loan"
        onBack={handleBack}
        backLabel="Return to Loans"
      />
    )
  }

  const isOverdue = !loan.returnDate && parseISODateSafe(loan.dueDate) < new Date()
  const isReturned = !!loan.returnDate

  return (
    <div className="max-w-4xl mx-auto">
      <BackLink onClick={handleBack} data-test="back-to-loans">
        Back to Loans
      </BackLink>

      <PageCard padding={false}>
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900" data-test="loan-book-title">
                <EntityLink to={`/books/${loan.bookId}`}>{loan.bookTitle}</EntityLink>
              </h1>
              <p className="text-gray-600 mt-1">Borrowed by: {loan.userName}</p>
            </div>
            <div className="flex gap-3">
              {isLibrarian && (
                <Button
                  variant="outline"
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
                <p className="text-gray-900">
                  <EntityLink to={`/books/${loan.bookId}`}>{loan.bookTitle}</EntityLink>
                </p>
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
      </PageCard>

      <ConfirmDialog
        isOpen={showReturnConfirm}
        onClose={() => setShowReturnConfirm(false)}
        onConfirm={handleReturn}
        title="Return Book"
        message="Mark this book as returned?"
        confirmText="Yes, Return"
        variant="primary"
        isLoading={returnBook.isPending}
        confirmDataTest="confirm-return-loan"
        cancelDataTest="cancel-return-loan"
      />

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Loan"
        message="Are you sure you want to delete this loan? This action cannot be undone."
        confirmText="Yes, Delete"
        variant="danger"
        isLoading={deleteLoan.isPending}
        confirmDataTest="confirm-delete-loan"
        cancelDataTest="cancel-delete-loan"
      />
    </div>
  )
}
