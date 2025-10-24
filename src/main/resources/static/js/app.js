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
    const date = new Date(dateString);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const year = date.getFullYear();
    return `${month}/${day}/${year}`;
}

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM loaded');
    // Add submit event listener to login form for debugging
    const loginForm = document.getElementById('login-form').querySelector('form');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            console.log('Login form submitted with username:', document.getElementById('username').value);
        });
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
        showLoginError();
        // Clear the error param from URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

async function checkAuthentication() {
    console.log('Checking authentication...');
    try {
        const response = await fetch('/api/users/me');
        console.log('Authentication check response status:', response.status);
        const contentType = response.headers.get('content-type');
        console.log('Response content-type:', contentType);
        if (response.ok && contentType && contentType.includes('application/json')) {
            const user = await response.json();
            console.log('User authenticated:', user);
            showMainContent(user.roles);
        } else {
            console.log('Authentication failed or non-JSON response, showing search page');
            showPublicSearchPage();
        }
    } catch (error) {
        console.error('Error during authentication check:', error);
        showPublicSearchPage();
    }
}

function showPublicSearchPage() {
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('main-content').style.display = 'block';
    showSection('search');
    document.getElementById('login-menu-btn').style.display = 'block';
    document.getElementById('logout-menu-btn').style.display = 'none';
    const pageTitle = document.getElementById('page-title');
    pageTitle.innerHTML = 'The St. Martin de Porres Branch<br>of the Sacred Heart Library System';

    // Show section menu but hide non-public items
    const sectionMenu = document.getElementById('section-menu');
    sectionMenu.style.display = 'flex';
    sectionMenu.querySelectorAll('li').forEach(item => {
        if (!item.classList.contains('public-item')) {
            item.style.display = 'none';
        } else {
            item.style.display = 'list-item';
        }
    });
}

function showLoginForm() {
    document.getElementById('login-form').style.display = 'block';
    document.getElementById('main-content').style.display = 'none';
    document.getElementById('login-menu-btn').style.display = 'block';
    document.getElementById('logout-menu-btn').style.display = 'none';
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }

    // Show section menu but hide non-public items
    const sectionMenu = document.getElementById('section-menu');
    sectionMenu.style.display = 'flex';
    sectionMenu.querySelectorAll('li').forEach(item => {
        if (!item.classList.contains('public-item')) {
            item.style.display = 'none';
        } else {
            item.style.display = 'list-item';
        }
    });
}

function showLoginError() {
    console.log('Showing login error');
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.textContent = 'Invalid username or password. Please try again.';
        errorEl.style.display = 'block';
    } else {
        alert('Login failed: Invalid username or password.');
    }
    showLoginForm();
}

function showMainContent(roles) {
    console.log('Showing main content for roles:', roles);
    isLibrarian = roles.includes('LIBRARIAN');
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('main-content').style.display = 'block';
    document.getElementById('section-menu').style.display = 'flex';
    document.getElementById('login-menu-btn').style.display = 'none';
    document.getElementById('logout-menu-btn').style.display = 'block';

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
    'users': 'users',
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
    if (!isLibrarian && (document.getElementById('login-form').style.display === 'block')) {
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('main-content').style.display = 'block';
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
    if (navbarCollapse.classList.contains('show')) {
        const bsCollapse = new bootstrap.Collapse(navbarCollapse, {
            toggle: false
        });
        bsCollapse.hide();
    }
}

function createBookByPhoto() {
    showSection('books');
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const title = `${year}-${month}-${day}-${hours}-${minutes}-${seconds}`;

    // This function will be created in books.js
    prepareNewBookForPhoto(title);
}

async function logout() {
    document.body.classList.remove('user-is-librarian');
    try {
        const response = await fetch('/logout', { method: 'POST' });
        if (response.ok) {
            window.location.href = '/';
        } else {
            console.error('Logout failed with status:', response.status);
            alert('Logout failed. Please try again.');
        }
    } catch (error) {
        console.error('An error occurred during logout:', error);
        alert('An error occurred during logout. Please check your connection and try again.');
    }
}

async function fetchData(url) {
    const response = await fetch(url);
    if (!response.ok) {
        let errorMsg;
        if (response.status === 401) {
            errorMsg = "Your login session has timed out. Please log in again.";
        } else {
            errorMsg = `HTTP error! status: ${response.status}`;
            try {
                const errorText = await response.text();
                let errorData;
                try {
                    errorData = JSON.parse(errorText);
                    if (errorData && typeof errorData === 'object' && errorData.message) {
                        errorMsg = errorData.message;
                    }
                } catch (parseErr) {
                    errorMsg = `${errorMsg} - ${errorText.substring(0, 100)}...`;
                }
            } catch (textErr) {
                // Use generic if text fails
            }
        }
        throw new Error(errorMsg);
    }
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.indexOf("application/json") !== -1) {
        return response.json();
    }
    return; // No content
}

async function postData(url, data, isFormData = false, expectJson = true) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {};
    if (token) {
        headers['X-XSRF-TOKEN'] = token;
    }

    let body;
    if (isFormData) {
        body = data;
    } else {
        headers['Content-Type'] = 'application/json';
        body = JSON.stringify(data);
    }

    const response = await fetch(url, {
        method: 'POST',
        headers,
        body: body
    });
    if (!response.ok) {
        let errorMsg;
        if (response.status === 401) {
            errorMsg = "Your login session has timed out. Please log in again.";
        } else {
            errorMsg = `HTTP error! status: ${response.status}`;
            try {
                const errorText = await response.text();
                let errorData;
                try {
                    errorData = JSON.parse(errorText);
                    if (errorData && typeof errorData === 'object' && errorData.message) {
                        errorMsg = errorData.message;
                    }
                } catch (parseErr) {
                    errorMsg = `${errorMsg} - ${errorText.substring(0, 100)}...`;
                }
            } catch (textErr) {
                // Use generic if text fails
            }
        }
        throw new Error(errorMsg);
    }
    if (expectJson) {
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return response.json();
        }
        return {}; // Empty object if not JSON
    }
    return response;
}

async function putData(url, data, expectJson = true) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['X-XSRF-TOKEN'] = token;
    }
    const response = await fetch(url, {
        method: 'PUT',
        headers,
        body: JSON.stringify(data)
    });
    if (!response.ok) {
        let errorMsg;
        if (response.status === 401) {
            errorMsg = "Your login session has timed out. Please log in again.";
        } else {
            errorMsg = `HTTP error! status: ${response.status}`;
            try {
                const errorText = await response.text();
                let errorData;
                try {
                    errorData = JSON.parse(errorText);
                    if (errorData && typeof errorData === 'object' && errorData.message) {
                        errorMsg = errorData.message;
                    }
                } catch (parseErr) {
                    errorMsg = `${errorMsg} - ${errorText.substring(0, 100)}...`;
                }
            } catch (textErr) {
                // Use generic if text fails
            }
        }
        throw new Error(errorMsg);
    }
    if (expectJson) {
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return response.json();
        }
        return {}; // Empty object if not JSON
    }
    return response;
}

async function deleteData(url) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {};
    if (token) {
        headers['X-XSRF-TOKEN'] = token;
    }
    const response = await fetch(url, {
        method: 'DELETE',
        headers
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `HTTP error! status: ${response.status}`);
    }
    return response;
}

async function applyForCard() {
    const username = document.getElementById('new-applicant-name').value.trim();
    const password = document.getElementById('new-applicant-password').value;
    if (!username || !password) {
        showApplyError('Please fill in all fields.');
        return;
    }
    try {
        await postData('/api/public/register', { username, password }, false, false);
        document.getElementById('new-applicant-name').value = '';
        document.getElementById('new-applicant-password').value = '';
        showApplySuccess('Library card application successful.');
        clearApplyError();
    } catch (error) {
        showApplyError('Failed to apply for library card: ' + error.message);
    }
}

function showApplyError(message) {
    const errorEl = document.getElementById('apply-error');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }
}

function clearApplyError() {
    const errorEl = document.getElementById('apply-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }
}

function showApplySuccess(message) {
    const successEl = document.getElementById('apply-success');
    if (successEl) {
        successEl.textContent = message;
        successEl.style.display = 'block';
    }
}
