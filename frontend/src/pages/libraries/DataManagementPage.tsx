// (c) Copyright 2025 by Muczynski
import { useState, useRef } from 'react'
import { Button } from '@/components/ui/Button'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { exportJsonData, exportPhotos, useImportJsonData } from '@/api/data-management'
import { useLibraries } from '@/api/libraries'
import { useBooks } from '@/api/books'
import { useAuthors } from '@/api/authors'
import { useUsers } from '@/api/users'
import { useLoans } from '@/api/loans'
import {
  PiDownload,
  PiUpload,
  PiDatabase,
  PiImage,
  PiFileArrowDown,
  PiFileArrowUp,
} from 'react-icons/pi'

export function DataManagementPage() {
  const [isExportingJson, setIsExportingJson] = useState(false)
  const [isExportingPhotos, setIsExportingPhotos] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const fileInputRef = useRef<HTMLInputElement>(null)
  const importJsonData = useImportJsonData()

  // Fetch data for statistics
  const { data: libraries = [] } = useLibraries()
  const { data: books = [] } = useBooks()
  const { data: authors = [] } = useAuthors()
  const { data: users = [] } = useUsers()
  const { data: loans = [] } = useLoans()

  const handleExportJson = async () => {
    setIsExportingJson(true)
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const blob = await exportJsonData()

      // Generate filename with statistics (like main branch)
      let libraryName = 'library'
      if (libraries.length > 0) {
        // Sanitize library name for use as filename
        libraryName = libraries[0].name
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-+|-+$/g, '')
      }

      const bookCount = books.length
      const authorCount = authors.length
      const userCount = users.length
      const loanCount = loans.length
      const date = new Date().toISOString().split('T')[0]

      const filename = `${libraryName}-${bookCount}-books-${authorCount}-authors-${userCount}-users-${loanCount}-loans-${date}.json`

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

  const handleExportPhotos = async () => {
    setIsExportingPhotos(true)
    setSuccessMessage('')
    setErrorMessage('')

    try {
      const blob = await exportPhotos()

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `library-photos-${new Date().toISOString().split('T')[0]}.zip`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)

      setSuccessMessage('Photo export downloaded successfully')
    } catch (error) {
      console.error('Failed to export photos:', error)
      setErrorMessage('Failed to export photos. Please try again.')
    } finally {
      setIsExportingPhotos(false)
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
      setErrorMessage('Failed to import JSON data. Please check the file format and try again.')
    }
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Data Management</h1>
        <p className="text-gray-600">
          Export and import library data for backup or migration
        </p>
      </div>

      {/* Success/Error Messages */}
      {successMessage && (
        <div className="mb-6">
          <SuccessMessage message={successMessage} />
        </div>
      )}
      {errorMessage && (
        <div className="mb-6">
          <ErrorMessage message={errorMessage} />
        </div>
      )}

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
            <div data-test="stat-libraries">
              <div className="text-2xl font-bold text-gray-900">{libraries.length}</div>
              <div className="text-sm text-gray-600">Libraries</div>
            </div>
            <div data-test="stat-books">
              <div className="text-2xl font-bold text-gray-900">{books.length}</div>
              <div className="text-sm text-gray-600">Books</div>
            </div>
            <div data-test="stat-authors">
              <div className="text-2xl font-bold text-gray-900">{authors.length}</div>
              <div className="text-sm text-gray-600">Authors</div>
            </div>
            <div data-test="stat-users">
              <div className="text-2xl font-bold text-gray-900">{users.length}</div>
              <div className="text-sm text-gray-600">Users</div>
            </div>
            <div data-test="stat-loans">
              <div className="text-2xl font-bold text-gray-900">{loans.length}</div>
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

      {/* Photo Export Section */}
      <div className="bg-white rounded-lg shadow overflow-hidden mb-6">
        <div className="bg-purple-600 px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            <PiImage className="w-8 h-8" />
            <div>
              <h2 className="text-xl font-bold">Photo Export</h2>
              <p className="text-sm text-purple-100">
                Download all book and author photos as a ZIP file
              </p>
            </div>
          </div>
        </div>

        <div className="p-6">
          <div className="flex items-start gap-3">
            <PiFileArrowDown className="w-6 h-6 text-purple-600 mt-1" />
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Export Photos
              </h3>
              <p className="text-sm text-gray-600 mb-4">
                Download all photos stored in Google Photos as a ZIP archive. This is
                separate from the JSON export and can be used for complete backup.
              </p>
              <Button
                variant="primary"
                onClick={handleExportPhotos}
                isLoading={isExportingPhotos}
                leftIcon={<PiDownload />}
                data-test="export-photos"
              >
                Export Photos
              </Button>
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
            <strong>Photo Export downloads from Google Photos</strong> - This may take
            some time depending on the number of photos
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
