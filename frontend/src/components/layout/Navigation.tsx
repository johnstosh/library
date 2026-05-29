// (c) Copyright 2025 by Muczynski
import { useEffect } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuthStore, useIsLibrarian } from '@/stores/authStore'
import { clsx } from 'clsx'
import { useBranches } from '@/api/branches'
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel,
  Menu,
  MenuButton,
  MenuItems,
  MenuItem,
} from '@headlessui/react'

function getInitials(username: string | null | undefined): string {
  const name = (username ?? '').trim()
  if (!name) return '?'
  const parts = name.split(/\s+/).filter(Boolean)
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase()
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
}

function desktopNavCls({ isActive }: { isActive: boolean }): string {
  return clsx(
    'px-3 py-1.5 rounded-md text-sm font-medium transition-colors',
    isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900',
  )
}

function mobileNavCls({ isActive }: { isActive: boolean }): string {
  return clsx(
    'block px-3 py-2.5 rounded-md text-sm font-medium transition-colors',
    isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900',
  )
}

export function Navigation() {
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const isLibrarian = useIsLibrarian()
  const isAuthenticated = !!user
  const { data: branches = [] } = useBranches()

  const branchName = branches.length > 0 ? branches[0].branchName : 'Branch'
  const librarySystemName = branches.length > 0 ? branches[0].librarySystemName : 'Library System'

  useEffect(() => {
    if (branches.length > 0) {
      document.title = `The ${branchName} Branch of the ${librarySystemName}`
    } else {
      document.title = 'Library'
    }
  }, [branches, branchName, librarySystemName])

  const handleLogout = () => {
    logout()
    window.location.href = '/login'
  }

  return (
    <Disclosure as="nav" className="bg-white shadow-sm border-b border-gray-200" data-test="navigation">
      {({ open, close }) => (
        <>
          <div className="mx-[2%]">
            <div className="flex items-center h-16 gap-4">

              {/* App name — left */}
              <Link to="/" className="flex flex-col items-start shrink-0" data-test="branch-name">
                <span className="text-base font-bold text-gray-900 leading-tight">
                  The {branchName} Branch
                </span>
                <span className="text-xs text-gray-600 leading-tight">
                  of the {librarySystemName}
                </span>
              </Link>

              {/* Desktop nav — hidden on mobile */}
              <nav className="hidden md:flex items-center gap-1 ml-2" aria-label="Primary">
                <NavLink to="/search" className={desktopNavCls} data-test="nav-search">Search</NavLink>
                {isAuthenticated && (
                  <>
                    <NavLink to="/books"    className={desktopNavCls} data-test="nav-books">Books</NavLink>
                    <NavLink to="/authors"  className={desktopNavCls} data-test="nav-authors">Authors</NavLink>
                    <NavLink to="/loans"    className={desktopNavCls} data-test="nav-loans">Loans</NavLink>
                    <NavLink to="/my-card"  className={desktopNavCls} data-test="nav-my-card">My Card</NavLink>
                    <NavLink to="/settings" className={desktopNavCls} data-test="nav-settings">Settings</NavLink>
                  </>
                )}
                {isLibrarian && (
                  <>
                    <div className="w-px h-6 bg-gray-300 mx-1" />
                    <NavLink to="/branches"        className={desktopNavCls} data-test="nav-branches">Branches</NavLink>
                    <NavLink to="/users"           className={desktopNavCls} data-test="nav-users">Users</NavLink>
                    <NavLink to="/applications"    className={desktopNavCls} data-test="nav-applications">Applications</NavLink>
                    <NavLink to="/data-management" className={desktopNavCls} data-test="nav-data">Data</NavLink>
                    <NavLink to="/photos-management" className={desktopNavCls} data-test="nav-photos">Photos</NavLink>
                    <NavLink to="/global-settings" className={desktopNavCls} data-test="nav-global-settings">Global Settings</NavLink>
                  </>
                )}
              </nav>

              {/* Right side: hamburger (mobile) + user avatar */}
              <div className="ml-auto flex items-center gap-2">

                {/* Hamburger — mobile only */}
                <DisclosureButton
                  className="md:hidden flex items-center justify-center w-9 h-9 rounded-md text-gray-600 hover:bg-gray-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
                  aria-label={open ? 'Close menu' : 'Open menu'}
                  data-test="mobile-menu-button"
                >
                  <span className="text-xl leading-none">{open ? '✕' : '☰'}</span>
                </DisclosureButton>

                {/* User avatar menu */}
                {isAuthenticated ? (
                  <Menu as="div" className="relative">
                    <MenuButton
                      aria-label="User menu"
                      data-test="nav-user-menu"
                      className="flex items-center justify-center w-8 h-8 rounded-full bg-blue-600 text-white text-xs font-semibold hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
                    >
                      {getInitials(user?.username)}
                    </MenuButton>
                    <MenuItems
                      anchor="bottom end"
                      className="absolute right-0 mt-2 w-60 rounded-md shadow-lg bg-white ring-1 ring-black/5 z-50 focus:outline-none"
                    >
                      <div className="px-4 py-3 border-b border-gray-100">
                        <p className="text-sm font-semibold text-gray-900 truncate" data-test="nav-username">
                          {user?.username}
                        </p>
                        <div className="mt-1 flex items-center gap-1.5 flex-wrap">
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
                      <div className="py-1">
                        <MenuItem>
                          {({ focus }) => (
                            <button
                              type="button"
                              onClick={handleLogout}
                              data-test="nav-logout"
                              className={clsx(
                                'w-full text-left px-4 py-2 text-sm',
                                focus ? 'bg-gray-100 text-gray-900' : 'text-gray-700',
                              )}
                            >
                              Logout
                            </button>
                          )}
                        </MenuItem>
                      </div>
                    </MenuItems>
                  </Menu>
                ) : (
                  <Link
                    to="/login"
                    className="text-sm text-gray-700 hover:underline"
                    data-test="nav-login"
                  >
                    Login
                  </Link>
                )}
              </div>
            </div>
          </div>

          {/* Mobile nav panel */}
          <DisclosurePanel className="md:hidden border-t border-gray-200 bg-white">
            <nav className="mx-[2%] py-2 space-y-1" aria-label="Primary mobile">
              <NavLink to="/search" className={mobileNavCls} data-test="nav-search-mobile" onClick={() => close()}>Search</NavLink>
              {isAuthenticated && (
                <>
                  <NavLink to="/books"    className={mobileNavCls} data-test="nav-books-mobile"    onClick={() => close()}>Books</NavLink>
                  <NavLink to="/authors"  className={mobileNavCls} data-test="nav-authors-mobile"  onClick={() => close()}>Authors</NavLink>
                  <NavLink to="/loans"    className={mobileNavCls} data-test="nav-loans-mobile"    onClick={() => close()}>Loans</NavLink>
                  <NavLink to="/my-card"  className={mobileNavCls} data-test="nav-my-card-mobile"  onClick={() => close()}>My Card</NavLink>
                  <NavLink to="/settings" className={mobileNavCls} data-test="nav-settings-mobile" onClick={() => close()}>Settings</NavLink>
                </>
              )}
              {!isAuthenticated && (
                <NavLink to="/login" className={mobileNavCls} data-test="nav-login-mobile" onClick={() => close()}>Login</NavLink>
              )}
              {isLibrarian && (
                <>
                  <div className="border-t border-gray-200 my-1" />
                  <NavLink to="/branches"          className={mobileNavCls} data-test="nav-branches-mobile"      onClick={() => close()}>Branches</NavLink>
                  <NavLink to="/users"             className={mobileNavCls} data-test="nav-users-mobile"         onClick={() => close()}>Users</NavLink>
                  <NavLink to="/applications"      className={mobileNavCls} data-test="nav-applications-mobile"  onClick={() => close()}>Applications</NavLink>
                  <NavLink to="/data-management"   className={mobileNavCls} data-test="nav-data-mobile"          onClick={() => close()}>Data Management</NavLink>
                  <NavLink to="/photos-management" className={mobileNavCls} data-test="nav-photos-mobile"        onClick={() => close()}>Photos</NavLink>
                  <NavLink to="/global-settings"   className={mobileNavCls} data-test="nav-global-settings-mobile" onClick={() => close()}>Global Settings</NavLink>
                </>
              )}
            </nav>
          </DisclosurePanel>
        </>
      )}
    </Disclosure>
  )
}
