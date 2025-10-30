// (c) Copyright 2025 by Muczynski - Initialization Module

import { checkAuthentication, showLoginError } from './auth.js';
import { showSection } from './sections.js';
import { showBulkSuccess } from './utils.js';

export function initApp() {
    console.log('DOM loaded');
    // Add submit event listener to login form for debugging
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        const form = loginForm.querySelector('form');
        if (form) {
            form.addEventListener('submit', (e) => {
                console.log('Login form submitted with username:', document.getElementById('username')?.value);
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
