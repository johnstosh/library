// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { useAuthStore } from '@/stores/authStore'
import { printLibraryCard } from '@/api/library-cards'
import { PiIdentificationCard, PiFilePdf } from 'react-icons/pi'

export function MyLibraryCardPage() {
  const [isGenerating, setIsGenerating] = useState(false)
  const user = useAuthStore((state) => state.user)

  const handlePrintCard = async () => {
    setIsGenerating(true)
    try {
      const blob = await printLibraryCard()

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'library-card.pdf'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to generate library card PDF:', error)
      alert('Failed to generate library card PDF. Please try again.')
    } finally {
      setIsGenerating(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">My Library Card</h1>
        <p className="text-gray-600">
          View and print your personalized library card
        </p>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        {/* Card Preview Section */}
        <div className="bg-gradient-to-br from-blue-600 to-blue-800 p-8 text-white">
          <div className="flex items-center gap-4 mb-6">
            <PiIdentificationCard className="w-16 h-16" />
            <div>
              <h2 className="text-2xl font-bold">Library Card</h2>
              <p className="text-blue-100">Member Since {new Date().getFullYear()}</p>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-sm rounded-lg p-6">
            <div className="grid gap-4">
              <div>
                <p className="text-sm text-blue-100 mb-1">Cardholder Name</p>
                <p className="text-2xl font-bold">{user?.username}</p>
              </div>
              <div>
                <p className="text-sm text-blue-100 mb-1">Member ID</p>
                <p className="text-lg font-mono">{user?.id?.toString().padStart(8, '0')}</p>
              </div>
              <div>
                <p className="text-sm text-blue-100 mb-1">Status</p>
                <p className="text-lg">
                  <span className="inline-flex items-center px-3 py-1 rounded-full bg-green-500 text-white font-medium">
                    Active
                  </span>
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Actions Section */}
        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-1">
                Print Your Card
              </h3>
              <p className="text-sm text-gray-600">
                Download a wallet-sized PDF version of your library card
              </p>
            </div>
            <Button
              variant="primary"
              size="lg"
              onClick={handlePrintCard}
              isLoading={isGenerating}
              leftIcon={<PiFilePdf />}
              data-test="print-library-card"
            >
              Print Card
            </Button>
          </div>
        </div>
      </div>

      {/* Information Section */}
      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">Using Your Library Card:</h3>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li>Present your card when checking out books</li>
          <li>Keep your card in a safe place</li>
          <li>Report lost or stolen cards to a librarian immediately</li>
          <li>Your card is non-transferable</li>
        </ul>
      </div>
    </div>
  )
}
