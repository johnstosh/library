document.addEventListener('DOMContentLoaded', () => {
    checkAuthentication();
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', login);
    }
    // Check for login error in URL params
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        showLoginError();
        // Clear the error param from URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

async function checkAuthentication() {
    try {
        const response = await fetch('/api/users/me');
        if (response.ok) {
            const user = await response.json();
            showMainContent(user.roles);
        } else {
            showLoginForm();
        }
    } catch (error) {
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
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('main-content').style.display = 'block';
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }

    if (roles.includes('LIBRARIAN')) {
        document.querySelectorAll('.librarian-only').forEach(el => {
            el.style.display = 'block';
        });
    }

    loadLibraries();
    loadAuthors();
    loadBooks();
    if (roles.includes('LIBRARIAN')) {
        loadUsers();
        loadLoans();
        populateBookDropdowns();
        populateLoanDropdowns();
    }
}

async function fetchData(url) {
    const response = await fetch(url);
    return response.json();
}

async function postData(url, data) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });
    return response.json();
}

async function putData(url) {
    const response = await fetch(url, {
        method: 'PUT'
    });
    return response.json();
}

async function loadLibraries() {
    const libraries = await fetchData('/api/libraries');
    const list = document.getElementById('library-list');
    list.innerHTML = '';
    libraries.forEach(library => {
        const li = document.createElement('li');
        li.textContent = `${library.name} (${library.hostname})`;
        list.appendChild(li);
    });
}

async function addLibrary() {
    const name = document.getElementById('new-library-name').value;
    const hostname = document.getElementById('new-library-hostname').value;
    if (!name || !hostname) return;
    await postData('/api/libraries', { name, hostname });
    document.getElementById('new-library-name').value = '';
    document.getElementById('new-library-hostname').value = '';
    loadLibraries();
    populateBookDropdowns();
}

async function loadAuthors() {
    const authors = await fetchData('/api/authors');
    const list = document.getElementById('author-list');
    list.innerHTML = '';
    authors.forEach(author => {
        const li = document.createElement('li');
        li.textContent = author.name;
        list.appendChild(li);
    });
}

async function addAuthor() {
    const name = document.getElementById('new-author-name').value;
    const dateOfBirth = document.getElementById('new-author-dob').value;
    const dateOfDeath = document.getElementById('new-author-dod').value;
    const religiousAffiliation = document.getElementById('new-author-religion').value;
    const birthCountry = document.getElementById('new-author-country').value;
    const nationality = document.getElementById('new-author-nationality').value;
    const briefBiography = document.getElementById('new-author-bio').value;
    if (!name) return;
    await postData('/api/authors', { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
    document.getElementById('new-author-name').value = '';
    loadAuthors();
    populateBookDropdowns();
}

async function bulkImportAuthors() {
    const authorsJson = document.getElementById('bulk-authors').value;
    if (!authorsJson) return;
    try {
        const authors = JSON.parse(authorsJson);
        await postData('/api/authors/bulk', authors);
        loadAuthors();
    } catch (error) {
        alert('Invalid JSON for bulk import of authors.');
    }
}

async function loadBooks() {
    const books = await fetchData('/api/books');
    const list = document.getElementById('book-list');
    list.innerHTML = '';
    books.forEach(book => {
        const li = document.createElement('li');
        li.textContent = book.title;
        list.appendChild(li);
    });
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
    if (!title || !authorId || !libraryId) return;
    await postData('/api/books', { title, publicationYear, publisher, plotSummary, relatedWorks, detailedDescription, dateAddedToLibrary, status, authorId, libraryId });
    document.getElementById('new-book-title').value = '';
    loadBooks();
    populateLoanDropdowns();
}

async function bulkImportBooks() {
    const booksJson = document.getElementById('bulk-books').value;
    if (!booksJson) return;
    try {
        const books = JSON.parse(booksJson);
        await postData('/api/books/bulk', books);
        loadBooks();
    } catch (error) {
        alert('Invalid JSON for bulk import of books.');
    }
}

async function loadUsers() {
    const users = await fetchData('/api/users');
    const list = document.getElementById('user-list');
    list.innerHTML = '';
    users.forEach(user => {
        const li = document.createElement('li');
        li.textContent = user.username;
        list.appendChild(li);
    });
}

async function loadLoans() {
    const loans = await fetchData('/api/loans');
    const list = document.getElementById('loan-list');
    list.innerHTML = '';
    for (const loan of loans) {
        const li = document.createElement('li');
        const book = await fetchData(`/api/books/${loan.bookId}`);
        const user = await fetchData(`/api/users/${loan.userId}`);
        li.textContent = `${book.title} loaned to ${user.username} on ${loan.loanDate}`;
        if (loan.returnDate) {
            li.textContent += ` (returned on ${loan.returnDate})`;
        } else {
            const returnButton = document.createElement('button');
            returnButton.textContent = 'Return';
            returnButton.onclick = () => returnBook(loan.id);
            li.appendChild(returnButton);
        }
        list.appendChild(li);
    }
}

async function checkoutBook() {
    const bookId = document.getElementById('loan-book').value;
    const userId = document.getElementById('loan-user').value;
    if (!bookId || !userId) return;
    await postData('/api/loans/checkout', { bookId, userId });
    loadLoans();
}

async function returnBook(loanId) {
    await putData(`/api/loans/return/${loanId}`);
    loadLoans();
}

async function populateBookDropdowns() {
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
}

async function populateLoanDropdowns() {
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
}

async function login(event) {
    event.preventDefault();
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    if (!usernameInput || !passwordInput) {
        alert('Username and password fields are required.');
        return;
    }
    const username = usernameInput.value;
    const password = passwordInput.value;
    if (!username || !password) {
        alert('Please enter both username and password.');
        return;
    }

    const formData = new FormData();
    formData.append('username', username);
    formData.append('password', password);

    try {
        const response = await fetch('/login', {
            method: 'POST',
            body: formData,
            credentials: 'include',
            redirect: 'manual'
        });

        if (response.status === 302 || response.status === 303) {
            const location = response.headers.get('Location');
            if (location && location.includes('error')) {
                showLoginError();
            } else {
                // Success
                await checkAuthentication();
            }
        } else {
            // Unexpected response
            showLoginError();
        }
    } catch (error) {
        console.error('Login error:', error);
        showLoginError();
    }
}
