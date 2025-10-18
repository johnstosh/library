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

    checkAuthentication();
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
            console.log('Authentication failed or non-JSON response, showing login form');
            showLoginForm();
        }
    } catch (error) {
        console.error('Error during authentication check:', error);
        showLoginForm();
    }
}

function showLoginForm() {
    document.getElementById('login-form').style.display = 'block';
    document.getElementById('main-content').style.display = 'none';
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }
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
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }

    document.getElementById('section-menu').style.display = 'flex';

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

function logout() {
    document.body.classList.remove('user-is-librarian');
    fetch('/logout', { method: 'POST' }).then(() => {
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

async function postData(url, data) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['X-XSRF-TOKEN'] = token;
    }
    const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(data)
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

async function loadLibraries() {
    try {
        const libraries = await fetchData('/api/libraries');
        const list = document.getElementById('library-list');
        list.innerHTML = '';
        libraries.forEach(library => {
            const li = document.createElement('li');
            li.setAttribute('data-test', 'library-item');
            li.setAttribute('data-entity-id', library.id);
            const span = document.createElement('span');
            span.setAttribute('data-test', 'library-name');
            span.textContent = `${library.name} (${library.hostname})`;
            li.appendChild(span);
            if (isLibrarian) {
                const viewBtn = document.createElement('button');
                viewBtn.setAttribute('data-test', 'view-library-btn');
                viewBtn.textContent = '🔍';
                viewBtn.title = 'View details';
                viewBtn.onclick = () => viewLibrary(library.id);
                li.appendChild(viewBtn);

                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-library-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editLibrary(library.id);
                li.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-library-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
                delBtn.onclick = () => deleteLibrary(library.id);
                li.appendChild(delBtn);
            }
            list.appendChild(li);
        });
        const pageTitle = document.getElementById('page-title');
        if (libraries.length > 0) {
            pageTitle.innerHTML = `The ${libraries[0].name} Branch<br><small>of the Sacred Heart Library System</small>`;
        } else {
            pageTitle.textContent = 'Library Management';
        }
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to load libraries: ' + error.message);
    }
}

async function addLibrary() {
    const name = document.getElementById('new-library-name').value;
    const hostname = document.getElementById('new-library-hostname').value;
    if (!name || !hostname) {
        showError('libraries', 'Name and hostname are required.');
        return;
    }
    try {
        await postData('/api/libraries', { name, hostname });
        document.getElementById('new-library-name').value = '';
        document.getElementById('new-library-hostname').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to add library: ' + error.message);
    }
}

async function editLibrary(id) {
    const data = await fetchData(`/api/libraries/${id}`);
    document.getElementById('new-library-name').value = data.name || '';
    document.getElementById('new-library-hostname').value = data.hostname || '';
    const btn = document.getElementById('add-library-btn');
    btn.textContent = 'Update Library';
    btn.onclick = () => updateLibrary(id);
}

async function updateLibrary(id) {
    const name = document.getElementById('new-library-name').value;
    const hostname = document.getElementById('new-library-hostname').value;
    if (!name || !hostname) {
        showError('libraries', 'Name and hostname are required.');
        return;
    }
    try {
        await putData(`/api/libraries/${id}`, { name, hostname });
        document.getElementById('new-library-name').value = '';
        document.getElementById('new-library-hostname').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        const btn = document.getElementById('add-library-btn');
        btn.textContent = 'Add Library';
        btn.onclick = addLibrary;
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to update library: ' + error.message);
    }
}

async function deleteLibrary(id) {
    if (!confirm('Are you sure you want to delete this library?')) return;
    try {
        await deleteData(`/api/libraries/${id}`);
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to delete library: ' + error.message);
    }
}

async function viewLibrary(id) {
    const data = await fetchData(`/api/libraries/${id}`);
    alert(`Library Details:\nID: ${data.id}\nName: ${data.name}\nHostname: ${data.hostname}`);
}

async function loadAuthors() {
    try {
        const authors = await fetchData('/api/authors');
        const list = document.getElementById('author-list');
        list.innerHTML = '';
        authors.forEach(author => {
            const li = document.createElement('li');
            li.setAttribute('data-test', 'author-item');
            li.setAttribute('data-entity-id', author.id);
            const span = document.createElement('span');
            span.setAttribute('data-test', 'author-name');
            span.textContent = author.name;
            li.appendChild(span);
            if (isLibrarian) {
                const viewBtn = document.createElement('button');
                viewBtn.setAttribute('data-test', 'view-author-btn');
                viewBtn.textContent = '🔍';
                viewBtn.title = 'View details';
                viewBtn.onclick = () => viewAuthor(author.id);
                li.appendChild(viewBtn);

                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-author-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editAuthor(author.id);
                li.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-author-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
                delBtn.onclick = () => deleteAuthor(author.id);
                li.appendChild(delBtn);
            }
            list.appendChild(li);
        });
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to load authors: ' + error.message);
    }
}

async function addAuthor() {
    const name = document.getElementById('new-author-name').value;
    const dateOfBirth = document.getElementById('new-author-dob').value;
    const dateOfDeath = document.getElementById('new-author-dod').value;
    const religiousAffiliation = document.getElementById('new-author-religion').value;
    const birthCountry = document.getElementById('new-author-country').value;
    const nationality = document.getElementById('new-author-nationality').value;
    const briefBiography = document.getElementById('new-author-bio').value;
    if (!name) {
        showError('authors', 'Author name is required.');
        return;
    }
    try {
        await postData('/api/authors', { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
        document.getElementById('new-author-name').value = '';
        document.getElementById('new-author-dob').value = '';
        document.getElementById('new-author-dod').value = '';
        document.getElementById('new-author-religion').value = '';
        document.getElementById('new-author-country').value = '';
        document.getElementById('new-author-nationality').value = '';
        document.getElementById('new-author-bio').value = '';
        await loadAuthors();
        await populateBookDropdowns();
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to add author: ' + error.message);
    }
}

async function editAuthor(id) {
    const data = await fetchData(`/api/authors/${id}`);
    document.getElementById('new-author-name').value = data.name || '';
    document.getElementById('new-author-dob').value = data.dateOfBirth || '';
    document.getElementById('new-author-dod').value = data.dateOfDeath || '';
    document.getElementById('new-author-religion').value = data.religiousAffiliation || '';
    document.getElementById('new-author-country').value = data.birthCountry || '';
    document.getElementById('new-author-nationality').value = data.nationality || '';
    document.getElementById('new-author-bio').value = data.briefBiography || '';
    const btn = document.getElementById('add-author-btn');
    btn.textContent = 'Update Author';
    btn.onclick = () => updateAuthor(id);
}

async function updateAuthor(id) {
    const name = document.getElementById('new-author-name').value;
    const dateOfBirth = document.getElementById('new-author-dob').value;
    const dateOfDeath = document.getElementById('new-author-dod').value;
    const religiousAffiliation = document.getElementById('new-author-religion').value;
    const birthCountry = document.getElementById('new-author-country').value;
    const nationality = document.getElementById('new-author-nationality').value;
    const briefBiography = document.getElementById('new-author-bio').value;
    if (!name) {
        showError('authors', 'Author name is required.');
        return;
    }
    try {
        await putData(`/api/authors/${id}`, { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
        document.getElementById('new-author-name').value = '';
        document.getElementById('new-author-dob').value = '';
        document.getElementById('new-author-dod').value = '';
        document.getElementById('new-author-religion').value = '';
        document.getElementById('new-author-country').value = '';
        document.getElementById('new-author-nationality').value = '';
        document.getElementById('new-author-bio').value = '';
        await loadAuthors();
        await populateBookDropdowns();
        const btn = document.getElementById('add-author-btn');
        btn.textContent = 'Add Author';
        btn.onclick = addAuthor;
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to update author: ' + error.message);
    }
}

async function deleteAuthor(id) {
    if (!confirm('Are you sure you want to delete this author?')) return;
    try {
        await deleteData(`/api/authors/${id}`);
        await loadAuthors();
        await populateBookDropdowns();
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to delete author: ' + error.message);
    }
}

async function viewAuthor(id) {
    const data = await fetchData(`/api/authors/${id}`);
    alert(`Author Details:\nID: ${data.id}\nName: ${data.name}\nDate of Birth: ${data.dateOfBirth || 'N/A'}\nDate of Death: ${data.dateOfDeath || 'N/A'}\nReligious Affiliation: ${data.religiousAffiliation || 'N/A'}\nBirth Country: ${data.birthCountry || 'N/A'}\nNationality: ${data.nationality || 'N/A'}\nBrief Biography: ${data.briefBiography || 'N/A'}`);
}

async function bulkImportAuthors() {
    const authorsJson = document.getElementById('bulk-authors').value;
    if (!authorsJson) {
        showError('authors', 'Please provide JSON for bulk import.');
        return;
    }
    try {
        const authors = JSON.parse(authorsJson);
        await postData('/api/authors/bulk', authors);
        await loadAuthors();
        showBulkSuccess('bulk-authors');
        document.getElementById('bulk-authors').value = '';
        clearError('authors');
    } catch (error) {
        showError('authors', 'Invalid JSON or failed bulk import: ' + error.message);
    }
}

async function loadBooks() {
    try {
        const books = await fetchData('/api/books');
        const list = document.getElementById('book-list');
        list.innerHTML = '';
        books.forEach(book => {
            const li = document.createElement('li');
            li.setAttribute('data-test', 'book-item');
            li.setAttribute('data-entity-id', book.id);
            const span = document.createElement('span');
            span.setAttribute('data-test', 'book-title');
            span.textContent = book.title;
            li.appendChild(span);
            if (isLibrarian) {
                const viewBtn = document.createElement('button');
                viewBtn.setAttribute('data-test', 'view-book-btn');
                viewBtn.textContent = '🔍';
                viewBtn.title = 'View details';
                viewBtn.onclick = () => viewBook(book.id);
                li.appendChild(viewBtn);

                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-book-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editBook(book.id);
                li.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-book-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
                delBtn.onclick = () => deleteBook(book.id);
                li.appendChild(delBtn);
            }
            list.appendChild(li);
        });
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to load books: ' + error.message);
    }
}

async function addBook() {
    const title = document.getElementById('new-book-title').value;
    const publicationYear = document.getElementById('new-book-year').value;
    const publisher = document.getElementById('new-book-publisher').value;
    const plotSummary = document.getElementById('new-book-summary').value;
    const relatedWorks = document.getElementById('new-book-related').value;
    const detailedDescription = document.getElementById('new-book-description').value;
    const dateAddedToLibrary = document.getElementById('new-book-added').value;
    const status = document.getElementById('new-book-status').value;
    const authorId = document.getElementById('book-author').value;
    const libraryId = document.getElementById('book-library').value;
    if (!title || !authorId || !libraryId) {
        showError('books', 'Title, author, and library are required.');
        return;
    }
    try {
        await postData('/api/books', { title, publicationYear, publisher, plotSummary, relatedWorks, detailedDescription, dateAddedToLibrary, status, authorId, libraryId });
        document.getElementById('new-book-title').value = '';
        document.getElementById('new-book-year').value = '';
        document.getElementById('new-book-publisher').value = '';
        document.getElementById('new-book-summary').value = '';
        document.getElementById('new-book-related').value = '';
        document.getElementById('new-book-description').value = '';
        document.getElementById('new-book-added').value = '';
        document.getElementById('new-book-status').value = 'ACTIVE';
        document.getElementById('book-author').selectedIndex = 0;
        document.getElementById('book-library').selectedIndex = 0;
        await loadBooks();
        await populateLoanDropdowns();
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to add book: ' + error.message);
    }
}

async function editBook(id) {
    const data = await fetchData(`/api/books/${id}`);
    document.getElementById('new-book-title').value = data.title || '';
    document.getElementById('new-book-year').value = data.publicationYear || '';
    document.getElementById('new-book-publisher').value = data.publisher || '';
    document.getElementById('new-book-summary').value = data.plotSummary || '';
    document.getElementById('new-book-related').value = data.relatedWorks || '';
    document.getElementById('new-book-description').value = data.detailedDescription || '';
    document.getElementById('new-book-added').value = data.dateAddedToLibrary || '';
    document.getElementById('new-book-status').value = data.status || 'ACTIVE';
    document.getElementById('book-author').value = data.authorId || '';
    document.getElementById('book-library').value = data.libraryId || '';
    const btn = document.getElementById('add-book-btn');
    btn.textContent = 'Update Book';
    btn.onclick = () => updateBook(id);
}

async function updateBook(id) {
    const title = document.getElementById('new-book-title').value;
    const publicationYear = document.getElementById('new-book-year').value;
    const publisher = document.getElementById('new-book-publisher').value;
    const plotSummary = document.getElementById('new-book-summary').value;
    const relatedWorks = document.getElementById('new-book-related').value;
    const detailedDescription = document.getElementById('new-book-description').value;
    const dateAddedToLibrary = document.getElementById('new-book-added').value;
    const status = document.getElementById('new-book-status').value;
    const authorId = document.getElementById('book-author').value;
    const libraryId = document.getElementById('book-library').value;
    if (!title || !authorId || !libraryId) {
        showError('books', 'Title, author, and library are required.');
        return;
    }
    try {
        await putData(`/api/books/${id}`, { title, publicationYear, publisher, plotSummary, relatedWorks, detailedDescription, dateAddedToLibrary, status, authorId, libraryId });
        document.getElementById('new-book-title').value = '';
        document.getElementById('new-book-year').value = '';
        document.getElementById('new-book-publisher').value = '';
        document.getElementById('new-book-summary').value = '';
        document.getElementById('new-book-related').value = '';
        document.getElementById('new-book-description').value = '';
        document.getElementById('new-book-added').value = '';
        document.getElementById('new-book-status').value = 'ACTIVE';
        document.getElementById('book-author').selectedIndex = 0;
        document.getElementById('book-library').selectedIndex = 0;
        await loadBooks();
        await loadLoans();
        await populateLoanDropdowns();
        const btn = document.getElementById('add-book-btn');
        btn.textContent = 'Add Book';
        btn.onclick = addBook;
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to update book: ' + error.message);
    }
}

async function deleteBook(id) {
    if (!confirm('Are you sure you want to delete this book?')) return;
    try {
        await deleteData(`/api/books/${id}`);
        await loadBooks();
        await loadLoans();
        await populateLoanDropdowns();
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to delete book: ' + error.message);
    }
}

async function viewBook(id) {
    const data = await fetchData(`/api/books/${id}`);
    alert(`Book Details:\nID: ${data.id}\nTitle: ${data.title}\nPublication Year: ${data.publicationYear || 'N/A'}\nPublisher: ${data.publisher || 'N/A'}\nPlot Summary: ${data.plotSummary || 'N/A'}\nRelated Works: ${data.relatedWorks || 'N/A'}\nDetailed Description: ${data.detailedDescription || 'N/A'}\nDate Added: ${data.dateAddedToLibrary || 'N/A'}\nStatus: ${data.status || 'N/A'}\nAuthor: ${data.author ? data.author.name : 'N/A'}\nLibrary: ${data.library ? data.library.name : 'N/A'}`);
}

async function bulkImportBooks() {
    const booksJson = document.getElementById('bulk-books').value;
    if (!booksJson) {
        showError('books', 'Please provide JSON for bulk import.');
        return;
    }
    try {
        const books = JSON.parse(booksJson);
        await postData('/api/books/bulk', books);
        await loadBooks();
        showBulkSuccess('bulk-books');
        document.getElementById('bulk-books').value = '';
        clearError('books');
    } catch (error) {
        showError('books', 'Invalid JSON or failed bulk import: ' + error.message);
    }
}

async function loadUsers() {
    try {
        const users = await fetchData('/api/users');
        const list = document.getElementById('user-list');
        list.innerHTML = '';
        users.forEach(user => {
            const li = document.createElement('li');
            li.setAttribute('data-test', 'user-item');
            li.setAttribute('data-entity-id', user.id);
            const span = document.createElement('span');
            span.setAttribute('data-test', 'user-name');
            const rolesText = user.roles ? Array.from(user.roles).join(', ') : '';
            span.textContent = `${user.username} (${rolesText})`;
            li.appendChild(span);

            const viewBtn = document.createElement('button');
            viewBtn.setAttribute('data-test', 'view-user-btn');
            viewBtn.textContent = '🔍';
            viewBtn.title = 'View details';
            viewBtn.onclick = () => viewUser(user.id);
            li.appendChild(viewBtn);

            const editBtn = document.createElement('button');
            editBtn.setAttribute('data-test', 'edit-user-btn');
            editBtn.textContent = '✏️';
            editBtn.title = 'Edit';
            editBtn.onclick = () => editUser(user.id);
            li.appendChild(editBtn);

            const delBtn = document.createElement('button');
            delBtn.setAttribute('data-test', 'delete-user-btn');
            delBtn.textContent = '🗑️';
            delBtn.title = 'Delete';
            delBtn.onclick = () => deleteUser(user.id);
            li.appendChild(delBtn);
            list.appendChild(li);
        });
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to load users: ' + error.message);
    }
}

async function addUser() {
    const username = document.getElementById('new-user-username').value;
    const password = document.getElementById('new-user-password').value;
    const role = document.getElementById('new-user-role').value;
    if (!username || !password || !role) {
        showError('users', 'Please fill in all fields.');
        return;
    }
    try {
        await postData('/api/users', { username, password, role });
        document.getElementById('new-user-username').value = '';
        document.getElementById('new-user-password').value = '';
        document.getElementById('new-user-role').value = 'USER';
        await loadUsers();
        await populateLoanDropdowns();
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to add user: ' + error.message);
    }
}

async function editUser(id) {
    const data = await fetchData(`/api/users/${id}`);
    document.getElementById('new-user-username').value = data.username || '';
    document.getElementById('new-user-password').value = '';
    const roleSelect = document.getElementById('new-user-role');
    const roles = data.roles;
    if (roles && roles.size > 0) {
        roleSelect.value = Array.from(roles)[0];
    }
    const btn = document.getElementById('add-user-btn');
    btn.textContent = 'Update User';
    btn.onclick = () => updateUser(id);
}

async function updateUser(id) {
    const username = document.getElementById('new-user-username').value;
    const password = document.getElementById('new-user-password').value;
    const role = document.getElementById('new-user-role').value;
    if (!username || !role) {
        showError('users', 'Please fill in username and role.');
        return;
    }
    try {
        await putData(`/api/users/${id}`, { username, password: password || null, role });
        document.getElementById('new-user-username').value = '';
        document.getElementById('new-user-password').value = '';
        document.getElementById('new-user-role').value = 'USER';
        await loadUsers();
        await populateLoanDropdowns();
        const btn = document.getElementById('add-user-btn');
        btn.textContent = 'Add User';
        btn.onclick = addUser;
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to update user: ' + error.message);
    }
}

async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
        await deleteData(`/api/users/${id}`);
        await loadUsers();
        await populateLoanDropdowns();
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to delete user: ' + error.message);
    }
}

async function viewUser(id) {
    const data = await fetchData(`/api/users/${id}`);
    const rolesText = data.roles ? Array.from(data.roles).join(', ') : '';
    alert(`User Details:\nID: ${data.id}\nUsername: ${data.username}\nRoles: ${rolesText}`);
}

async function loadLoans() {
    try {
        const loans = await fetchData('/api/loans');
        const list = document.getElementById('loan-list');
        list.innerHTML = '';

        // Fetch all books and users once for efficiency
        const books = await fetchData('/api/books');
        const bookMap = new Map(books.map(book => [book.id, book.title]));
        const users = await fetchData('/api/users');
        const userMap = new Map(users.map(user => [user.id, user.username]));

        for (const loan of loans) {
            const li = document.createElement('li');
            li.setAttribute('data-test', 'loan-item');
            li.setAttribute('data-entity-id', loan.id);
            const span = document.createElement('span');
            span.setAttribute('data-test', 'loan-details');
            const bookTitle = bookMap.get(loan.bookId) || 'Unknown Book';
            const userName = userMap.get(loan.userId) || 'Unknown User';
            span.textContent = `${bookTitle} loaned to ${userName} on ${formatDate(loan.loanDate)}`;
            li.appendChild(span);
            if (loan.returnDate) {
                span.textContent += ` (returned on ${formatDate(loan.returnDate)})`;
            } else {
                const returnButton = document.createElement('button');
                returnButton.setAttribute('data-test', 'return-book-btn');
                returnButton.setAttribute('data-loan-id', loan.id);
                returnButton.textContent = 'Return';
                returnButton.className = 'return-btn';
                returnButton.onclick = () => returnBook(loan.id);
                li.appendChild(returnButton);
            }

            const viewBtn = document.createElement('button');
            viewBtn.setAttribute('data-test', 'view-loan-btn');
            viewBtn.textContent = '🔍';
            viewBtn.title = 'View details';
            viewBtn.onclick = () => viewLoan(loan.id);
            li.appendChild(viewBtn);

            const editBtn = document.createElement('button');
            editBtn.setAttribute('data-test', 'edit-loan-btn');
            editBtn.textContent = '✏️';
            editBtn.title = 'Edit';
            editBtn.onclick = () => editLoan(loan.id);
            li.appendChild(editBtn);

            const delBtn = document.createElement('button');
            delBtn.setAttribute('data-test', 'delete-loan-btn');
            delBtn.textContent = '🗑️';
            delBtn.title = 'Delete';
            delBtn.onclick = () => deleteLoan(loan.id);
            li.appendChild(delBtn);
            list.appendChild(li);
        }
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to load loans: ' + error.message);
    }
}

async function checkoutBook() {
    const bookId = document.getElementById('loan-book').value;
    const userId = document.getElementById('loan-user').value;
    const loanDate = document.getElementById('loan-date').value;
    const returnDate = document.getElementById('return-date').value;
    if (!bookId || !userId) {
        showError('loans', 'Book and user are required.');
        return;
    }
    try {
        await postData('/api/loans/checkout', { bookId, userId, loanDate: loanDate || null, returnDate: returnDate || null });
        document.getElementById('loan-book').selectedIndex = 0;
        document.getElementById('loan-user').selectedIndex = 0;
        document.getElementById('loan-date').value = '';
        document.getElementById('return-date').value = '';
        await loadLoans();
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to checkout book: ' + error.message);
    }
}

async function editLoan(id) {
    const data = await fetchData(`/api/loans/${id}`);
    document.getElementById('loan-book').value = data.bookId || '';
    document.getElementById('loan-user').value = data.userId || '';
    document.getElementById('loan-date').value = data.loanDate || '';
    document.getElementById('return-date').value = data.returnDate || '';
    const btn = document.getElementById('checkout-btn');
    btn.textContent = 'Update Loan';
    btn.onclick = () => updateLoan(id);
}

async function updateLoan(id) {
    const bookId = document.getElementById('loan-book').value;
    const userId = document.getElementById('loan-user').value;
    const loanDate = document.getElementById('loan-date').value;
    const returnDate = document.getElementById('return-date').value;
    if (!bookId || !userId) {
        showError('loans', 'Book and user are required.');
        return;
    }
    try {
        await putData(`/api/loans/${id}`, { bookId, userId, loanDate: loanDate || null, returnDate: returnDate || null });
        document.getElementById('loan-book').selectedIndex = 0;
        document.getElementById('loan-user').selectedIndex = 0;
        document.getElementById('loan-date').value = '';
        document.getElementById('return-date').value = '';
        await loadLoans();
        const btn = document.getElementById('checkout-btn');
        btn.textContent = 'Checkout Book';
        btn.onclick = checkoutBook;
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to update loan: ' + error.message);
    }
}

async function deleteLoan(id) {
    if (!confirm('Are you sure you want to delete this loan?')) return;
    try {
        await deleteData(`/api/loans/${id}`);
        await loadLoans();
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to delete loan: ' + error.message);
    }
}

async function viewLoan(id) {
    const loan = await fetchData(`/api/loans/${id}`);
    const returnStatus = loan.returnDate ? `Returned on ${formatDate(loan.returnDate)}` : 'Not returned';
    alert(`Loan Details:\nID: ${loan.id}\nBook: ${loan.bookTitle}\nUser: ${loan.userName}\nLoan Date: ${formatDate(loan.loanDate)}\n${returnStatus}`);
}

async function returnBook(loanId) {
    try {
        await putData(`/api/loans/return/${loanId}`);
        await loadLoans();
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to return book: ' + error.message);
    }
}

async function populateBookDropdowns() {
    try {
        const authors = await fetchData('/api/authors');
        const authorSelect = document.getElementById('book-author');
        authorSelect.innerHTML = '';
        authors.forEach(author => {
            const option = document.createElement('option');
            option.value = author.id;
            option.textContent = author.name;
            authorSelect.appendChild(option);
        });

        const libraries = await fetchData('/api/libraries');
        const librarySelect = document.getElementById('book-library');
        librarySelect.innerHTML = '';
        libraries.forEach(library => {
            const option = document.createElement('option');
            option.value = library.id;
            option.textContent = library.name;
            librarySelect.appendChild(option);
        });
    } catch (error) {
        showError('books', 'Failed to populate dropdowns: ' + error.message);
    }
}

async function populateLoanDropdowns() {
    try {
        const books = await fetchData('/api/books');
        const bookSelect = document.getElementById('loan-book');
        bookSelect.innerHTML = '';
        books.forEach(book => {
            const option = document.createElement('option');
            option.value = book.id;
            option.textContent = book.title;
            bookSelect.appendChild(option);
        });

        const users = await fetchData('/api/users');
        const userSelect = document.getElementById('loan-user');
        userSelect.innerHTML = '';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.username;
            userSelect.appendChild(option);
        });
    } catch (error) {
        showError('loans', 'Failed to populate dropdowns: ' + error.message);
    }
}
