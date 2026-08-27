// (c) Copyright 2025 by Muczynski
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useLoans, useReturnBook, useDeleteLoan } from '@/api/loans'
import { useLoansChips, useUiStore } from '@/stores/uiStore'
import { applyLoanChipFilters } from '@/utils/loanChipFilters'
import { LoanFilters } from './components/LoanFilters'
import { formatDate, parseISODateSafe } from '@/utils/formatters'
import type { LoanDto } from '@/types/dtos'
import { useIsLibrarian } from '@/stores/authStore'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { loanStatusTone } from '@/utils/status'
import { IconButton } from '@/components/ui/IconButton'
import { EntityLink } from '@/components/ui/EntityLink'
import { DeleteIcon, ReturnIcon, ViewIcon } from '@/components/ui/Icons'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { useToast } from '@/hooks/useToast'

export function LoansPage() {
  const navigate = useNavigate()
  const [returnLoanId, setReturnLoanId] = useState<number | null>(null)
  const [deleteLoanId, setDeleteLoanId] = useState<number | null>(null)

  const chips = useLoansChips()
  const toggleLoansChip = useUiStore((state) => state.toggleLoansChip)
  const isLibrarian = useIsLibrarian()

  const { data: allLoans = [], isLoading, error, isFetching } = useLoans(true)
  const loans = useMemo(() => applyLoanChipFilters(allLoans, chips), [allLoans, chips])
  const returnBook = useReturnBook()
  const deleteLoan = useDeleteLoan()
  const toast = useToast()

  const handleCheckout = () => {
    navigate('/loans/new')
  }

  const handleCheckoutByPhoto = () => {
    navigate('/loans/new?captureMode=file')
  }

  const handleCheckoutByCamera = () => {
    navigate('/loans/new?captureMode=camera')
  }

  const handleReturn = async () => {
    if (returnLoanId === null) return

    try {
      await returnBook.mutateAsync(returnLoanId)
      setReturnLoanId(null)
    } catch (error) {
      console.error('Failed to return book:', error)
      toast.error('Failed to return book')
    }
  }

  const handleDelete = async () => {
    if (deleteLoanId === null) return

    try {
      await deleteLoan.mutateAsync(deleteLoanId)
      setDeleteLoanId(null)
    } catch (error) {
      console.error('Failed to delete loan:', error)
      toast.error('Failed to delete loan')
    }
  }

  const columns: Column<LoanDto>[] = [
    {
      key: 'bookTitle',
      header: 'Book',
      accessor: (loan) => (
        <div>
          <EntityLink to={`/books/${loan.bookId}`} className="font-medium" data-test={`view-loan-book-${loan.id}`}>
            {loan.bookTitle}
          </EntityLink>
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
            !loan.returnDate && parseISODateSafe(loan.dueDate) < new Date()
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
        const isOverdue = !loan.returnDate && parseISODateSafe(loan.dueDate) < new Date()
        const label = loan.returnDate ? 'Returned' : isOverdue ? 'Overdue' : 'Active'
        return (
          <StatusBadge tone={loanStatusTone(!!loan.returnDate, isOverdue)}>
            {label}
          </StatusBadge>
        )
      },
      width: '10%',
    },
  ]

  return (
    <div>
      <PageHeader
        title="Loans"
        actions={
          <>
            <Button variant="secondary" onClick={handleCheckoutByPhoto} data-test="checkout-by-photo">
              Checkout by Photo
            </Button>
            <Button variant="secondary" onClick={handleCheckoutByCamera} data-test="checkout-by-camera">
              Checkout by Camera
            </Button>
            <Button variant="primary" onClick={handleCheckout} data-test="checkout-book">
              Checkout Book
            </Button>
          </>
        }
      />

      {error && (
        <ErrorMessage message={`Error loading loans: ${error.message}`} className="mb-4" />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200">
          <LoanFilters chips={chips} onToggle={toggleLoansChip} />
        </div>

        <div className="p-4">
          <DataTable
            data={loans}
            columns={columns}
            keyExtractor={(loan) => loan.id}
            actions={(loan) => (
              <>
                <IconButton
                  to={`/loans/${loan.id}`}
                  icon={<ViewIcon />}
                  label="View Details"
                  data-test={`view-loan-details-${loan.id}`}
                />
                {isLibrarian && !loan.returnDate && (
                  <IconButton
                    icon={<ReturnIcon />}
                    label="Return Book"
                    tone="success"
                    onClick={() => setReturnLoanId(loan.id)}
                    data-test={`return-loan-${loan.id}`}
                  />
                )}
                {isLibrarian && (
                  <IconButton
                    icon={<DeleteIcon />}
                    label="Delete"
                    tone="danger"
                    onClick={() => setDeleteLoanId(loan.id)}
                    data-test={`delete-loan-${loan.id}`}
                  />
                )}
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No loans found"
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={loans.length} singular="loan" plural="loans" isLoading={isLoading} />
      </PageCard>

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
