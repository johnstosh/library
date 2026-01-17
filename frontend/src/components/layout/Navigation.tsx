// (c) Copyright 2025 by Muczynski
import { Link, useLocation } from 'react-router-dom'
import { useAuthStore, useIsLibrarian } from '@/stores/authStore'
import { Button } from '@/components/ui/Button'
import { clsx } from 'clsx'
import { useTestDataPageVisibility } from '@/api/global-properties'
import { useBranches } from '@/api/branches'

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
  const isAuthenticated = !!user
  const { data: testDataVisibility } = useTestDataPageVisibility()
  const { data: branches = [] } = useBranches()

  const handleLogout = () => {
    logout()
    window.location.href = '/login'
  }

  // Get branch name for display
  const branchName = branches.length > 0 ? branches[0].branchName : 'Branch'
  const librarySystemName = branches.length > 0 ? branches[0].librarySystemName : 'Library System'

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200" data-test="navigation">
      <div className="mx-[2%]">
        <div className="flex justify-between h-16">
          {/* Left side - Main navigation */}
          <div className="flex items-center space-x-4">
            <Link to="/" className="flex flex-col items-start" data-test="library-name">
              <span className="text-base font-bold text-gray-900 leading-tight">
                The {branchName} Branch
              </span>
              <span className="text-xs text-gray-600 leading-tight">
                of the {librarySystemName}
              </span>
            </Link>

            <div className="hidden md:flex items-center space-x-2">
              {/* Public navigation items (always visible) */}
              <NavLink to="/search" data-test="nav-search">
                Search
              </NavLink>

              {/* Authenticated user navigation items */}
              {isAuthenticated && (
                <>
                  <NavLink to="/books" data-test="nav-books">
                    Books
                  </NavLink>
                  <NavLink to="/authors" data-test="nav-authors">
                    Authors
                  </NavLink>
                  <NavLink to="/loans" data-test="nav-loans">
                    Loans
                  </NavLink>
                  <NavLink to="/settings" data-test="nav-settings">
                    Settings
                  </NavLink>
                  <NavLink to="/my-card" data-test="nav-my-card">
                    My Card
                  </NavLink>
                </>
              )}

              {/* Librarian-only items */}
              {isLibrarian && (
                <>
                  <div className="w-px h-6 bg-gray-300 mx-2" />
                  <NavLink to="/branches" data-test="nav-libraries">
                    Branches
                  </NavLink>
                  <NavLink to="/users" data-test="nav-users">
                    Users
                  </NavLink>
                  <NavLink to="/applications" data-test="nav-applications">
                    Applications
                  </NavLink>
                  <NavLink to="/data-management" data-test="nav-data">
                    Data
                  </NavLink>
                  <NavLink to="/global-settings" data-test="nav-global-settings">
                    Global Settings
                  </NavLink>
                  {testDataVisibility?.showTestDataPage && (
                    <NavLink to="/test-data" data-test="nav-test-data">
                      Test Data
                    </NavLink>
                  )}
                </>
              )}
            </div>
          </div>

          {/* Right side - User menu */}
          <div className="flex items-center">
            {isAuthenticated ? (
              <div className="flex flex-col items-end gap-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-700" data-test="nav-username">
                    {user?.username}
                  </span>
                  {user?.ssoSubjectId && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                      SSO
                    </span>
                  )}
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
            ) : (
              <Link to="/login">
                <Button variant="primary" size="sm" data-test="nav-login">
                  Login
                </Button>
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      <div className="md:hidden border-t border-gray-200">
        <div className="px-2 pt-2 pb-3 space-y-1">
          {/* Public navigation items (always visible) */}
          <NavLink to="/search" data-test="nav-search-mobile">
            Search
          </NavLink>

          {/* Authenticated user navigation items */}
          {isAuthenticated && (
            <>
              <NavLink to="/books" data-test="nav-books-mobile">
                Books
              </NavLink>
              <NavLink to="/authors" data-test="nav-authors-mobile">
                Authors
              </NavLink>
              <NavLink to="/loans" data-test="nav-loans-mobile">
                Loans
              </NavLink>
              <NavLink to="/settings" data-test="nav-settings-mobile">
                Settings
              </NavLink>
              <NavLink to="/my-card" data-test="nav-my-card-mobile">
                My Card
              </NavLink>
            </>
          )}

          {/* Login/Logout for mobile */}
          {!isAuthenticated && (
            <NavLink to="/login" data-test="nav-login-mobile">
              Login
            </NavLink>
          )}
          {isAuthenticated && (
            <div className="border-t border-gray-200 mt-2 pt-2">
              <div className="px-3 py-2">
                <div className="flex items-center gap-2 text-sm text-gray-700" data-test="nav-username-mobile">
                  <span>{user?.username}</span>
                  {user?.ssoSubjectId && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                      SSO
                    </span>
                  )}
                  {isLibrarian && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                      Librarian
                    </span>
                  )}
                </div>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleLogout}
                className="w-full justify-start px-3"
                data-test="nav-logout-mobile"
              >
                Logout
              </Button>
            </div>
          )}

          {isLibrarian && (
            <>
              <div className="border-t border-gray-200 my-2" />
              <NavLink to="/branches" data-test="nav-libraries-mobile">
                Branches
              </NavLink>
              <NavLink to="/users" data-test="nav-users-mobile">
                Users
              </NavLink>
              <NavLink to="/applications" data-test="nav-applications-mobile">
                Applications
              </NavLink>
              <NavLink to="/data-management" data-test="nav-data-mobile">
                Data Management
              </NavLink>
              <NavLink to="/global-settings" data-test="nav-global-settings-mobile">
                Global Settings
              </NavLink>
              {testDataVisibility?.showTestDataPage && (
                <NavLink to="/test-data" data-test="nav-test-data-mobile">
                  Test Data
                </NavLink>
              )}
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
