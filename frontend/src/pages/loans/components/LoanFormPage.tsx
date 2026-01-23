// (c) Copyright 2025 by Muczynski
import { useState, useEffect, useMemo, useRef } from 'react'
import { Select } from '@/components/ui/Select'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { Spinner } from '@/components/progress/Spinner'
import { useCheckoutBook, useCheckoutBookWithPhoto, useTranscribeCheckoutCard } from '@/api/loans'
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
  captureMode?: 'file' | 'camera' | null
}

export function LoanFormPage({ title, loan, onSuccess, onCancel, initialFilters, captureMode }: LoanFormPageProps) {
  const isEditing = !!loan
  const isLibrarian = useIsLibrarian()
  const currentUser = useAuthStore((state) => state.user)

  // Photo capture state
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [isTranscribing, setIsTranscribing] = useState(false)
  const [transcriptionError, setTranscriptionError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const hasTriggeredFileInput = useRef(false)
  const transcribeMutation = useTranscribeCheckoutCard()

  // Helper to normalize date from Grok format to form format (MM-DD-YYYY)
  // Returns empty string for invalid/unparseable dates to prevent crashes
  const normalizeDate = (dateStr: string): string => {
    if (!dateStr || dateStr === 'N/A') return ''
    try {
      // Handle formats like "1-7-24", "01-07-24", "1-28"
      const parts = dateStr.split('-')
      if (parts.length === 2) {
        // Format: M-D (no year), assume current year
        const [month, day] = parts
        const monthNum = parseInt(month, 10)
        const dayNum = parseInt(day, 10)
        if (isNaN(monthNum) || isNaN(dayNum) || monthNum < 1 || monthNum > 12 || dayNum < 1 || dayNum > 31) {
          return '' // Invalid date parts
        }
        const year = new Date().getFullYear()
        return `${month.padStart(2, '0')}-${day.padStart(2, '0')}-${year}`
      } else if (parts.length === 3) {
        // Format: M-D-YY or M-D-YYYY
        const [month, day, yearPart] = parts
        const monthNum = parseInt(month, 10)
        const dayNum = parseInt(day, 10)
        const yearNum = parseInt(yearPart, 10)
        if (isNaN(monthNum) || isNaN(dayNum) || isNaN(yearNum) || monthNum < 1 || monthNum > 12 || dayNum < 1 || dayNum > 31) {
          return '' // Invalid date parts
        }
        const year = yearPart.length === 2 ? `20${yearPart}` : yearPart
        return `${month.padStart(2, '0')}-${day.padStart(2, '0')}-${year}`
      }
      return '' // Unrecognized format
    } catch {
      return '' // Any parsing error returns empty string
    }
  }

  // Helper to get today's date in MM-DD-YYYY format safely
  const getTodayFormatted = (): string => {
    try {
      const today = new Date()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')
      const year = today.getFullYear()
      return `${month}-${day}-${year}`
    } catch {
      return '' // Fallback if date operations fail
    }
  }

  const [formData, setFormData] = useState({
    bookId: '',
    userId: '',
    checkoutDate: initialFilters?.checkoutDate
      ? normalizeDate(initialFilters.checkoutDate)
      : getTodayFormatted(),
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

  // Auto-trigger file input when captureMode is set
  useEffect(() => {
    if (captureMode && !hasTriggeredFileInput.current && fileInputRef.current) {
      hasTriggeredFileInput.current = true
      // Small delay to ensure the input is mounted
      setTimeout(() => {
        fileInputRef.current?.click()
      }, 100)
    }
  }, [captureMode])

  // Handle file selection and auto-transcribe
  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setSelectedFile(file)
    const url = URL.createObjectURL(file)
    setPreviewUrl(url)
    setTranscriptionError(null)
    setIsTranscribing(true)

    try {
      const result = await transcribeMutation.mutateAsync(file)

      // Update form filters with transcribed data
      if (result.title && result.title !== 'N/A') {
        setBookFilters(prev => ({ ...prev, title: result.title }))
      }
      if (result.author && result.author !== 'N/A') {
        setBookFilters(prev => ({ ...prev, author: result.author }))
      }
      if (result.callNumber && result.callNumber !== 'N/A') {
        setBookFilters(prev => ({ ...prev, locNumber: result.callNumber }))
      }
      if (result.lastIssuedTo && result.lastIssuedTo !== 'N/A') {
        setUserFilter(result.lastIssuedTo)
      }
      if (result.lastDate && result.lastDate !== 'N/A') {
        setFormData(prev => ({ ...prev, checkoutDate: normalizeDate(result.lastDate) }))
        skipDueDateAutoCalc.current = true
      }
      if (result.lastDue && result.lastDue !== 'N/A') {
        setFormData(prev => ({ ...prev, dueDate: normalizeDate(result.lastDue) }))
      }

      setHasUnsavedChanges(true)
    } catch (err) {
      setTranscriptionError(err instanceof Error ? err.message : 'Failed to transcribe checkout card')
    } finally {
      setIsTranscribing(false)
    }
  }

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
        try {
          const today = new Date()
          const twoWeeksLater = new Date(today)
          twoWeeksLater.setDate(today.getDate() + 14)

          const checkoutDateStr = formatDateToInput(today.toISOString())
          const dueDateStr = formatDateToInput(twoWeeksLater.toISOString())

          if (checkoutDateStr && dueDateStr) {
            setFormData(prev => ({
              ...prev,
              checkoutDate: checkoutDateStr,
              dueDate: dueDateStr,
            }))
          }
        } catch {
          // Ignore date errors - form will show empty dates which user can fill
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loan, currentUser, isLibrarian])

  // Helper function to format ISO date string to MM-DD-YYYY
  // Uses parseISODateSafe to avoid timezone issues with date-only strings
  // Returns empty string for invalid dates to prevent crashes
  const formatDateToInput = (isoDate: string): string => {
    if (!isoDate) return ''
    try {
      const date = parseISODateSafe(isoDate)
      // Check if date is valid
      if (isNaN(date.getTime())) {
        return ''
      }
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const year = date.getFullYear()
      return `${month}-${day}-${year}`
    } catch {
      return '' // Any parsing error returns empty string
    }
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
      try {
        const isoDate = parseInputToISO(formData.checkoutDate)
        if (isoDate) {
          const checkoutDate = new Date(isoDate)
          // Check if date is valid before calculating due date
          if (!isNaN(checkoutDate.getTime())) {
            const dueDate = new Date(checkoutDate)
            dueDate.setDate(checkoutDate.getDate() + 14)
            const formattedDueDate = formatDateToInput(dueDate.toISOString())
            if (formattedDueDate) {
              setFormData(prev => ({
                ...prev,
                dueDate: formattedDueDate,
              }))
            }
          }
        }
      } catch {
        // Ignore date calculation errors - user can manually enter due date
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
    setBookFilters(prev => ({ ...prev, [field]: value }))
    setHasUnsavedChanges(true)
  }

  const handleUserFilterChange = (value: string) => {
    setUserFilter(value)
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

    // Parse and validate dates
    const loanDate = parseInputToISO(formData.checkoutDate)
    const dueDate = parseInputToISO(formData.dueDate)

    if (!loanDate || !dueDate) {
      setError('Invalid date format. Please use MM-DD-YYYY format.')
      return
    }

    try {
      if (selectedFile) {
        // Use checkout-with-photo endpoint
        await checkoutBookWithPhoto.mutateAsync({
          bookId: parseInt(formData.bookId),
          userId: parseInt(formData.userId),
          loanDate,
          dueDate,
          photo: selectedFile,
        })
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

  // Helper to normalize call numbers by removing all spaces for comparison
  const normalizeCallNumber = (callNumber: string): string => {
    return callNumber.replace(/\s+/g, '').toLowerCase()
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
      // Normalize call number filter by removing spaces
      const locMatch = normalizeCallNumber(bookFilters.locNumber)

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

      // Normalize book's call number by removing spaces before comparison
      const bookLocNormalized = book.locNumber ? normalizeCallNumber(book.locNumber) : ''
      if (locMatch && bookLocNormalized.includes(locMatch)) {
        score += 10
        // Exact match gets bonus
        if (bookLocNormalized === locMatch) score += 20
      }

      return { book, score }
    })

    // Sort by score (highest first) and return books with score > 0
    return booksWithScore
      .filter(item => item.score > 0)
      .sort((a, b) => b.score - a.score)
      .map(item => item.book)
  }, [books, bookFilters.title, bookFilters.author, bookFilters.locNumber])

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

  // Auto-select or update book selection when filtered list changes
  useEffect(() => {
    if (isEditing) return

    const hasFilters = bookFilters.title || bookFilters.author || bookFilters.locNumber

    if (hasFilters && filteredBooks.length > 0) {
      // Check if currently selected book is still in filtered results
      const currentBookInFiltered = formData.bookId &&
        filteredBooks.some(b => b.id.toString() === formData.bookId)

      if (!currentBookInFiltered) {
        // Update to first filtered book if current selection is not in results
        setFormData(prev => ({ ...prev, bookId: filteredBooks[0].id.toString() }))
      }
    } else if (hasFilters && filteredBooks.length === 0 && formData.bookId) {
      // Clear selection if filters are set but no books match
      setFormData(prev => ({ ...prev, bookId: '' }))
    }
  }, [filteredBooks, bookFilters, isEditing])

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

  // Determine if we should show the photo panel
  const showPhotoPanel = captureMode || selectedFile

  return (
    <div className={`flex gap-6 ${showPhotoPanel ? '' : ''}`}>
      {/* Main Form */}
      <div className={`bg-white rounded-lg shadow ${showPhotoPanel ? 'flex-1' : 'w-full'}`}>
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

      {/* Photo Panel - shown when captureMode is set or photo is selected */}
      {showPhotoPanel && (
        <div className="w-96 bg-white rounded-lg shadow">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">
              {captureMode === 'camera' ? 'Checkout by Camera' : 'Checkout by Photo'}
            </h2>
          </div>

          <div className="p-6 space-y-4">
            {/* Hidden file input */}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              capture={captureMode === 'camera' ? 'environment' : undefined}
              onChange={handleFileSelect}
              className="hidden"
              data-test="checkout-card-photo-input"
            />

            {/* Select Photo Button - shown if no photo yet */}
            {!previewUrl && !isTranscribing && (
              <Button
                variant="secondary"
                onClick={() => fileInputRef.current?.click()}
                className="w-full"
                data-test="select-photo-button"
              >
                {captureMode === 'camera' ? 'Take Photo' : 'Select Photo'}
              </Button>
            )}

            {/* Loading indicator during transcription */}
            {isTranscribing && (
              <div className="flex flex-col items-center justify-center py-8">
                <Spinner size="lg" />
                <p className="mt-4 text-sm text-gray-600">Transcribing checkout card...</p>
              </div>
            )}

            {/* Error message */}
            {transcriptionError && (
              <div className="p-4 bg-red-50 border border-red-200 rounded">
                <p className="text-red-700 text-sm">{transcriptionError}</p>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setTranscriptionError(null)
                    fileInputRef.current?.click()
                  }}
                  className="mt-2"
                >
                  Try Again
                </Button>
              </div>
            )}

            {/* Image preview */}
            {previewUrl && !isTranscribing && (
              <div className="space-y-4">
                <img
                  src={previewUrl}
                  alt="Checkout card"
                  className="w-full rounded border border-gray-300"
                />
                <p className="text-xs text-gray-500 text-center">
                  Compare the image with the form data above
                </p>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    if (fileInputRef.current) {
                      fileInputRef.current.value = ''  // Reset to allow re-selecting same file
                      fileInputRef.current.click()
                    }
                  }}
                  className="w-full"
                >
                  Select Different Photo
                </Button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
