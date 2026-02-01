// (c) Copyright 2025 by Muczynski
import { useState, useRef } from 'react'
import { Button } from '@/components/ui/Button'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import {
  exportJsonData,
  useImportJsonData,
  useDatabaseStats,
  usePhotoExportStats,
} from '@/api/data-management'
import { useBranches } from '@/api/branches'
import {
  PiDownload,
  PiUpload,
  PiDatabase,
  PiFileArrowDown,
  PiFileArrowUp,
} from 'react-icons/pi'

export function DataManagementPage() {
  const [isExportingJson, setIsExportingJson] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const fileInputRef = useRef<HTMLInputElement>(null)
  const importJsonData = useImportJsonData()

  // Fetch database statistics (total counts from backend)
  const { data: dbStats } = useDatabaseStats()
  // Fetch libraries for filename generation (this is a small list)
  const { data: libraries = [] } = useBranches()

  // Photo stats for filename generation
  const { data: photoStats } = usePhotoExportStats()

  const handleExportJson = async () => {
    setIsExportingJson(true)
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const blob = await exportJsonData()

      // Generate filename with statistics from database
      let libraryName = 'library'
      if (libraries.length > 0) {
        // Sanitize library branch name for use as filename
        libraryName = libraries[0].branchName
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-+|-+$/g, '')
      }

      const bookCount = dbStats?.bookCount ?? 0
      const authorCount = dbStats?.authorCount ?? 0
      const userCount = dbStats?.userCount ?? 0
      const loanCount = dbStats?.loanCount ?? 0
      const photoCount = photoStats?.total ?? 0
      const date = new Date().toISOString().split('T')[0]

      const filename = `${libraryName}-${bookCount}-books-${authorCount}-authors-${userCount}-users-${loanCount}-loans-${photoCount}-photos-${date}.json`

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)

      setSuccessMessage('JSON export downloaded successfully')
    } catch (error) {
      console.error('Failed to export JSON:', error)
      setErrorMessage('Failed to export JSON data. Please try again.')
    } finally {
      setIsExportingJson(false)
    }
  }

  const handleImportClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setSuccessMessage('')
    setErrorMessage('')

    try {
      await importJsonData.mutateAsync(file)
      setSuccessMessage('JSON import completed successfully')

      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (error) {
      console.error('Failed to import JSON:', error)
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      setErrorMessage(`Failed to import JSON data: ${errorMessage}`)
    }
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Floating Status Messages - Fixed at top of viewport */}
      {(successMessage || errorMessage) && (
        <div className="fixed top-16 left-0 right-0 z-50 flex justify-center px-4">
          <div className="max-w-4xl w-full">
            {/* Success/Error Messages */}
            {successMessage && (
              <div className="mb-3">
                <SuccessMessage message={successMessage} className="shadow-lg" />
              </div>
            )}
            {errorMessage && (
              <div className="mb-3">
                <ErrorMessage message={errorMessage} className="shadow-lg" />
              </div>
            )}
          </div>
        </div>
      )}

      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Data Management</h1>
        <p className="text-gray-600">
          Export and import library data for backup or migration
        </p>
      </div>

      {/* JSON Export/Import Section */}
      <div className="bg-white rounded-lg shadow overflow-hidden mb-6">
        <div className="bg-blue-600 px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            <PiDatabase className="w-8 h-8" />
            <div>
              <h2 className="text-xl font-bold">Database Export/Import</h2>
              <p className="text-sm text-blue-100">
                Export all library data to JSON or import from a backup file
              </p>
            </div>
          </div>
        </div>

        {/* Database Statistics */}
        <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
          <div className="grid grid-cols-5 gap-4 text-center">
            <div data-test="stat-branches">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.branchCount ?? 0}</div>
              <div className="text-sm text-gray-600">Branches</div>
            </div>
            <div data-test="stat-books">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.bookCount ?? 0}</div>
              <div className="text-sm text-gray-600">Books</div>
            </div>
            <div data-test="stat-authors">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.authorCount ?? 0}</div>
              <div className="text-sm text-gray-600">Authors</div>
            </div>
            <div data-test="stat-users">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.userCount ?? 0}</div>
              <div className="text-sm text-gray-600">Users</div>
            </div>
            <div data-test="stat-loans">
              <div className="text-2xl font-bold text-gray-900">{dbStats?.loanCount ?? 0}</div>
              <div className="text-sm text-gray-600">Loans</div>
            </div>
          </div>
        </div>

        <div className="p-6 grid md:grid-cols-2 gap-6">
          {/* Export JSON */}
          <div className="border border-gray-200 rounded-lg p-6">
            <div className="flex items-start gap-3 mb-4">
              <PiFileArrowDown className="w-6 h-6 text-blue-600 mt-1" />
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Export Data
                </h3>
                <p className="text-sm text-gray-600 mb-4">
                  Download all library data as a JSON file. Includes libraries, authors,
                  books, users, and loans. Photos are excluded.
                </p>
                <Button
                  variant="primary"
                  onClick={handleExportJson}
                  isLoading={isExportingJson}
                  leftIcon={<PiDownload />}
                  data-test="export-json"
                >
                  Export JSON
                </Button>
              </div>
            </div>
          </div>

          {/* Import JSON */}
          <div className="border border-gray-200 rounded-lg p-6">
            <div className="flex items-start gap-3 mb-4">
              <PiFileArrowUp className="w-6 h-6 text-green-600 mt-1" />
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Import Data
                </h3>
                <p className="text-sm text-gray-600 mb-4">
                  Upload a JSON export file to import data. This will merge with existing
                  data (does not delete existing records).
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".json"
                  onChange={handleFileChange}
                  className="hidden"
                  data-test="import-json-input"
                />
                <Button
                  variant="secondary"
                  onClick={handleImportClick}
                  isLoading={importJsonData.isPending}
                  leftIcon={<PiUpload />}
                  data-test="import-json"
                >
                  Import JSON
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Important Notes */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
        <h3 className="text-sm font-medium text-yellow-900 mb-3">Important Notes:</h3>
        <ul className="text-sm text-yellow-800 space-y-2 list-disc list-inside">
          <li>
            <strong>JSON Export does NOT include photos</strong> - Use Photo Export
            separately for a complete backup
          </li>
          <li>
            <strong>Import merges data</strong> - It does not delete existing records,
            only adds or updates
          </li>
          <li>
            <strong>Regular backups recommended</strong> - Export data periodically to
            prevent data loss
          </li>
        </ul>
      </div>
    </div>
  )
}
