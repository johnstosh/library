// (c) Copyright 2025 by Muczynski
async function addAuthor() {
    window.scrollTo(0, 0);
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

    const btn = document.getElementById('add-author-btn');
    showButtonSpinner(btn, 'Adding...');

    try {
        const newAuthor = await postData('/api/authors', { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
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
    } finally {
        hideButtonSpinner(btn);
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
    document.getElementById('current-author-id').value = id;
    const btn = document.getElementById('add-author-btn');
    btn.textContent = 'Update Author';
    btn.onclick = () => updateAuthor(id);

    document.getElementById('cancel-author-btn').style.display = 'inline-block';
    document.getElementById('add-author-photo-btn').style.display = 'inline-block';
    document.getElementById('add-author-photo-google-btn').style.display = 'inline-block';

    showAuthorList(false);

    const photos = await fetchData(`/api/authors/${id}/photos`);
    displayAuthorPhotos(photos, id);

    const books = await fetchData(`/api/authors/${id}/books`);
    displayAuthorBooks(books);
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

    const btn = document.getElementById('add-author-btn');
    showButtonSpinner(btn, 'Updating...');

    try {
        await putData(`/api/authors/${id}`, { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
        await loadAuthors();
        await populateBookDropdowns();
        resetAuthorForm();
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to update author: ' + error.message);
    } finally {
        hideButtonSpinner(btn);
    }
}

function resetAuthorForm() {
    document.getElementById('new-author-name').value = '';
    document.getElementById('new-author-dob').value = '';
    document.getElementById('new-author-dod').value = '';
    document.getElementById('new-author-religion').value = '';
    document.getElementById('new-author-country').value = '';
    document.getElementById('new-author-nationality').value = '';
    document.getElementById('new-author-bio').value = '';
    document.getElementById('current-author-id').value = '';

    // Remove readonly attributes (in case they were set for viewing)
    document.getElementById('new-author-name').readOnly = false;
    document.getElementById('new-author-dob').readOnly = false;
    document.getElementById('new-author-dod').readOnly = false;
    document.getElementById('new-author-religion').readOnly = false;
    document.getElementById('new-author-country').readOnly = false;
    document.getElementById('new-author-nationality').readOnly = false;
    document.getElementById('new-author-bio').readOnly = false;

    const btn = document.getElementById('add-author-btn');
    btn.textContent = 'Add Author';
    btn.onclick = addAuthor;

    document.getElementById('cancel-author-btn').style.display = 'none';
    document.getElementById('add-author-photo-btn').style.display = 'none';
    document.getElementById('add-author-photo-google-btn').style.display = 'none';
    document.getElementById('author-photos-container').style.display = 'none';
    document.getElementById('author-books-container').style.display = 'none';

    // Hide the form for non-librarians (it was shown by viewAuthor)
    if (!window.isLibrarian) {
        const authorForm = document.querySelector('[data-test="authors-form"]');
        if (authorForm) {
            authorForm.style.display = 'none';
        }
    }

    showAuthorList(true);
    clearError('authors');
}

function displayAuthorBooks(books) {
    const container = document.getElementById('author-books-container');
    const booksDiv = document.getElementById('author-books');

    if (!books || books.length === 0) {
        container.style.display = 'none';
        return;
    }

    container.style.display = 'block';
    booksDiv.innerHTML = '';

    const ul = document.createElement('ul');
    ul.classList.add('list-group');

    books.forEach(book => {
        const li = document.createElement('li');
        li.classList.add('list-group-item', 'd-flex', 'justify-content-between', 'align-items-center');

        // Left side: title and year
        const leftDiv = document.createElement('div');
        leftDiv.classList.add('d-flex', 'align-items-center', 'gap-2');

        const titleSpan = document.createElement('span');
        titleSpan.textContent = book.title;
        if (book.publicationYear) {
            titleSpan.textContent += ` (${book.publicationYear})`;
        }
        leftDiv.appendChild(titleSpan);

        // Add LOC number badge if available
        if (book.locNumber) {
            const badge = document.createElement('span');
            badge.classList.add('badge', 'bg-success');
            badge.textContent = book.locNumber;
            leftDiv.appendChild(badge);
        }

        li.appendChild(leftDiv);

        // Right side: action buttons
        const actionsDiv = document.createElement('div');
        actionsDiv.classList.add('btn-group', 'btn-group-sm');

        // View button
        const viewBtn = document.createElement('button');
        viewBtn.classList.add('btn', 'btn-outline-secondary');
        viewBtn.textContent = '👁️';
        viewBtn.title = 'View';
        viewBtn.onclick = () => {
            window.viewBook(book.id);
        };
        actionsDiv.appendChild(viewBtn);

        // Edit button
        const editBtn = document.createElement('button');
        editBtn.classList.add('btn', 'btn-outline-secondary');
        editBtn.textContent = '✏️';
        editBtn.title = 'Edit';
        editBtn.onclick = () => {
            editBook(book.id);
        };
        actionsDiv.appendChild(editBtn);

        // Delete button
        const delBtn = document.createElement('button');
        delBtn.classList.add('btn', 'btn-outline-danger');
        delBtn.textContent = '🗑️';
        delBtn.title = 'Delete';
        delBtn.onclick = () => {
            deleteBook(book.id);
        };
        actionsDiv.appendChild(delBtn);

        li.appendChild(actionsDiv);
        ul.appendChild(li);
    });

    booksDiv.appendChild(ul);
}

async function deleteAuthorsWithNoBooks() {
    const confirmed = confirm('Are you sure you want to delete all authors who have no books? This action cannot be undone.');
    if (!confirmed) {
        return;
    }

    const btn = document.getElementById('delete-authors-no-books-btn');
    showButtonSpinner(btn, 'Deleting...');

    try {
        const result = await postData('/api/authors/delete-authors-with-no-books', {});
        showSuccess('authors', result.message || `Deleted ${result.deletedCount} author(s) with no books`);
        await loadAuthors();
    } catch (error) {
        showError('authors', 'Failed to delete authors: ' + error.message);
    } finally {
        hideButtonSpinner(btn);
    }
}

// Expose functions globally for access from other scripts
window.resetAuthorForm = resetAuthorForm;
window.updateAuthor = updateAuthor;
