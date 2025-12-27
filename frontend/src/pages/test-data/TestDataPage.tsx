// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { Spinner } from '@/components/progress/Spinner'
import {
  useTestDataStats,
  useGenerateTestData,
  useGenerateLoanData,
  useDeleteAllTestData,
  useTotalPurge,
} from '@/api/test-data'

export function TestDataPage() {
  const [numBooks, setNumBooks] = useState(10)
  const [numLoans, setNumLoans] = useState(5)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showPurgeConfirm, setShowPurgeConfirm] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const { data: stats, isLoading: statsLoading } = useTestDataStats()
  const generateBooks = useGenerateTestData()
  const generateLoans = useGenerateLoanData()
  const deleteAll = useDeleteAllTestData()
  const totalPurge = useTotalPurge()

  const handleGenerateBooks = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await generateBooks.mutateAsync(numBooks)
      setSuccessMessage(result.message)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to generate test data')
    }
  }

  const handleGenerateLoans = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const result = await generateLoans.mutateAsync(numLoans)
      setSuccessMessage(result.message)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to generate loan data')
    }
  }

  const handleDeleteAll = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      await deleteAll.mutateAsync()
      setSuccessMessage('All test data deleted successfully')
      setShowDeleteConfirm(false)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to delete test data')
      setShowDeleteConfirm(false)
    }
  }

  const handleTotalPurge = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      await totalPurge.mutateAsync()
      setSuccessMessage('Database purged successfully')
      setShowPurgeConfirm(false)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to purge database')
      setShowPurgeConfirm(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">Test Data Management</h1>

      <div className="space-y-6">
        {/* Warning Banner */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <p className="text-sm text-yellow-900 font-medium">
            ⚠️ Development Environment Only
          </p>
          <p className="text-sm text-yellow-800 mt-1">
            These tools are for generating test data in development environments. Use with caution.
          </p>
        </div>

        {successMessage && <SuccessMessage message={successMessage} />}
        {errorMessage && <ErrorMessage message={errorMessage} />}

        {/* Current Statistics */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Current Statistics</h2>

          {statsLoading ? (
            <div className="flex justify-center py-6">
              <Spinner />
            </div>
          ) : (
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-blue-50 rounded-lg p-4">
                <p className="text-sm font-medium text-blue-900">Books</p>
                <p className="text-3xl font-bold text-blue-600 mt-2" data-test="books-count">
                  {stats?.books || 0}
                </p>
              </div>
              <div className="bg-green-50 rounded-lg p-4">
                <p className="text-sm font-medium text-green-900">Authors</p>
                <p className="text-3xl font-bold text-green-600 mt-2" data-test="authors-count">
                  {stats?.authors || 0}
                </p>
              </div>
              <div className="bg-purple-50 rounded-lg p-4">
                <p className="text-sm font-medium text-purple-900">Loans</p>
                <p className="text-3xl font-bold text-purple-600 mt-2" data-test="loans-count">
                  {stats?.loans || 0}
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Generate Books */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Generate Books & Authors</h2>
          <p className="text-sm text-gray-600 mb-4">
            Creates random books with authors, assigns them to libraries, and optionally adds LOC call numbers.
          </p>

          <div className="flex items-end gap-4">
            <div className="flex-1">
              <Input
                label="Number of Books"
                type="number"
                min="1"
                max="1000"
                value={numBooks}
                onChange={(e) => setNumBooks(parseInt(e.target.value) || 0)}
                data-test="num-books-input"
              />
            </div>
            <Button
              variant="primary"
              onClick={handleGenerateBooks}
              isLoading={generateBooks.isPending}
              data-test="generate-books"
            >
              Generate Books
            </Button>
          </div>
        </div>

        {/* Generate Loans */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Generate Loans</h2>
          <p className="text-sm text-gray-600 mb-4">
            Creates random loan records for existing books and users.
          </p>

          <div className="flex items-end gap-4">
            <div className="flex-1">
              <Input
                label="Number of Loans"
                type="number"
                min="1"
                max="1000"
                value={numLoans}
                onChange={(e) => setNumLoans(parseInt(e.target.value) || 0)}
                data-test="num-loans-input"
              />
            </div>
            <Button
              variant="primary"
              onClick={handleGenerateLoans}
              isLoading={generateLoans.isPending}
              data-test="generate-loans"
            >
              Generate Loans
            </Button>
          </div>
        </div>

        {/* Delete Operations */}
        <div className="bg-white rounded-lg shadow p-6 border-2 border-red-200">
          <h2 className="text-xl font-semibold text-red-900 mb-4">Danger Zone</h2>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">Delete All Test Data</p>
                <p className="text-xs text-gray-500">Removes all generated test books, authors, and loans</p>
              </div>
              <Button
                variant="danger"
                onClick={() => setShowDeleteConfirm(true)}
                data-test="delete-all-test-data"
              >
                Delete All
              </Button>
            </div>

            <div className="border-t border-gray-200 pt-4 flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">Total Database Purge</p>
                <p className="text-xs text-gray-500">
                  Deletes ALL data including users and libraries. Cannot be undone!
                </p>
              </div>
              <Button
                variant="danger"
                onClick={() => setShowPurgeConfirm(true)}
                data-test="total-purge"
              >
                Total Purge
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDeleteAll}
        title="Delete All Test Data"
        message="Are you sure you want to delete all test data? This will remove all generated books, authors, and loans. This action cannot be undone."
        confirmText="Delete All"
        variant="danger"
        isLoading={deleteAll.isPending}
      />

      {/* Purge Confirmation Dialog */}
      <ConfirmDialog
        isOpen={showPurgeConfirm}
        onClose={() => setShowPurgeConfirm(false)}
        onConfirm={handleTotalPurge}
        title="Total Database Purge"
        message="⚠️ WARNING: This will delete ALL data from the database including users, libraries, books, authors, and loans. This action cannot be undone. Are you absolutely sure?"
        confirmText="I understand, purge everything"
        variant="danger"
        isLoading={totalPurge.isPending}
      />
    </div>
  )
}
