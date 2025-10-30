// (c) Copyright 2025 by Muczynski
console.log('app.js loaded');

let isLibrarian = false;

function getCookie(name) {
    let cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

async function fetchData(url) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'GET',
            headers: headers
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error status: ${response.status}`);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('Fetch error:', error);
        throw error;
    }
}

async function postData(url, data, isFormData = false, includeCsrf = true) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {};
    if (!isFormData) {
        headers['Content-Type'] = 'application/json';
    }
    if (includeCsrf && token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: headers,
            body: isFormData ? data : JSON.stringify(data)
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error status: ${response.status}`);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('Post error:', error);
        throw error;
    }
}

async function putData(url, data, includeCsrf = true) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (includeCsrf && token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify(data)
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error status: ${response.status}`);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('Put error:', error);
        throw error;
    }
}

async function deleteData(url) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {};
    if (token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'DELETE',
            headers: headers
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error status: ${response.status}`);
        }

        return response.ok;
    } catch (error) {
        console.error('Delete error:', error);
        throw error;
    }
}

function showError(sectionId, message) {
    let errorDiv = document.querySelector(`#${sectionId}-section [data-test="form-error"]`);
    if (!errorDiv) {
        errorDiv = document.createElement('div');
        errorDiv.setAttribute('data-test', 'form-error');
        errorDiv.style.color = 'red';
        errorDiv.style.display = 'block';
        const section = document.getElementById(`${sectionId}-section`);
        if (section) {
            section.insertBefore(errorDiv, section.firstChild.nextSibling); // After h2
        }
    }
    errorDiv.textContent = message;
    if (['authors', 'books', 'users'].includes(sectionId)) {
        window.scrollTo(0, 0);
    }
}

function clearError(sectionId) {
    const errorDiv = document.querySelector(`#${sectionId}-section [data-test="form-error"]`);
    if (errorDiv) {
        errorDiv.remove();
    }
}

function showBulkSuccess(textareaId) {
    let successDiv = document.querySelector(`#${textareaId} + [data-test="bulk-import-success"]`);
    if (!successDiv) {
        successDiv = document.createElement('div');
        successDiv.setAttribute('data-test', 'bulk-import-success');
        successDiv.style.color = 'green';
        successDiv.textContent = 'Bulk import successful.';
        successDiv.style.display = 'block';
    }
    const textarea = document.getElementById(textareaId);
    if (textarea) {
        textarea.insertAdjacentElement('afterend', successDiv);
    }
    setTimeout(() => {
        if (successDiv) successDiv.remove();
    }, 3000);
}

function formatDate(dateString) {
    if (!dateString) return '';

    // The dateString from the backend is 'YYYY-MM-DD'.
    // `new Date('YYYY-MM-DD')` creates a date at midnight UTC.
    // In timezones behind UTC, this can result in the previous day.
    // To fix this, we parse the string and create the date in the local timezone.
    const parts = dateString.split('T')[0].split('-');
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed in JS
    const day = parseInt(parts[2], 10);
    const date = new Date(year, month, day);

    const displayMonth = String(date.getMonth() + 1).padStart(2, '0');
    const displayDay = String(date.getDate()).padStart(2, '0');
    const displayYear = date.getFullYear();
    return `${displayMonth}/${displayDay}/${displayYear}`;
}

document.addEventListener('DOMContentLoaded', () => {
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
        showPublicSearchPage();
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
});

async function checkAuthentication() {
    console.log('Checking authentication...');
    try {
        const response = await fetch('/api/users/me', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (response.status === 401) {
            console.log('Not authenticated');
            showPublicSearchPage();
            return;
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const user = await response.json();
            showMainContent(user.roles);
        } else {
            throw new Error('Response is not JSON');
        }
    } catch (error) {
        console.log('Authentication check failed:', error);
        showPublicSearchPage();
    }
}

function showPublicSearchPage() {
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

function showLoginForm() {
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

function showLoginError() {
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

function showMainContent(roles) {
    console.log('Showing main content for roles:', roles);
    isLibrarian = roles.includes('LIBRARIAN');
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

    if (isLibrarian) {
        document.body.classList.add('user-is-librarian');
        showSection('loans');
        loadLibraries();
        populateBookDropdowns();
        populateLoanDropdowns();
    } else {
        document.body.classList.remove('user-is-librarian');
        // Hide librarian-only sections and menu items
        document.querySelectorAll('.librarian-only').forEach(item => {
            item.style.display = 'none';
        });
        showSection('search');
    }
}

const sectionConfig = {
    'libraries': { load: loadLibraries, reset: null },
    'authors': { load: loadAuthors, reset: resetAuthorForm },
    'books': { load: loadBooks, reset: resetBookForm },
    'search': { load: null, reset: null },
    'library-card': { load: loadApplied, reset: null },
    'users': { load: loadUsers, reset: null },
    'loans': { load: loadLoans, reset: null },
    'test-data': { load: loadTestDataStats, reset: null },
    'settings': { load: loadSettings, reset: null }
};

function showSection(sectionId, event) {
    const currentActiveButton = document.querySelector('#section-menu button.active');
    const newActiveButton = event ? event.target.closest('button') : document.querySelector(`#section-menu button[onclick*="'${sectionId}'"]`);

    // Reset forms when navigating away
    if (currentActiveButton) {
        const currentSectionId = currentActiveButton.getAttribute('onclick').match(/'([^']+)'/)[1];
        if (currentSectionId !== sectionId) {
            const config = sectionConfig[currentSectionId];
            if (config && config.reset) {
                config.reset();
            }
        }
    }

    // If not logged in and showing a section, hide login and show main content
    if (!isLibrarian && (document.getElementById('login-form')?.style.display === 'block')) {
        const loginForm = document.getElementById('login-form');
        if (loginForm) loginForm.style.display = 'none';
        const mainContent = document.getElementById('main-content');
        if (mainContent) mainContent.style.display = 'block';
    }

    // Hide all sections and show the target one
    document.querySelectorAll('.section').forEach(section => {
        section.style.setProperty('display', 'none', 'important');
    });
    const targetSection = document.getElementById(sectionId + '-section');
    if (targetSection) {
        targetSection.style.setProperty('display', 'block', 'important');
    }

    // Load data and reset form for the section
    const config = sectionConfig[sectionId];
    if (config) {
        if (currentActiveButton === newActiveButton && config.reset) {
            config.reset();
        }
        if (config.load) {
            config.load();
        }
    }


    // Update active button
    document.querySelectorAll('#section-menu button').forEach(btn => {
        btn.classList.remove('active');
    });
    if (newActiveButton) {
        newActiveButton.classList.add('active');
    }

    // Hide the navbar on mobile after a menu item is clicked
    const navbarCollapse = document.getElementById('navbarNav');
    if (navbarCollapse && navbarCollapse.classList.contains('show')) {
        const bsCollapse = new bootstrap.Collapse(navbarCollapse, {
            toggle: false
        });
        bsCollapse.hide();
    }
}

async function logout() {
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
