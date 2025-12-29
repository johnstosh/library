// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { Checkbox } from '@/components/ui/Checkbox'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useLoans, useCheckoutBook, useReturnBook, useDeleteLoan } from '@/api/loans'
import { useBooks } from '@/api/books'
import { useAuthStore } from '@/stores/authStore'
import { useLoansShowAll, useUiStore } from '@/stores/uiStore'
import { formatDate } from '@/utils/formatters'
import type { LoanDto } from '@/types/dtos'

export function LoansPage() {
  const [showCheckoutForm, setShowCheckoutForm] = useState(false)
  const [returnLoanId, setReturnLoanId] = useState<number | null>(null)
  const [deleteLoanId, setDeleteLoanId] = useState<number | null>(null)

  const [formData, setFormData] = useState({
    bookId: '',
  })
  const [error, setError] = useState('')

  const user = useAuthStore((state) => state.user)
  const showAll = useLoansShowAll()
  const setLoansShowAll = useUiStore((state) => state.setLoansShowAll)

  const { data: loans = [], isLoading } = useLoans(showAll)
  const { data: books = [] } = useBooks('all')
  const checkoutBook = useCheckoutBook()
  const returnBook = useReturnBook()
  const deleteLoan = useDeleteLoan()

  const handleCheckout = () => {
    setFormData({ bookId: '' })
    setError('')
    setShowCheckoutForm(true)
  }

  const handleCloseForm = () => {
    setShowCheckoutForm(false)
    setFormData({ bookId: '' })
    setError('')
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.bookId || !user) {
      setError('Please select a book')
      return
    }

    try {
      await checkoutBook.mutateAsync({
        bookId: parseInt(formData.bookId),
        userId: user.id,
      })
      handleCloseForm()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
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

  const availableBooks = books.filter((b) => b.status === 'ACTIVE')

  const bookOptions = availableBooks.map((b) => ({
    value: b.id,
    label: `${b.title} - ${b.author}`,
  }))

  const columns: Column<LoanDto>[] = [
    {
      key: 'bookTitle',
      header: 'Book',
      accessor: (loan) => (
        <div>
          <div className="font-medium text-gray-900">{loan.bookTitle}</div>
          {loan.userName && <div className="text-sm text-gray-500">Borrowed by: {loan.userName}</div>}
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

  const isSubmitting = checkoutBook.isPending

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Loans</h1>
        <Button variant="primary" onClick={handleCheckout} data-test="checkout-book">
          Checkout Book
        </Button>
      </div>

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
                {!loan.returnDate && (
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

      <Modal
        isOpen={showCheckoutForm}
        onClose={handleCloseForm}
        title="Checkout Book"
        size="md"
        footer={
          <div className="flex justify-end gap-3">
            <Button variant="ghost" onClick={handleCloseForm} disabled={isSubmitting} data-test="checkout-cancel">
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={isSubmitting}
              data-test="checkout-submit"
            >
              Checkout
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && <ErrorMessage message={error} />}

          <Select
            label="Book"
            value={formData.bookId}
            onChange={(e) => setFormData({ ...formData, bookId: e.target.value })}
            options={[{ value: '', label: 'Select a book' }, ...bookOptions]}
            required
            helpText="Only available books are shown"
            data-test="checkout-book-select"
          />

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-900">
              <strong>Borrower:</strong> {user?.username}
            </p>
            <p className="text-sm text-blue-900 mt-1">
              <strong>Due Date:</strong> 14 days from checkout
            </p>
          </div>
        </form>
      </Modal>

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
    </div>
  )
}
