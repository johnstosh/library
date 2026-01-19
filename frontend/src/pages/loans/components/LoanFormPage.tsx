// (c) Copyright 2025 by Muczynski
import { useState, useEffect, useMemo, useRef } from 'react'
import { Select } from '@/components/ui/Select'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useCheckoutBook, useCheckoutBookWithPhoto } from '@/api/loans'
import { useBooks } from '@/api/books'
import { useUsers } from '@/api/users'
import { useAuthStore } from '@/stores/authStore'
import { useIsLibrarian } from '@/stores/authStore'
import { formatDate, parseISODateSafe } from '@/utils/formatters'
import type { LoanDto } from '@/types/dtos'

interface InitialFilters {
  title?: string
  author?: string
  locNumber?: string
  borrower?: string
  checkoutDate?: string
  dueDate?: string
  hasPhoto?: boolean
}

interface LoanFormPageProps {
  title: string
  loan?: LoanDto
  onSuccess: () => void
  onCancel: () => void
  initialFilters?: InitialFilters
}

export function LoanFormPage({ title, loan, onSuccess, onCancel, initialFilters }: LoanFormPageProps) {
  const isEditing = !!loan
  const isLibrarian = useIsLibrarian()
  const currentUser = useAuthStore((state) => state.user)

  // Helper to normalize date from Grok format to form format (MM-DD-YYYY)
  const normalizeDate = (dateStr: string): string => {
    if (!dateStr) return ''
    // Handle formats like "1-7-24", "01-07-24", "1-28"
    const parts = dateStr.split('-')
    if (parts.length === 2) {
      // Format: M-D (no year), assume current year
      const [month, day] = parts
      const year = new Date().getFullYear()
      return `${month.padStart(2, '0')}-${day.padStart(2, '0')}-${year}`
    } else if (parts.length === 3) {
      // Format: M-D-YY or M-D-YYYY
      const [month, day, yearPart] = parts
      const year = yearPart.length === 2 ? `20${yearPart}` : yearPart
      return `${month.padStart(2, '0')}-${day.padStart(2, '0')}-${year}`
    }
    return dateStr
  }

  const [formData, setFormData] = useState({
    bookId: '',
    userId: '',
    checkoutDate: initialFilters?.checkoutDate
      ? normalizeDate(initialFilters.checkoutDate)
      : new Date().toLocaleDateString('en-US', { month: '2-digit', day: '2-digit', year: 'numeric' }),
    dueDate: initialFilters?.dueDate ? normalizeDate(initialFilters.dueDate) : '',
  })
  const [bookFilters, setBookFilters] = useState({
    title: initialFilters?.title || '',
    author: initialFilters?.author || '',
    locNumber: initialFilters?.locNumber || '',
  })
  const [userFilter, setUserFilter] = useState(initialFilters?.borrower || '')
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const { data: books = [] } = useBooks('all')
  const { data: users = [] } = useUsers()
  const checkoutBook = useCheckoutBook()
  const checkoutBookWithPhoto = useCheckoutBookWithPhoto()

  // Track if initial transcription dueDate should be preserved (skip first auto-calc)
  const skipDueDateAutoCalc = useRef(!!initialFilters?.dueDate)

  useEffect(() => {
    if (loan) {
      setFormData({
        bookId: loan.bookId.toString(),
        userId: loan.userId.toString(),
        checkoutDate: formatDateToInput(loan.loanDate),
        dueDate: formatDateToInput(loan.dueDate),
      })
    } else {
      // New loan - set user if applicable
      if (currentUser && !isLibrarian) {
        // Default to current user if not a librarian
        setFormData(prev => ({
          ...prev,
          userId: currentUser.id.toString(),
        }))
      }
      // Only set default dates if no transcription dates were provided
      if (!initialFilters?.checkoutDate) {
        const today = new Date()
        const twoWeeksLater = new Date(today)
        twoWeeksLater.setDate(today.getDate() + 14)

        setFormData(prev => ({
          ...prev,
          checkoutDate: formatDateToInput(today.toISOString()),
          dueDate: formatDateToInput(twoWeeksLater.toISOString()),
        }))
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loan, currentUser, isLibrarian])

  // Helper function to format ISO date string to MM-DD-YYYY
  // Uses parseISODateSafe to avoid timezone issues with date-only strings
  const formatDateToInput = (isoDate: string): string => {
    if (!isoDate) return ''
    const date = parseISODateSafe(isoDate)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const year = date.getFullYear()
    return `${month}-${day}-${year}`
  }

  // Helper function to parse MM-DD-YYYY to ISO date string
  const parseInputToISO = (dateStr: string): string => {
    if (!dateStr) return ''
    const parts = dateStr.split('-')
    if (parts.length !== 3) return ''
    const [month, day, year] = parts
    return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
  }

  // Update due date when checkout date changes
  useEffect(() => {
    // Skip auto-calculation on initial mount if transcription provided dueDate
    if (skipDueDateAutoCalc.current) {
      skipDueDateAutoCalc.current = false
      return
    }

    if (formData.checkoutDate && !isEditing) {
      const isoDate = parseInputToISO(formData.checkoutDate)
      if (isoDate) {
        const checkoutDate = new Date(isoDate)
        const dueDate = new Date(checkoutDate)
        dueDate.setDate(checkoutDate.getDate() + 14)
        setFormData(prev => ({
          ...prev,
          dueDate: formatDateToInput(dueDate.toISOString()),
        }))
      }
    }
  }, [formData.checkoutDate, isEditing])

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

  const handleBookFilterChange = (field: string, value: string) => {
    setBookFilters({ ...bookFilters, [field]: value })
    setHasUnsavedChanges(true)
  }

  const handleUserFilterChange = (value: string) => {
    setUserFilter(value)
    setHasUnsavedChanges(true)
  }

  // Helper function to convert base64 data URL to File
  const base64ToFile = (dataUrl: string, filename: string, mimeType: string): File => {
    const arr = dataUrl.split(',')
    const bstr = atob(arr[1])
    let n = bstr.length
    const u8arr = new Uint8Array(n)
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n)
    }
    return new File([u8arr], filename, { type: mimeType })
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

    // Parse and validate dates
    const loanDate = parseInputToISO(formData.checkoutDate)
    const dueDate = parseInputToISO(formData.dueDate)

    if (!loanDate || !dueDate) {
      setError('Invalid date format. Please use MM-DD-YYYY format.')
      return
    }

    try {
      // Check if we have a checkout card photo from the transcription
      const storedPhotoData = sessionStorage.getItem('checkoutCardPhoto')
      let photoFile: File | undefined

      if (initialFilters?.hasPhoto && storedPhotoData) {
        try {
          const photoInfo = JSON.parse(storedPhotoData)
          photoFile = base64ToFile(photoInfo.data, photoInfo.name, photoInfo.type)
        } catch (photoErr) {
          console.warn('Failed to parse stored photo, continuing without photo:', photoErr)
        }
      }

      if (photoFile) {
        // Use checkout-with-photo endpoint
        await checkoutBookWithPhoto.mutateAsync({
          bookId: parseInt(formData.bookId),
          userId: parseInt(formData.userId),
          loanDate,
          dueDate,
          photo: photoFile,
        })
        // Clear the stored photo after successful checkout
        sessionStorage.removeItem('checkoutCardPhoto')
      } else {
        // Use regular checkout endpoint
        await checkoutBook.mutateAsync({
          bookId: parseInt(formData.bookId),
          userId: parseInt(formData.userId),
          loanDate,
          dueDate,
        })
      }

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

  // Filter and rank books based on title, author, and locNumber
  const filteredBooks = useMemo(() => {
    const availableBooks = books.filter((b) => b.status === 'ACTIVE')

    if (!bookFilters.title && !bookFilters.author && !bookFilters.locNumber) {
      return availableBooks
    }

    // Calculate match score for each book
    const booksWithScore = availableBooks.map((book) => {
      let score = 0
      const titleMatch = bookFilters.title.toLowerCase()
      const authorMatch = bookFilters.author.toLowerCase()
      const locMatch = bookFilters.locNumber.toLowerCase()

      if (titleMatch && book.title.toLowerCase().includes(titleMatch)) {
        score += 10
        // Exact match gets bonus
        if (book.title.toLowerCase() === titleMatch) score += 20
      }

      if (authorMatch && book.author?.toLowerCase().includes(authorMatch)) {
        score += 10
        // Exact match gets bonus
        if (book.author?.toLowerCase() === authorMatch) score += 20
      }

      if (locMatch && book.locNumber?.toLowerCase().includes(locMatch)) {
        score += 10
        // Exact match gets bonus
        if (book.locNumber?.toLowerCase() === locMatch) score += 20
      }

      return { book, score }
    })

    // Sort by score (highest first) and return books with score > 0
    return booksWithScore
      .filter(item => item.score > 0)
      .sort((a, b) => b.score - a.score)
      .map(item => item.book)
  }, [books, bookFilters])

  // Filter users based on username
  const filteredUsers = useMemo(() => {
    if (!userFilter) {
      return users
    }

    const filterLower = userFilter.toLowerCase()
    return users.filter((user) =>
      user.username.toLowerCase().includes(filterLower)
    )
  }, [users, userFilter])


  // Update book filters when a book is selected
  useEffect(() => {
    if (formData.bookId) {
      const selectedBook = books.find(b => b.id === parseInt(formData.bookId))
      if (selectedBook && !bookFilters.title && !bookFilters.author && !bookFilters.locNumber) {
        setBookFilters({
          title: selectedBook.title,
          author: selectedBook.author || '',
          locNumber: selectedBook.locNumber || '',
        })
      }
    }
  }, [formData.bookId, books])

  // Auto-select first book when filtered list changes and no book is selected
  useEffect(() => {
    if (!isEditing && !formData.bookId && filteredBooks.length > 0 &&
        (bookFilters.title || bookFilters.author || bookFilters.locNumber)) {
      setFormData(prev => ({ ...prev, bookId: filteredBooks[0].id.toString() }))
    }
  }, [filteredBooks, formData.bookId, bookFilters, isEditing])

  // Auto-select first user when filtered list changes and no user is selected
  useEffect(() => {
    if (!isEditing && !formData.userId && filteredUsers.length > 0 && userFilter) {
      setFormData(prev => ({ ...prev, userId: filteredUsers[0].id.toString() }))
    }
  }, [filteredUsers, formData.userId, userFilter, isEditing])

  const bookOptions = filteredBooks.map((b) => ({
    value: b.id,
    label: `${b.title} - ${b.author}`,
  }))

  const userOptions = filteredUsers.map((u) => ({
    value: u.id,
    label: u.username,
  }))

  const isLoading = checkoutBook.isPending || checkoutBookWithPhoto.isPending

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
                    !loan.returnDate && parseISODateSafe(loan.dueDate) < new Date()
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
            {/* Book Selection with Title/Author/LOC filters */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <div>
                <Select
                  label="Book"
                  value={formData.bookId}
                  onChange={(e) => handleFieldChange('bookId', e.target.value)}
                  options={[{ value: '', label: 'Select a book' }, ...bookOptions]}
                  required
                  helpText={`Showing ${filteredBooks.length} available books`}
                  data-test="loan-book-select"
                  disabled={isEditing}
                />
              </div>
              <div className="space-y-3">
                <Input
                  label="Title Filter"
                  value={bookFilters.title}
                  onChange={(e) => handleBookFilterChange('title', e.target.value)}
                  placeholder="Filter books by title"
                  data-test="loan-title-filter"
                />
                <Input
                  label="Author Filter"
                  value={bookFilters.author}
                  onChange={(e) => handleBookFilterChange('author', e.target.value)}
                  placeholder="Filter books by author"
                  data-test="loan-author-filter"
                />
                <Input
                  label="Call Number Filter"
                  value={bookFilters.locNumber}
                  onChange={(e) => handleBookFilterChange('locNumber', e.target.value)}
                  placeholder="Filter books by LOC number"
                  data-test="loan-loc-filter"
                />
              </div>
            </div>

            {/* Borrower Selection with Filter */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <div>
                <Select
                  label="Borrower"
                  value={formData.userId}
                  onChange={(e) => handleFieldChange('userId', e.target.value)}
                  options={[{ value: '', label: 'Select a user' }, ...userOptions]}
                  required
                  helpText={`Showing ${filteredUsers.length} users`}
                  data-test="loan-user-select"
                  disabled={isEditing || !isLibrarian}
                />
              </div>
              <div>
                <Input
                  label="Borrower Filter"
                  value={userFilter}
                  onChange={(e) => handleUserFilterChange(e.target.value)}
                  placeholder="Filter borrowers by username"
                  data-test="loan-user-filter"
                />
              </div>
            </div>

            {/* Checkout Date */}
            <div className="grid grid-cols-1 gap-4">
              <Input
                label="Checkout Date"
                type="text"
                value={formData.checkoutDate}
                onChange={(e) => handleFieldChange('checkoutDate', e.target.value)}
                required
                placeholder="MM-DD-YYYY"
                helpText="Enter checkout date in MM-DD-YYYY format (defaults to today)"
                data-test="loan-checkout-date"
                disabled={isEditing}
              />
            </div>

            {/* Due Date */}
            <div className="grid grid-cols-1 gap-4">
              <Input
                label="Due Date"
                type="text"
                value={formData.dueDate}
                onChange={(e) => handleFieldChange('dueDate', e.target.value)}
                required
                placeholder="MM-DD-YYYY"
                helpText="Enter due date in MM-DD-YYYY format (defaults to 2 weeks from checkout)"
                data-test="loan-due-date"
                disabled={isEditing}
              />
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
