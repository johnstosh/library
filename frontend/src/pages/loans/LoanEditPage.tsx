// (c) Copyright 2025 by Muczynski
import { useState, useEffect, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { Spinner } from '@/components/progress/Spinner'
import { ThrottledThumbnail } from '@/components/ui/ThrottledThumbnail'
import { useLoan, useUpdateLoan, useAddLoanPhoto } from '@/api/loans'
import { getThumbnailUrl, getPhotoUrl } from '@/api/photos'
import { parseISODateSafe } from '@/utils/formatters'
import { PiArrowLeft, PiCamera } from 'react-icons/pi'

function formatDateToInput(isoDate: string | undefined): string {
  if (!isoDate) return ''
  try {
    const date = parseISODateSafe(isoDate)
    if (isNaN(date.getTime())) return ''
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const year = date.getFullYear()
    return `${month}-${day}-${year}`
  } catch {
    return ''
  }
}

function parseInputToISO(dateStr: string): string {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  if (parts.length !== 3) return ''
  const [month, day, year] = parts
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
}

export function LoanEditPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const loanId = id ? parseInt(id, 10) : 0
  const { data: loan, isLoading } = useLoan(loanId)
  const updateLoan = useUpdateLoan()
  const addLoanPhoto = useAddLoanPhoto()

  const [checkoutDate, setCheckoutDate] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [returnDate, setReturnDate] = useState('')
  const [selectedPhoto, setSelectedPhoto] = useState<File | null>(null)
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (loan) {
      setCheckoutDate(formatDateToInput(loan.loanDate))
      setDueDate(formatDateToInput(loan.dueDate))
      setReturnDate(formatDateToInput(loan.returnDate))
    }
  }, [loan])

  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault()
        e.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [hasUnsavedChanges])

  const handleCancel = () => {
    if (hasUnsavedChanges && !window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
      return
    }
    navigate(`/loans/${loanId}`)
  }

  const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedPhoto(file)
      setHasUnsavedChanges(true)
    }
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    const loanDateISO = parseInputToISO(checkoutDate)
    const dueDateISO = parseInputToISO(dueDate)
    const returnDateISO = returnDate ? parseInputToISO(returnDate) : undefined

    if (!loanDateISO || !dueDateISO) {
      setError('Invalid date format. Please use MM-DD-YYYY.')
      return
    }

    try {
      await updateLoan.mutateAsync({
        id: loanId,
        bookId: loan!.bookId,
        userId: loan!.userId,
        loanDate: loanDateISO,
        dueDate: dueDateISO,
        ...(returnDateISO ? { returnDate: returnDateISO } : {}),
      })
      if (selectedPhoto) {
        await addLoanPhoto.mutateAsync({ id: loanId, photo: selectedPhoto })
      }
      setHasUnsavedChanges(false)
      navigate(`/loans/${loanId}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save changes')
    }
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
          <button onClick={() => navigate('/loans')} className="text-blue-600 hover:text-blue-800">
            Return to Loans
          </button>
        </div>
      </div>
    )
  }

  const isSaving = updateLoan.isPending || addLoanPhoto.isPending

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Button variant="ghost" onClick={handleCancel} leftIcon={<PiArrowLeft />}>
          Back to Loan
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200">
          <h1 className="text-2xl font-bold text-gray-900">Edit Loan</h1>
        </div>

        <form onSubmit={handleSave} className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

          {/* Read-only info */}
          <div className="bg-gray-50 rounded-lg p-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Book</p>
              <p className="text-gray-900">{loan.bookTitle}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Borrower</p>
              <p className="text-gray-900">{loan.userName}</p>
            </div>
          </div>

          {/* Editable dates */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Checkout Date"
              type="text"
              value={checkoutDate}
              onChange={(e) => { setCheckoutDate(e.target.value); setHasUnsavedChanges(true) }}
              placeholder="MM-DD-YYYY"
              required
              data-test="loan-edit-checkout-date"
            />
            <Input
              label="Due Date"
              type="text"
              value={dueDate}
              onChange={(e) => { setDueDate(e.target.value); setHasUnsavedChanges(true) }}
              placeholder="MM-DD-YYYY"
              required
              data-test="loan-edit-due-date"
            />
          </div>

          <Input
            label="Return Date (optional)"
            type="text"
            value={returnDate}
            onChange={(e) => { setReturnDate(e.target.value); setHasUnsavedChanges(true) }}
            placeholder="MM-DD-YYYY (leave blank if not returned)"
            data-test="loan-edit-return-date"
          />

          {/* Photo section */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <PiCamera className="w-5 h-5 text-gray-600" />
              <h3 className="text-base font-semibold text-gray-900">Checkout Card Photo</h3>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handlePhotoSelect}
              className="hidden"
              data-test="loan-edit-photo-input"
            />

            {selectedPhoto ? (
              <div className="space-y-3">
                <img
                  src={URL.createObjectURL(selectedPhoto)}
                  alt="Selected checkout card photo"
                  className="max-w-sm rounded border border-gray-300"
                />
                <Button
                  variant="ghost"
                  size="sm"
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  data-test="loan-edit-replace-photo"
                >
                  Choose Different Photo
                </Button>
              </div>
            ) : loan.photoId ? (
              <div className="space-y-3">
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
                    className="max-w-sm rounded border border-gray-300 hover:opacity-90 transition-opacity cursor-pointer"
                    respectOrientation
                  />
                </a>
                <div>
                  <Button
                    variant="secondary"
                    size="sm"
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    data-test="loan-edit-replace-photo"
                  >
                    Replace Photo
                  </Button>
                </div>
              </div>
            ) : (
              <Button
                variant="secondary"
                type="button"
                onClick={() => fileInputRef.current?.click()}
                data-test="loan-edit-add-photo"
              >
                Add Photo
              </Button>
            )}
          </div>
        </form>

        <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg flex justify-end gap-3">
          <Button
            variant="ghost"
            type="button"
            onClick={handleCancel}
            disabled={isSaving}
            data-test="loan-edit-cancel"
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            type="submit"
            onClick={handleSave}
            isLoading={isSaving}
            data-test="loan-edit-save"
          >
            Save
          </Button>
        </div>
      </div>
    </div>
  )
}
