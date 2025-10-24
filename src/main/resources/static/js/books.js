async function loadBooks() {
    try {
        const books = await fetchData('/api/books');
        const tableBody = document.getElementById('book-list-body');
        tableBody.innerHTML = '';
        books.forEach(book => {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'book-item');
            row.setAttribute('data-entity-id', book.id);

            const titleCell = document.createElement('td');
            const titleSpan = document.createElement('span');
            titleSpan.setAttribute('data-test', 'book-title');
            titleSpan.textContent = book.title;
            titleCell.appendChild(titleSpan);
            row.appendChild(titleCell);

            const actionsCell = document.createElement('td');
            if (isLibrarian) {
                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-book-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editBook(book.id);
                actionsCell.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-book-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
                delBtn.onclick = () => deleteBook(book.id);
                actionsCell.appendChild(delBtn);
            }
            row.appendChild(actionsCell);
            tableBody.appendChild(row);
        });
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to load books: ' + error.message);
    }
}

function showBookList(show) {
    document.querySelector('[data-test="book-table"]').style.display = show ? 'table' : 'none';
    document.querySelector('[data-test="books-form"]').style.display = show ? 'none' : 'block';
}

function showSuccess(sectionId, message) {
    let successDiv = document.querySelector(`#${sectionId}-section [data-test="form-success"]`);
    if (!successDiv) {
        successDiv = document.createElement('div');
        successDiv.setAttribute('data-test', 'form-success');
        successDiv.style.color = 'green';
        successDiv.style.display = 'block';
        const section = document.getElementById(`${sectionId}-section`);
        if (section) {
            section.insertBefore(successDiv, section.firstChild.nextSibling);
        }
    }
    successDiv.textContent = message;
}

function clearSuccess(sectionId) {
    const successDiv = document.querySelector(`#${sectionId}-section [data-test="form-success"]`);
    if (successDiv) {
        successDiv.remove();
    }
}

function resetBookForm() {
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
    document.getElementById('current-book-id').value = '';

    const btn = document.getElementById('add-book-btn');
    btn.textContent = 'Add Book';
    btn.onclick = addBook;

    document.getElementById('add-photo-btn').style.display = 'none';
    document.getElementById('cancel-book-btn').style.display = 'none';
    document.getElementById('book-by-photo-btn').style.display = 'none';
    document.getElementById('book-by-photo-btn').onclick = null;
    document.getElementById('book-photos-container').style.display = 'none';
    showBookList(true);

    clearSuccess('books');
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
        resetBookForm();
        await loadBooks();
        await populateLoanDropdowns();
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to add book: ' + error.message);
    }
}

async function editBook(id) {
    showBookList(false);
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
    document.getElementById('current-book-id').value = id;
    const btn = document.getElementById('add-book-btn');
    btn.textContent = 'Update Book';
    btn.onclick = () => updateBook(id);
    document.getElementById('add-photo-btn').style.display = 'inline-block';
    document.getElementById('cancel-book-btn').style.display = 'inline-block';

    const photos = await fetchData(`/api/books/${id}/photos`);
    displayBookPhotos(photos, id);
    if (photos && photos.length > 0) {
        document.getElementById('book-by-photo-btn').style.display = 'inline-block';
        document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(id);
    }
}

async function generateBookByPhoto(bookId) {
    try {
        const updatedBook = await putData(`/api/books/${bookId}/book-by-photo`, {}, true);
        await populateBookDropdowns();
        document.getElementById('new-book-title').value = updatedBook.title || '';
        document.getElementById('new-book-year').value = updatedBook.publicationYear || '';
        document.getElementById('new-book-publisher').value = updatedBook.publisher || '';
        document.getElementById('new-book-summary').value = updatedBook.plotSummary || '';
        document.getElementById('new-book-related').value = updatedBook.relatedWorks || '';
        document.getElementById('new-book-description').value = updatedBook.detailedDescription || '';
        document.getElementById('new-book-added').value = updatedBook.dateAddedToLibrary || '';
        document.getElementById('new-book-status').value = updatedBook.status || 'ACTIVE';
        document.getElementById('book-author').value = updatedBook.authorId || '';
        document.getElementById('book-library').value = updatedBook.libraryId || '';
        showSuccess('books', 'Book metadata generated successfully using AI');
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to generate book metadata: ' + error.message);
    }
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
        resetBookForm();
        await loadBooks();
        await loadLoans();
        await populateLoanDropdowns();
        clearError('books');
        showBookList(true);
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

function displayBookPhotos(photos, bookId) {
    const photosContainer = document.getElementById('book-photos-container');
    const photosDiv = document.getElementById('book-photos');
    photosDiv.innerHTML = '';

    if (photos && photos.length > 0) {
        photosContainer.style.display = 'block';
        photosDiv.className = 'book-photo-thumbnails';
        photos.forEach(photo => {
            const thumbnail = document.createElement('div');
            thumbnail.className = 'book-photo-thumbnail';
            thumbnail.setAttribute('data-photo-id', photo.id);

            const img = document.createElement('img');
            img.setAttribute('data-test', 'book-photo');
            img.src = `/api/photos/${photo.id}/image`;
            if (photo.rotation && photo.rotation !== 0) {
                img.style.transform = `rotate(${photo.rotation}deg)`;
            }
            thumbnail.appendChild(img);

            // Rotate CCW button (upper left) - arrow circle pointing left
            const rotateCcwBtn = document.createElement('button');
            rotateCcwBtn.className = 'photo-overlay-btn rotate-ccw-btn';
            rotateCcwBtn.innerHTML = '↺';
            rotateCcwBtn.title = 'Rotate counterclockwise 90 degrees';
            rotateCcwBtn.onclick = () => rotatePhotoCCW(bookId, photo.id);
            thumbnail.appendChild(rotateCcwBtn);

            // Rotate CW button (upper right) - arrow circle pointing right
            const rotateCwBtn = document.createElement('button');
            rotateCwBtn.className = 'photo-overlay-btn rotate-cw-btn';
            rotateCwBtn.innerHTML = '↻';
            rotateCwBtn.title = 'Rotate clockwise 90 degrees';
            rotateCwBtn.onclick = () => rotatePhotoCW(bookId, photo.id);
            thumbnail.appendChild(rotateCwBtn);

            // Delete button (top center, between rotates)
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'photo-overlay-btn delete-btn';
            deleteBtn.innerHTML = '🗑️';
            deleteBtn.title = 'Delete Photo';
            deleteBtn.onclick = () => deleteBookPhoto(bookId, photo.id);
            thumbnail.appendChild(deleteBtn);

            // Edit button (lower left)
            const editBtn = document.createElement('button');
            editBtn.className = 'photo-overlay-btn edit-btn';
            editBtn.innerHTML = '✏️';
            editBtn.title = 'Edit Photo';
            editBtn.onclick = () => editPhoto(bookId, photo.id);
            thumbnail.appendChild(editBtn);

            photosDiv.appendChild(thumbnail);
        });
    } else {
        photosContainer.style.display = 'none';
    }
}

async function deleteBookPhoto(bookId, photoId) {
    if (!confirm('Are you sure you want to delete this photo?')) return;
    try {
        await deleteData(`/api/books/${bookId}/photos/${photoId}`);
        const thumbnail = document.querySelector(`.book-photo-thumbnail[data-photo-id='${photoId}']`);
        if (thumbnail) {
            thumbnail.remove();
        }
        const photosDiv = document.getElementById('book-photos');
        if (photosDiv.childElementCount === 0) {
            document.getElementById('book-photos-container').style.display = 'none';
            document.getElementById('book-by-photo-btn').style.display = 'none';
        }
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to delete photo: ' + error.message);
    }
}

async function rotatePhotoCCW(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/rotate-ccw`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to rotate photo counterclockwise: ' + error.message);
    }
}

async function rotatePhotoCW(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/rotate-cw`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to rotate photo clockwise: ' + error.message);
    }
}

async function editPhoto(bookId, photoId) {
    // For now, since PhotoDto fields are not specified in UI, prompt for a potential field like caption
    // Assuming PhotoDto has a 'caption' field; adjust as needed
    const caption = prompt('Enter new caption for the photo (or leave blank):');
    if (caption === null) return; // Cancelled

    try {
        const photoDto = { caption: caption || null };
        await putData(`/api/books/${bookId}/photos/${photoId}`, photoDto);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to update photo: ' + error.message);
    }
}

async function addPhoto() {
    document.getElementById('photo-upload').click();
}

document.getElementById('photo-upload').addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const bookId = document.getElementById('current-book-id').value;
    if (!bookId) {
        showError('books', 'No book selected for photo upload.');
        return;
    }
    const formData = new FormData();
    formData.append('file', file);

    document.body.style.cursor = 'wait';

    try {
        await postData(`/api/books/${bookId}/photos`, formData, true);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        displayBookPhotos(photos, bookId);
        if (photos && photos.length > 0) {
            document.getElementById('book-by-photo-btn').style.display = 'inline-block';
            const currentId = document.getElementById('current-book-id').value;
            if (currentId) {
                document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(currentId);
            }
        }
        event.target.value = '';
        clearError('books');
        document.getElementById('book-photos-container').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showError('books', 'Failed to add photo: ' + error.message);
        event.target.value = '';
    } finally {
        document.body.style.cursor = 'default';
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
