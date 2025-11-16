// (c) Copyright 2025 by Muczynski - Initialization Module

import { checkAuthentication, showLoginError } from './auth.js';
import { showSection } from './sections.js';
import { showBulkSuccess, hashPassword } from './utils.js';

export function initApp() {
    console.log('DOM loaded');
    // Add submit event listener to login form to hash password before submission
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        const form = loginForm.querySelector('form');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault(); // Prevent default form submission
                console.log('Login form submitted with username:', document.getElementById('username')?.value);

                const usernameInput = document.getElementById('username');
                const passwordInput = document.getElementById('password');

                if (!usernameInput || !passwordInput) {
                    console.error('Username or password input not found');
                    return;
                }

                const username = usernameInput.value;
                const password = passwordInput.value;

                if (!username || !password) {
                    console.error('Username or password is empty');
                    return;
                }

                // Hash password with SHA-256 before sending
                const hashedPassword = await hashPassword(password);

                // Create form data with hashed password
                const formData = new FormData();
                formData.append('username', username);
                formData.append('password', hashedPassword);

                // Submit to login endpoint
                // Note: Custom success handler returns 200 OK with JSON (no redirect)
                // Failure still redirects to /?error
                try {
                    const response = await fetch('/login', {
                        method: 'POST',
                        body: formData,
                        credentials: 'same-origin' // Ensure cookies are sent/received
                    });

                    if (response.ok && !response.redirected) {
                        // Login successful - got 200 OK with JSON
                        console.log('Login successful');
                        // Reload page to check authentication and show appropriate content
                        window.location.href = '/';
                    } else if (response.redirected && response.url.includes('error')) {
                        // Login failed - Spring Security redirected to /?error
                        console.log('Login failed, redirected to error page');
                        showLoginError();
                    } else {
                        // Unexpected response
                        console.log('Unexpected login response:', response.status, response.redirected);
                        showLoginError();
                    }
                } catch (error) {
                    console.error('Login error:', error);
                    showLoginError();
                }
            });
        }
    }

    const searchInput = document.querySelector('[data-test="search-input"]');
    if (searchInput) {
        searchInput.addEventListener('keyup', function(event) {
            if (event.key === 'Enter') {
                performSearch();
            }
        });
    }

    try {
        checkAuthentication();
    } catch (error) {
        console.error('Error in initial auth check, showing search page.', error);
        showSection('search');
    }
    // Check for login error in URL params
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        console.log('Login error detected in URL params');
        console.log('Error param value:', urlParams.get('error'));
        showLoginError();
        // Clear the error param from URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }
}
