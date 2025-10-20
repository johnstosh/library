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
    window.scrollTo(0, 0);
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
        document.getElementById('book-photos-container').style.display = 'none';
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

    const photos = await fetchData(`/api/books/${id}/photos`);
    displayBookPhotos(photos, id);
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
        document.getElementById('book-photos-container').style.display = 'none';
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

function displayBookPhotos(photos, bookId) {
    const photosContainer = document.getElementById('book-photos-container');
    const photosDiv = document.getElementById('book-photos');
    photosDiv.innerHTML = '';

    if (photos && photos.length > 0) {
        photosContainer.style.display = 'block';
        photos.forEach(photo => {
            const photoWrapper = document.createElement('div');
            photoWrapper.className = 'book-photo-wrapper';
            photoWrapper.setAttribute('data-photo-id', photo.id);

            const deleteBtn = document.createElement('button');
            deleteBtn.setAttribute('data-test', 'delete-photo-btn');
            deleteBtn.textContent = '🗑️';
            deleteBtn.title = 'Delete Photo';
            deleteBtn.className = 'delete-photo-icon';
            deleteBtn.onclick = () => deleteBookPhoto(bookId, photo.id);

            const img = document.createElement('img');
            img.src = photo.url;
            img.style.width = '60%';
            img.setAttribute('data-test', 'book-photo');

            photoWrapper.appendChild(deleteBtn);
            photoWrapper.appendChild(img);
            photosDiv.appendChild(photoWrapper);
        });
    } else {
        photosContainer.style.display = 'none';
    }
}

async function deleteBookPhoto(bookId, photoId) {
    if (!confirm('Are you sure you want to delete this photo?')) return;
    try {
        await deleteData(`/api/books/${bookId}/photos/${photoId}`);
        const photoWrapper = document.querySelector(`.book-photo-wrapper[data-photo-id='${photoId}']`);
        if (photoWrapper) {
            photoWrapper.remove();
        }
        const photosDiv = document.getElementById('book-photos');
        if (photosDiv.childElementCount === 0) {
            document.getElementById('book-photos-container').style.display = 'none';
        }
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to delete photo: ' + error.message);
    }
}

async function addPhoto() {
    const bookId = document.getElementById('add-book-btn').textContent === 'Update Book'
        ? document.querySelector('#book-list .active')?.getAttribute('data-entity-id')
        : null;

    if (!bookId) {
        await addBook();
        const newBookId = document.querySelector('#book-list li:last-child')?.getAttribute('data-entity-id');
        if (newBookId) {
            await editBook(newBookId);
            document.getElementById('photo-upload').click();
        }
    } else {
        document.getElementById('photo-upload').click();
    }
}

document.getElementById('photo-upload').addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const bookId = document.querySelector('#book-list .active').getAttribute('data-entity-id');
    const formData = new FormData();
    formData.append('file', file);

    try {
        await postData(`/api/books/${bookId}/photos`, formData, true);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to add photo: ' + error.message);
    }
});

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
