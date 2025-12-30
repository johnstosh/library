// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useCheckoutBook } from '@/api/loans'
import { useBooks } from '@/api/books'
import { useUsers } from '@/api/users'
import { useAuthStore } from '@/stores/authStore'
import { useIsLibrarian } from '@/stores/authStore'
import { formatDate } from '@/utils/formatters'
import type { LoanDto } from '@/types/dtos'

interface LoanFormPageProps {
  title: string
  loan?: LoanDto
  onSuccess: () => void
  onCancel: () => void
}

export function LoanFormPage({ title, loan, onSuccess, onCancel }: LoanFormPageProps) {
  const isEditing = !!loan
  const isLibrarian = useIsLibrarian()
  const currentUser = useAuthStore((state) => state.user)

  const [formData, setFormData] = useState({
    bookId: '',
    userId: '',
  })
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const { data: books = [] } = useBooks('all')
  const { data: users = [] } = useUsers()
  const checkoutBook = useCheckoutBook()

  useEffect(() => {
    if (loan) {
      setFormData({
        bookId: loan.bookId.toString(),
        userId: loan.userId.toString(),
      })
    } else if (currentUser && !isLibrarian) {
      // Default to current user if not a librarian
      setFormData({
        bookId: '',
        userId: currentUser.id.toString(),
      })
    }
  }, [loan, currentUser, isLibrarian])

  // Warn user about unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault()
        e.returnValue = ''
      }
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const handleFieldChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value })
    setHasUnsavedChanges(true)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.bookId || !formData.userId) {
      setError('Book and User are required')
      return
    }

    // Only allow checkout for new loans
    if (isEditing) {
      setError('Loans cannot be edited. Use Return or Delete actions instead.')
      return
    }

    try {
      await checkoutBook.mutateAsync({
        bookId: parseInt(formData.bookId),
        userId: parseInt(formData.userId),
      })

      setHasUnsavedChanges(false)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      if (window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
        onCancel()
      }
    } else {
      onCancel()
    }
  }

  const availableBooks = books.filter((b) => b.status === 'ACTIVE')

  const bookOptions = availableBooks.map((b) => ({
    value: b.id,
    label: `${b.title} - ${b.author}`,
  }))

  const userOptions = users.map((u) => ({
    value: u.id,
    label: u.username,
  }))

  const isLoading = checkoutBook.isPending

  // Calculate due date (14 days from today for new loans)
  const dueDate = new Date()
  dueDate.setDate(dueDate.getDate() + 14)

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
        {error && <ErrorMessage message={error} />}

        {isEditing && loan ? (
          // View mode for existing loans
          <div className="space-y-4">
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
                    !loan.returnDate && new Date(loan.dueDate) < new Date()
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
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <p className="text-sm text-blue-900">
                Loans cannot be edited directly. Use the Return button to mark this loan as returned, or use the Delete button to remove it.
              </p>
            </div>
          </div>
        ) : (
          // Edit mode for new loans
          <>
            <Select
              label="Book"
              value={formData.bookId}
              onChange={(e) => handleFieldChange('bookId', e.target.value)}
              options={[{ value: '', label: 'Select a book' }, ...bookOptions]}
              required
              helpText="Only available books are shown"
              data-test="loan-book-select"
              disabled={isEditing}
            />

            <Select
              label="Borrower"
              value={formData.userId}
              onChange={(e) => handleFieldChange('userId', e.target.value)}
              options={[{ value: '', label: 'Select a user' }, ...userOptions]}
              required
              data-test="loan-user-select"
              disabled={isEditing || !isLibrarian}
            />

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <p className="text-sm text-blue-900">
                <strong>Checkout Date:</strong> {formatDate(new Date().toISOString())}
              </p>
              <p className="text-sm text-blue-900 mt-1">
                <strong>Due Date:</strong> {formatDate(dueDate.toISOString())} (14 days from checkout)
              </p>
            </div>
          </>
        )}
      </form>

      {/* Footer */}
      <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={handleCancel}
            disabled={isLoading}
            data-test="loan-form-cancel"
          >
            {isEditing ? 'Close' : 'Cancel'}
          </Button>
          {!isEditing && (
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={isLoading}
              data-test="loan-form-submit"
            >
              Checkout
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
