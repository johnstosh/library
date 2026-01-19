// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Checkbox } from '@/components/ui/Checkbox'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { CheckoutCardTranscriptionModal } from './components/CheckoutCardTranscriptionModal'
import { useLoans, useReturnBook, useDeleteLoan } from '@/api/loans'
import { useLoansShowAll, useUiStore } from '@/stores/uiStore'
import { formatDate } from '@/utils/formatters'
import type { LoanDto, CheckoutCardTranscriptionDto } from '@/types/dtos'
import { PiEye } from 'react-icons/pi'
import { useIsLibrarian } from '@/stores/authStore'

export function LoansPage() {
  const navigate = useNavigate()
  const [returnLoanId, setReturnLoanId] = useState<number | null>(null)
  const [deleteLoanId, setDeleteLoanId] = useState<number | null>(null)
  const [isTranscriptionModalOpen, setIsTranscriptionModalOpen] = useState(false)
  const [captureMode, setCaptureMode] = useState<'file' | 'camera'>('file')

  const showAll = useLoansShowAll()
  const setLoansShowAll = useUiStore((state) => state.setLoansShowAll)
  const isLibrarian = useIsLibrarian()

  const { data: loans = [], isLoading, error, isFetching } = useLoans(showAll)

  // Debug logging for loans
  console.log('[LoansPage] Render state:', {
    showAll,
    isLoading,
    isFetching,
    loansCount: loans.length,
    error: error?.message
  })
  if (loans.length > 0) {
    console.log('[LoansPage] First loan:', loans[0])
  }
  const returnBook = useReturnBook()
  const deleteLoan = useDeleteLoan()

  const handleCheckout = () => {
    navigate('/loans/new')
  }

  const handleCheckoutByPhoto = () => {
    setCaptureMode('file')
    setIsTranscriptionModalOpen(true)
  }

  const handleCheckoutByCamera = () => {
    setCaptureMode('camera')
    setIsTranscriptionModalOpen(true)
  }

  const handleTranscriptionComplete = (data: CheckoutCardTranscriptionDto) => {
    console.log('Transcription completed:', data)
    // TODO: Could auto-populate checkout form with this data
  }

  const handleViewLoan = (loan: LoanDto) => {
    navigate(`/loans/${loan.id}`)
  }

  const handleReturn = async () => {
    if (returnLoanId === null) return

    try {
      await returnBook.mutateAsync(returnLoanId)
      setReturnLoanId(null)
    } catch (error) {
      console.error('Failed to return book:', error)
    }
  }

  const handleDelete = async () => {
    if (deleteLoanId === null) return

    try {
      await deleteLoan.mutateAsync(deleteLoanId)
      setDeleteLoanId(null)
    } catch (error) {
      console.error('Failed to delete loan:', error)
    }
  }

  const columns: Column<LoanDto>[] = [
    {
      key: 'bookTitle',
      header: 'Book',
      accessor: (loan) => (
        <div>
          <button
            onClick={() => handleViewLoan(loan)}
            className="font-medium text-blue-600 hover:text-blue-800 text-left"
            data-test={`view-loan-${loan.id}`}
          >
            {loan.bookTitle}
          </button>
          {isLibrarian && loan.userName && (
            <div className="text-sm text-gray-500">Borrowed by: {loan.userName}</div>
          )}
        </div>
      ),
      width: '30%',
    },
    {
      key: 'loanDate',
      header: 'Checkout Date',
      accessor: (loan) => formatDate(loan.loanDate),
      width: '15%',
    },
    {
      key: 'dueDate',
      header: 'Due Date',
      accessor: (loan) => (
        <span
          className={
            !loan.returnDate && new Date(loan.dueDate) < new Date()
              ? 'text-red-600 font-medium'
              : ''
          }
        >
          {formatDate(loan.dueDate)}
        </span>
      ),
      width: '15%',
    },
    {
      key: 'returnDate',
      header: 'Return Date',
      accessor: (loan) =>
        loan.returnDate ? (
          <span className="text-green-600 font-medium">{formatDate(loan.returnDate)}</span>
        ) : (
          <span className="text-gray-400">Not returned</span>
        ),
      width: '15%',
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (loan) => {
        if (loan.returnDate) {
          return (
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
              Returned
            </span>
          )
        }
        const isOverdue = new Date(loan.dueDate) < new Date()
        return (
          <span
            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              isOverdue ? 'bg-red-100 text-red-800' : 'bg-blue-100 text-blue-800'
            }`}
          >
            {isOverdue ? 'Overdue' : 'Active'}
          </span>
        )
      },
      width: '10%',
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Loans</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={handleCheckoutByPhoto} data-test="checkout-by-photo">
            Checkout by Photo
          </Button>
          <Button variant="secondary" onClick={handleCheckoutByCamera} data-test="checkout-by-camera">
            Checkout by Camera
          </Button>
          <Button variant="primary" onClick={handleCheckout} data-test="checkout-book">
            Checkout Book
          </Button>
        </div>
      </div>

      {/* Error display for debugging */}
      {error && (
        <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
          <p className="font-bold">Error loading loans:</p>
          <p>{error.message}</p>
        </div>
      )}

      <div className="bg-white rounded-lg shadow">
        <div className="p-4 border-b border-gray-200">
          <Checkbox
            label="Show returned loans"
            checked={showAll}
            onChange={(e) => setLoansShowAll(e.target.checked)}
            data-test="show-all-loans"
          />
        </div>

        <div className="p-4">
          <DataTable
            data={loans}
            columns={columns}
            keyExtractor={(loan) => loan.id}
            actions={(loan) => (
              <>
                <button
                  onClick={() => handleViewLoan(loan)}
                  className="text-gray-600 hover:text-gray-900"
                  data-test={`view-loan-details-${loan.id}`}
                  title="View Details"
                >
                  <PiEye className="w-5 h-5" />
                </button>
                {isLibrarian && !loan.returnDate && (
                  <button
                    onClick={() => setReturnLoanId(loan.id)}
                    className="text-green-600 hover:text-green-900"
                    data-test={`return-loan-${loan.id}`}
                    title="Return Book"
                  >
                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                  </button>
                )}
                {isLibrarian && (
                  <button
                    onClick={() => setDeleteLoanId(loan.id)}
                    className="text-red-600 hover:text-red-900"
                    data-test={`delete-loan-${loan.id}`}
                    title="Delete"
                  >
                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                      />
                    </svg>
                  </button>
                )}
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No loans found"
          />
        </div>

        {!isLoading && loans.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
            <p className="text-sm text-gray-700">
              Showing {loans.length} {loans.length === 1 ? 'loan' : 'loans'}
            </p>
          </div>
        )}
      </div>

      <ConfirmDialog
        isOpen={returnLoanId !== null}
        onClose={() => setReturnLoanId(null)}
        onConfirm={handleReturn}
        title="Return Book"
        message="Mark this book as returned?"
        confirmText="Return"
        variant="primary"
        isLoading={returnBook.isPending}
      />

      <ConfirmDialog
        isOpen={deleteLoanId !== null}
        onClose={() => setDeleteLoanId(null)}
        onConfirm={handleDelete}
        title="Delete Loan"
        message="Are you sure you want to delete this loan record? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteLoan.isPending}
      />

      <CheckoutCardTranscriptionModal
        isOpen={isTranscriptionModalOpen}
        onClose={() => setIsTranscriptionModalOpen(false)}
        onTranscriptionComplete={handleTranscriptionComplete}
        captureMode={captureMode}
      />
    </div>
  )
}
