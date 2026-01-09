// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { useApplyForCard } from '@/api/library-cards'
import { hashPassword } from '@/utils/auth'
import { PiIdentificationCard } from 'react-icons/pi'

export function ApplyForCardPage() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const applyForCard = useApplyForCard()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    console.log('=== APPLY FOR CARD FORM SUBMITTED ===')

    // Validation
    if (!formData.username.trim()) {
      console.log('Validation failed: Name is required')
      setError('Name is required')
      return
    }

    if (!formData.password) {
      console.log('Validation failed: Password is required')
      setError('Password is required')
      return
    }

    if (formData.password.length < 6) {
      console.log('Validation failed: Password too short')
      setError('Password must be at least 6 characters')
      return
    }

    if (formData.password !== formData.confirmPassword) {
      console.log('Validation failed: Passwords do not match')
      setError('Passwords do not match')
      return
    }

    try {
      console.log('Validation passed, hashing password...')
      // Hash password client-side
      const hashedPassword = await hashPassword(formData.password)

      console.log('Password hashed, submitting to API...')
      console.log('Request data:', {
        username: formData.username.trim(),
        authority: 'USER',
      })

      await applyForCard.mutateAsync({
        username: formData.username.trim(),
        password: hashedPassword,
        authority: 'USER',
      })

      console.log('Application submitted successfully!')
      setSuccess(true)
      setFormData({ username: '', password: '', confirmPassword: '' })
    } catch (err) {
      console.error('Application submission failed:', err)
      setError(err instanceof Error ? err.message : 'Failed to submit application')
    }
  }

  const isSubmitting = applyForCard.isPending

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <PiIdentificationCard className="w-16 h-16 text-blue-600" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Apply for a Library Card
          </h1>
          <p className="text-gray-600">
            Fill out the form below to request a library card
          </p>
        </div>

        {success ? (
          <div className="bg-white rounded-lg shadow p-6" data-test="success-container">
            <SuccessMessage message="Application submitted successfully!" data-test="success-message" />
            <p className="mt-4 text-gray-700">
              A librarian will review your application. You will be notified when your
              card is approved.
            </p>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow p-6">
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              {error && <ErrorMessage message={error} data-test="error-message" />}

              <Input
                label="Full Name"
                type="text"
                value={formData.username}
                onChange={(e) =>
                  setFormData({ ...formData, username: e.target.value })
                }
                required
                placeholder="Enter your full name"
                data-test="apply-name"
                autoComplete="name"
              />

              <Input
                label="Password"
                type="password"
                value={formData.password}
                onChange={(e) =>
                  setFormData({ ...formData, password: e.target.value })
                }
                required
                placeholder="Choose a password (min 6 characters)"
                data-test="apply-password"
                helpText="Minimum 6 characters"
                autoComplete="new-password"
              />

              <Input
                label="Confirm Password"
                type="password"
                value={formData.confirmPassword}
                onChange={(e) =>
                  setFormData({ ...formData, confirmPassword: e.target.value })
                }
                required
                placeholder="Confirm your password"
                data-test="apply-confirm-password"
                autoComplete="new-password"
              />

              <div className="pt-4">
                <Button
                  type="submit"
                  variant="primary"
                  fullWidth
                  size="lg"
                  isLoading={isSubmitting}
                  data-test="apply-submit"
                >
                  Submit Application
                </Button>
              </div>
            </form>

            <div className="mt-6 text-center">
              <p className="text-sm text-gray-600">
                Already have an account?{' '}
                <a
                  href="/login"
                  className="text-blue-600 hover:text-blue-700 font-medium"
                >
                  Sign in
                </a>
              </p>
            </div>
          </div>
        )}

        <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h3 className="text-sm font-medium text-blue-900 mb-2">What happens next?</h3>
          <ol className="text-sm text-blue-800 space-y-1 list-decimal list-inside">
            <li>A librarian will review your application</li>
            <li>You'll receive an email when approved</li>
            <li>Log in to access your library card</li>
            <li>Start borrowing books!</li>
          </ol>
        </div>
      </div>
    </div>
  )
}
