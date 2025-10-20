console.log('app.js loaded');

let isLibrarian = false;

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
        console.error('Error in initial auth check, showing welcome screen.', error);
        showWelcomeScreen();
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
            console.log('Authentication failed or non-JSON response, showing welcome screen');
            showWelcomeScreen();
        }
    } catch (error) {
        console.error('Error during authentication check:', error);
        showWelcomeScreen();
    }
}

function showWelcomeScreen() {
    document.getElementById('welcome-screen').style.display = 'block';
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('main-content').style.display = 'none';
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
    document.getElementById('welcome-screen').style.display = 'none';
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
    document.getElementById('welcome-screen').style.display = 'none';
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('main-content').style.display = 'block';
    document.getElementById('section-menu').style.display = 'flex';
    // Ensure all menu items are visible for logged-in users
    document.querySelectorAll('#section-menu li').forEach(item => {
        item.style.display = 'list-item';
    });
    document.getElementById('login-menu-btn').style.display = 'none';
    document.getElementById('logout-menu-btn').style.display = 'block';
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }

    if (isLibrarian) {
        document.body.classList.add('user-is-librarian');
        showSection('loans');
    } else {
        document.body.classList.remove('user-is-librarian');
        showSection('books');
    }

    loadLibraries();
    loadAuthors();
    loadBooks();
    if (isLibrarian) {
        loadUsers();
        loadLoans();
        populateBookDropdowns();
        populateLoanDropdowns();
    }
}

function showSection(sectionId, event) {
    // If not logged in and showing a section, hide welcome/login and show main content
    if (!isLibrarian && (document.getElementById('welcome-screen').style.display === 'block' || document.getElementById('login-form').style.display === 'block')) {
        document.getElementById('welcome-screen').style.display = 'none';
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('main-content').style.display = 'block';
    }

    // Hide all sections forcefully
    document.querySelectorAll('.section').forEach(section => {
        section.style.setProperty('display', 'none', 'important');
    });
    // Show the selected section forcefully
    const targetSection = document.getElementById(sectionId + '-section');
    if (targetSection) {
        targetSection.style.setProperty('display', 'block', 'important');
    }
    // Update active button
    document.querySelectorAll('#section-menu button').forEach(btn => {
        btn.classList.remove('active');
    });
    if (event) {
        const clickedButton = event.target.closest('button');
        if (clickedButton) {
            clickedButton.classList.add('active');
        }
    } else {
        // If no event, activate the button corresponding to the sectionId
        const activeButton = document.querySelector(`#section-menu button[onclick*="'${sectionId}'"]`);
        if (activeButton) {
            activeButton.classList.add('active');
        }
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
    document.getElementById('new-book-title').value = title;
    document.getElementById('add-book-btn').scrollIntoView({ behavior: 'smooth' });
}

function logout() {
    document.body.classList.remove('user-is-librarian');
    fetch('/logout', { method: 'POST' }).then(() => {
        showWelcomeScreen();
        window.location.href = '/';
    });
}

async function fetchData(url) {
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
}

async function postData(url, data, isFormData = false) {
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
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
}

async function putData(url, data) {
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
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
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
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response;
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
        successDiv.textContent = 'Bulk import successful!';
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
