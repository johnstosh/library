// (c) Copyright 2025 by Muczynski
import { Link, useLocation } from 'react-router-dom'
import { useAuthStore, useIsLibrarian } from '@/stores/authStore'
import { Button } from '@/components/ui/Button'
import { clsx } from 'clsx'

interface NavLinkProps {
  to: string
  children: React.ReactNode
  'data-test'?: string
}

function NavLink({ to, children, 'data-test': dataTest }: NavLinkProps) {
  const location = useLocation()
  const isActive = location.pathname.startsWith(to)

  return (
    <Link
      to={to}
      className={clsx(
        'px-3 py-2 rounded-md text-sm font-medium transition-colors',
        isActive
          ? 'bg-blue-100 text-blue-700'
          : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
      )}
      data-test={dataTest}
    >
      {children}
    </Link>
  )
}

export function Navigation() {
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const isLibrarian = useIsLibrarian()

  const handleLogout = () => {
    logout()
    window.location.href = '/login'
  }

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Left side - Main navigation */}
          <div className="flex items-center space-x-4">
            <Link to="/" className="flex items-center">
              <span className="text-xl font-bold text-gray-900">Library</span>
            </Link>

            <div className="hidden md:flex items-center space-x-2">
              {/* User navigation items */}
              <NavLink to="/books" data-test="nav-books">
                Books
              </NavLink>
              <NavLink to="/authors" data-test="nav-authors">
                Authors
              </NavLink>
              <NavLink to="/search" data-test="nav-search">
                Search
              </NavLink>
              <NavLink to="/loans" data-test="nav-loans">
                Loans
              </NavLink>
              <NavLink to="/my-card" data-test="nav-my-card">
                My Card
              </NavLink>

              {/* Librarian-only items */}
              {isLibrarian && (
                <>
                  <div className="w-px h-6 bg-gray-300 mx-2" />
                  <NavLink to="/libraries" data-test="nav-libraries">
                    Libraries
                  </NavLink>
                  <NavLink to="/users" data-test="nav-users">
                    Users
                  </NavLink>
                  <NavLink to="/applications" data-test="nav-applications">
                    Applications
                  </NavLink>
                  <NavLink to="/labels" data-test="nav-labels">
                    Labels
                  </NavLink>
                  <NavLink to="/data-management" data-test="nav-data">
                    Data
                  </NavLink>
                </>
              )}
            </div>
          </div>

          {/* Right side - User menu */}
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-700">
                {user?.username}
                {user?.ssoSubjectId && (
                  <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                    SSO
                  </span>
                )}
              </span>
              {isLibrarian && (
                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                  Librarian
                </span>
              )}
            </div>

            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              data-test="nav-logout"
            >
              Logout
            </Button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      <div className="md:hidden border-t border-gray-200">
        <div className="px-2 pt-2 pb-3 space-y-1">
          <NavLink to="/books" data-test="nav-books-mobile">
            Books
          </NavLink>
          <NavLink to="/authors" data-test="nav-authors-mobile">
            Authors
          </NavLink>
          <NavLink to="/search" data-test="nav-search-mobile">
            Search
          </NavLink>
          <NavLink to="/loans" data-test="nav-loans-mobile">
            Loans
          </NavLink>
          <NavLink to="/my-card" data-test="nav-my-card-mobile">
            My Card
          </NavLink>

          {isLibrarian && (
            <>
              <div className="border-t border-gray-200 my-2" />
              <NavLink to="/libraries" data-test="nav-libraries-mobile">
                Libraries
              </NavLink>
              <NavLink to="/users" data-test="nav-users-mobile">
                Users
              </NavLink>
              <NavLink to="/applications" data-test="nav-applications-mobile">
                Applications
              </NavLink>
              <NavLink to="/labels" data-test="nav-labels-mobile">
                Labels
              </NavLink>
              <NavLink to="/data-management" data-test="nav-data-mobile">
                Data Management
              </NavLink>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
