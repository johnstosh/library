// (c) Copyright 2025 by Muczynski - Authentication Module

// Inactivity timeout configuration
const INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
let inactivityTimer = null;
let activityListenersActive = false;

/**
 * Handles inactivity timeout by logging out the user and redirecting to login
 * Does not timeout if any buttons are currently processing (have spinners)
 */
function handleInactivityTimeout() {
    // Check if any buttons are currently processing
    const activeSpinners = document.querySelectorAll('.spinner-border');
    if (activeSpinners.length > 0) {
        console.log('[Auth] Inactivity timeout deferred - buttons are processing');
        // Reset timer to check again later
        resetInactivityTimer();
        return;
    }

    console.log('[Auth] Inactivity timeout reached, logging out user');
    stopInactivityTimeout();
    logout();
}

/**
 * Resets the inactivity timer when user activity is detected
 */
function resetInactivityTimer() {
    // Clear existing timer
    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
    }

    // Set new timer
    inactivityTimer = setTimeout(handleInactivityTimeout, INACTIVITY_TIMEOUT_MS);
}

/**
 * Initializes inactivity timeout tracking for authenticated users
 */
function initInactivityTimeout() {
    // Don't re-initialize if already active
    if (activityListenersActive) {
        return;
    }

    console.log('[Auth] Initializing inactivity timeout (5 minutes)');

    // List of events that indicate user activity
    const activityEvents = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];

    // Add event listeners for user activity
    activityEvents.forEach(eventName => {
        document.addEventListener(eventName, resetInactivityTimer, true);
    });

    activityListenersActive = true;

    // Start the initial timer
    resetInactivityTimer();
}

/**
 * Stops inactivity timeout tracking and clears all listeners
 */
function stopInactivityTimeout() {
    console.log('[Auth] Stopping inactivity timeout');

    // Clear the timer
    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
        inactivityTimer = null;
    }

    // Remove event listeners
    if (activityListenersActive) {
        const activityEvents = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
        activityEvents.forEach(eventName => {
            document.removeEventListener(eventName, resetInactivityTimer, true);
        });
        activityListenersActive = false;
    }
}

/**
 * Check SSO configuration and update the Google SSO button visibility
 */
async function checkAndUpdateSsoButton() {
    try {
        const response = await fetch('/api/global-settings/sso-status', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const status = await response.json();
            const ssoButton = document.querySelector('[data-test="google-sso-button"]');
            const ssoSeparator = ssoButton?.previousElementSibling;

            if (ssoButton) {
                if (status.ssoConfigured) {
                    ssoButton.style.display = 'block';
                    if (ssoSeparator && ssoSeparator.classList.contains('my-3')) {
                        ssoSeparator.style.display = 'block';
                    }
                } else {
                    ssoButton.style.display = 'none';
                    if (ssoSeparator && ssoSeparator.classList.contains('my-3')) {
                        ssoSeparator.style.display = 'none';
                    }
                }
            }
        } else {
            console.error('Failed to check SSO status:', response.status);
            // If we can't check, hide the button to be safe
            const ssoButton = document.querySelector('[data-test="google-sso-button"]');
            if (ssoButton) {
                ssoButton.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Error checking SSO status:', error);
        // If we can't check, hide the button to be safe
        const ssoButton = document.querySelector('[data-test="google-sso-button"]');
        if (ssoButton) {
            ssoButton.style.display = 'none';
        }
    }
}

/**
 * Check test data page visibility setting and update the Test Data menu item visibility
 */
async function checkAndUpdateTestDataPageVisibility() {
    try {
        const response = await fetch('/api/global-properties/test-data-page-visibility', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const visibility = await response.json();
            const testDataMenuItem = document.querySelector('[data-test="menu-test-data-item"]');

            if (testDataMenuItem) {
                if (visibility.showTestDataPage) {
                    testDataMenuItem.style.display = 'list-item';
                } else {
                    testDataMenuItem.style.display = 'none';
                }
            }
        } else {
            console.error('Failed to check test data page visibility:', response.status);
            // If we can't check, show the test data page by default for backward compatibility
            const testDataMenuItem = document.querySelector('[data-test="menu-test-data-item"]');
            if (testDataMenuItem) {
                testDataMenuItem.style.display = 'list-item';
            }
        }
    } catch (error) {
        console.error('Error checking test data page visibility:', error);
        // If we can't check, show the test data page by default for backward compatibility
        const testDataMenuItem = document.querySelector('[data-test="menu-test-data-item"]');
        if (testDataMenuItem) {
            testDataMenuItem.style.display = 'list-item';
        }
    }
}

async function checkAuthentication() {
    console.log('Checking authentication...');
    try {
        const user = await fetchData('/api/users/me', { suppress401Redirect: true });
        await showMainContent(user.roles);
    } catch (error) {
        console.log('Authentication check failed:', error);
        await showPublicSearchPage();
    }
}

async function showPublicSearchPage() {
    // Stop inactivity timeout for public pages
    stopInactivityTimeout();

    const loginForm = document.getElementById('login-form');
    if (loginForm) loginForm.style.display = 'none';
    const mainContent = document.getElementById('main-content');
    if (mainContent) mainContent.style.display = 'block';
    showSection('search');
    const loginMenuBtn = document.getElementById('login-menu-btn');
    if (loginMenuBtn) loginMenuBtn.style.display = 'block';
    const logoutMenuBtn = document.getElementById('logout-menu-btn');
    if (logoutMenuBtn) logoutMenuBtn.style.display = 'none';
    const pageTitle = document.getElementById('page-title');
    if (pageTitle) {
        pageTitle.innerHTML = 'The St. Martin de Porres Branch<br>of the Sacred Heart Library System';
    }

    // Show section menu but hide non-public items
    const sectionMenu = document.getElementById('section-menu');
    if (sectionMenu) {
        sectionMenu.style.display = 'flex';
        sectionMenu.querySelectorAll('li').forEach(item => {
            if (!item.classList.contains('public-item')) {
                item.style.display = 'none';
            } else {
                item.style.display = 'list-item';
            }
        });
    }

    // Check and update test data page visibility based on global settings
    await checkAndUpdateTestDataPageVisibility();
}

async function showLoginForm() {
    // Stop inactivity timeout when showing login form
    stopInactivityTimeout();

    const loginForm = document.getElementById('login-form');
    if (loginForm) loginForm.style.display = 'block';
    const mainContent = document.getElementById('main-content');
    if (mainContent) mainContent.style.display = 'none';
    const loginMenuBtn = document.getElementById('login-menu-btn');
    if (loginMenuBtn) loginMenuBtn.style.display = 'block';
    const logoutMenuBtn = document.getElementById('logout-menu-btn');
    if (logoutMenuBtn) logoutMenuBtn.style.display = 'none';
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }

    // Show section menu but hide non-public items
    const sectionMenu = document.getElementById('section-menu');
    if (sectionMenu) {
        sectionMenu.style.display = 'flex';
        sectionMenu.querySelectorAll('li').forEach(item => {
            if (!item.classList.contains('public-item')) {
                item.style.display = 'none';
            } else {
                item.style.display = 'list-item';
            }
        });
    }

    // Check SSO configuration and show/hide Google SSO button
    await checkAndUpdateSsoButton();

    // Check and update test data page visibility based on global settings
    await checkAndUpdateTestDataPageVisibility();
}

async function showLoginError() {
    console.log('Showing login error');

    // Show login form elements
    const loginForm = document.getElementById('login-form');
    if (loginForm) loginForm.style.display = 'block';
    const mainContent = document.getElementById('main-content');
    if (mainContent) mainContent.style.display = 'none';
    const loginMenuBtn = document.getElementById('login-menu-btn');
    if (loginMenuBtn) loginMenuBtn.style.display = 'block';
    const logoutMenuBtn = document.getElementById('logout-menu-btn');
    if (logoutMenuBtn) logoutMenuBtn.style.display = 'none';

    // Show section menu but hide non-public items
    const sectionMenu = document.getElementById('section-menu');
    if (sectionMenu) {
        sectionMenu.style.display = 'flex';
        sectionMenu.querySelectorAll('li').forEach(item => {
            if (!item.classList.contains('public-item')) {
                item.style.display = 'none';
            } else {
                item.style.display = 'list-item';
            }
        });
    }

    // Check SSO configuration and show/hide Google SSO button
    await checkAndUpdateSsoButton();

    // Check and update test data page visibility based on global settings
    await checkAndUpdateTestDataPageVisibility();

    // Show error message (don't hide it like showLoginForm does)
    const errorEl = document.getElementById('login-error');
    console.log('Login error element found:', !!errorEl);
    const message = 'Invalid username or password. Please try again.';
    console.log('Login error message to display:', message);
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    } else {
        console.log('Login error element not found, falling back to alert');
        alert('Login failed: Invalid username or password.');
    }
}

async function showMainContent(roles) {
    console.log('Showing main content for roles:', roles);
    window.isLibrarian = roles.includes('LIBRARIAN');
    const loginForm = document.getElementById('login-form');
    if (loginForm) loginForm.style.display = 'none';
    const mainContent = document.getElementById('main-content');
    if (mainContent) mainContent.style.display = 'block';
    const sectionMenu = document.getElementById('section-menu');
    if (sectionMenu) sectionMenu.style.display = 'flex';
    const loginMenuBtn = document.getElementById('login-menu-btn');
    if (loginMenuBtn) loginMenuBtn.style.display = 'none';
    const logoutMenuBtn = document.getElementById('logout-menu-btn');
    if (logoutMenuBtn) logoutMenuBtn.style.display = 'block';

    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }

    if (window.isLibrarian) {
        document.body.classList.add('user-is-librarian');
        showSection('loans');
        loadLibraries();
        populateBookDropdowns();
    } else {
        document.body.classList.remove('user-is-librarian');
        // Hide librarian-only sections and menu items
        document.querySelectorAll('.librarian-only').forEach(item => {
            item.style.display = 'none';
        });
        // Hide items that should only be visible to librarians or unauthenticated users (not regular users)
        document.querySelectorAll('.librarian-or-unauthenticated').forEach(item => {
            item.style.display = 'none';
        });
        // Regular users start with search section (they now have access to Authors, Books, and Loans too)
        showSection('search');
        // Populate book dropdowns for regular users too (they need to view books)
        populateBookDropdowns();
    }

    // Check and update test data page visibility based on global settings
    await checkAndUpdateTestDataPageVisibility();

    // Start inactivity timeout for authenticated users
    initInactivityTimeout();
}

async function logout() {
    // Stop inactivity timeout
    stopInactivityTimeout();

    document.body.classList.remove('user-is-librarian');
    try {
        const response = await fetch('/logout', { method: 'POST' });
        if (!response.ok) {
            console.error('Logout failed with status:', response.status);
        }
    } catch (error) {
        console.error('An error occurred during logout:', error);
    } finally {
        window.location.href = '/';
    }
}

// Expose all functions globally
window.checkAuthentication = checkAuthentication;
window.showPublicSearchPage = showPublicSearchPage;
window.showLoginForm = showLoginForm;
window.showLoginError = showLoginError;
window.showMainContent = showMainContent;
window.logout = logout;
