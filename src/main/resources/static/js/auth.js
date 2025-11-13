// (c) Copyright 2025 by Muczynski - Authentication Module

import { fetchData } from './utils.js';

export async function checkAuthentication() {
    console.log('Checking authentication...');
    try {
        const user = await fetchData('/api/users/me', { suppress401Redirect: true });
        showMainContent(user.roles);
    } catch (error) {
        console.log('Authentication check failed:', error);
        showPublicSearchPage();
    }
}

export function showPublicSearchPage() {
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
}

export function showLoginForm() {
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
}

export function showLoginError() {
    console.log('Showing login error');
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
    showLoginForm();
}

export function showMainContent(roles) {
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
        // Regular users start with books section (they now have access to Authors, Books, and Loans)
        showSection('books');
    }
}

export async function logout() {
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
