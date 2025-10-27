// (c) Copyright 2025 by Muczynski
async function loadBooks() {
    try {
        const books = await fetchData('/api/books');
        const tableBody = document.getElementById('book-list-body');
        tableBody.innerHTML = '';
        books.forEach(book => {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'book-item');
            row.setAttribute('data-entity-id', book.id);

            const photoCell = document.createElement('td');
            if (book.firstPhotoId) {
                const img = document.createElement('img');
                img.src = `/api/photos/${book.firstPhotoId}/thumbnail?width=50`;
                img.style.width = '50px';
                img.style.height = 'auto';
                if (book.firstPhotoRotation && book.firstPhotoRotation !== 0) {
                    img.style.transform = `rotate(${book.firstPhotoRotation}deg)`;
                }
                img.setAttribute('data-test', 'book-thumbnail');
                photoCell.appendChild(img);
            }
            row.appendChild(photoCell);

            const titleCell = document.createElement('td');
            const titleSpan = document.createElement('span');
            titleSpan.setAttribute('data-test', 'book-title');
            titleSpan.textContent = book.title;
            titleCell.appendChild(titleSpan);
            row.appendChild(titleCell);

            const loansCell = document.createElement('td');
            if (book.status === 'WITHDRAWN' || book.status === 'LOST') {
                loansCell.textContent = book.status.toLowerCase();
            } else if (book.loanCount > 0) {
                loansCell.textContent = book.loanCount;
            }
            row.appendChild(loansCell);

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

async function prepareNewBookForPhoto(title) {
    // Enter edit mode for a new book
    showBookList(false);

    // Set title to timestamp
    document.getElementById('new-book-title').value = title;

    // Set author to the first one (id 1, assuming populated)
    document.getElementById('book-author').value = '1';

    // Set library to the first one (id 1)
    document.getElementById('book-library').value = '1';

    // Scroll to bottom
    window.scrollTo(0, document.body.scrollHeight);

    // Save initial data to backend (minimal, just title/author/library)
    const initialData = {
        title: title,
        authorId: '1',
        libraryId: '1',
        publicationYear: '',
        publisher: '',
        plotSummary: '',
        relatedWorks: '',
        detailedDescription: '',
        dateAddedToLibrary: '',
        status: 'ACTIVE'
    };
    try {
        const createdBook = await postData('/api/books', initialData);
        document.getElementById('current-book-id').value = createdBook.id;
        // After save, make buttons available
        document.getElementById('add-photo-btn').style.display = 'inline-block';
        document.getElementById('cancel-book-btn').style.display = 'inline-block';
        document.getElementById('book-by-photo-btn').style.display = 'none'; // Hidden until photo added
        document.getElementById('book-by-photo-btn').onclick = null;

        // Scroll stays at bottom
        window.scrollTo(0, document.body.scrollHeight);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to prepare new book: ' + error.message);
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
    await displayBookPhotos(photos, id);
    if (photos && photos.length > 0) {
        document.getElementById('book-by-photo-btn').style.display = 'inline-block';
        document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(id);
    }
}

async function generateBookByPhoto(bookId) {
    const addBookBtn = document.getElementById('add-book-btn');
    const cancelBookBtn = document.getElementById('cancel-book-btn');
    const addPhotoBtn = document.getElementById('add-photo-btn');
    const bookByPhotoBtn = document.getElementById('book-by-photo-btn');

    addBookBtn.disabled = true;
    cancelBookBtn.disabled = true;
    addPhotoBtn.disabled = true;
    bookByPhotoBtn.disabled = true;

    document.body.style.cursor = 'wait';
    try {
        const updatedBook = await putData(`/api/books/${bookId}/book-by-photo`, {}, true);

        // Intelligently update form fields
        if (updatedBook.title && updatedBook.title.trim() !== '') document.getElementById('new-book-title').value = updatedBook.title;
        if (updatedBook.publicationYear) document.getElementById('new-book-year').value = updatedBook.publicationYear;
        if (updatedBook.publisher && updatedBook.publisher.trim() !== '') document.getElementById('new-book-publisher').value = updatedBook.publisher;
        if (updatedBook.plotSummary && updatedBook.plotSummary.trim() !== '') document.getElementById('new-book-summary').value = updatedBook.plotSummary;
        if (updatedBook.relatedWorks && updatedBook.relatedWorks.trim() !== '') document.getElementById('new-book-related').value = updatedBook.relatedWorks;
        if (updatedBook.detailedDescription && updatedBook.detailedDescription.trim() !== '') document.getElementById('new-book-description').value = updatedBook.detailedDescription;
        if (updatedBook.dateAddedToLibrary) document.getElementById('new-book-added').value = updatedBook.dateAddedToLibrary;
        if (updatedBook.status) document.getElementById('new-book-status').value = updatedBook.status;
        if (updatedBook.authorId) document.getElementById('book-author').value = updatedBook.authorId;
        if (updatedBook.libraryId) document.getElementById('book-library').value = updatedBook.libraryId;

        // Repopulate dropdowns to include any new authors
        await populateBookDropdowns();
        // Reselect the author, in case the dropdown was repopulated
        if (updatedBook.authorId) document.getElementById('book-author').value = updatedBook.authorId;


        showSuccess('books', 'Book metadata generated successfully using AI');
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to generate book metadata: ' + error.message);
    } finally {
        addBookBtn.disabled = false;
        cancelBookBtn.disabled = false;
        addPhotoBtn.disabled = false;
        bookByPhotoBtn.disabled = false;
        document.body.style.cursor = 'default';
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
        const promises = [];
        photosContainer.style.display = 'block';
        photosDiv.className = 'book-photo-thumbnails';
        photos.forEach((photo, index) => {
            const thumbnail = document.createElement('div');
            thumbnail.className = 'book-photo-thumbnail';
            thumbnail.setAttribute('data-photo-id', photo.id);

            const img = document.createElement('img');
            img.setAttribute('data-test', 'book-photo');

            const loadPromise = new Promise(resolve => {
                img.onload = function() {
                    if (photo.rotation === 90 || photo.rotation === 270) {
                        const thumbnailContainer = this.parentElement;
                        const ratio = this.naturalHeight / this.naturalWidth;
                        const newWidth = thumbnailContainer.offsetHeight;
                        const newHeight = newWidth / ratio;

                        thumbnailContainer.style.width = `${newWidth}px`;
                        thumbnailContainer.style.height = `${newHeight}px`;
                        thumbnailContainer.style.maxWidth = `${newWidth}px`;

                        this.style.width = `${newHeight}px`;
                        this.style.height = `${newWidth}px`;

                        this.style.transformOrigin = 'bottom left';
                        if (photo.rotation === 270) {
                             this.style.transform = `rotate(${photo.rotation}deg) translateY(100%)`;
                        }
                    }
                    resolve();
                };
            });
            promises.push(loadPromise);

            img.src = `/api/photos/${photo.id}/thumbnail?width=300`;
            if (photo.rotation && photo.rotation !== 0) {
                img.style.transform = `rotate(${photo.rotation}deg)`;
            }
            thumbnail.appendChild(img);

            // Rotate CCW button (upper left)
            const rotateCcwBtn = document.createElement('button');
            rotateCcwBtn.className = 'photo-overlay-btn rotate-ccw-btn';
            rotateCcwBtn.innerHTML = '↺';
            rotateCcwBtn.title = 'Rotate counterclockwise 90 degrees';
            rotateCcwBtn.onclick = () => rotatePhotoCCW(bookId, photo.id);
            thumbnail.appendChild(rotateCcwBtn);

            // Rotate CW button (upper right)
            const rotateCwBtn = document.createElement('button');
            rotateCwBtn.className = 'photo-overlay-btn rotate-cw-btn';
            rotateCwBtn.innerHTML = '↻';
            rotateCwBtn.title = 'Rotate clockwise 90 degrees';
            rotateCwBtn.onclick = () => rotatePhotoCW(bookId, photo.id);
            thumbnail.appendChild(rotateCwBtn);

            // Delete button (top center)
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'photo-overlay-btn delete-btn';
            deleteBtn.innerHTML = '🗑️';
            deleteBtn.title = 'Delete Photo';
            deleteBtn.onclick = () => deleteBookPhoto(bookId, photo.id);
            thumbnail.appendChild(deleteBtn);

            // Edit button (bottom left, near move-left)
            const editBtn = document.createElement('button');
            editBtn.className = 'photo-overlay-btn edit-btn';
            editBtn.innerHTML = '✏️';
            editBtn.title = 'Edit Photo';
            editBtn.onclick = () => editPhoto(bookId, photo.id);
            thumbnail.appendChild(editBtn);

            // Move Left button (bottom left)
            if (index > 0) {
                const moveLeftBtn = document.createElement('button');
                moveLeftBtn.className = 'photo-overlay-btn move-left-btn';
                moveLeftBtn.innerHTML = '←';
                moveLeftBtn.title = 'Move Photo Left';
                moveLeftBtn.onclick = () => movePhotoLeft(bookId, photo.id);
                thumbnail.appendChild(moveLeftBtn);
            }

            // Move Right button (bottom right)
            if (index < photos.length - 1) {
                const moveRightBtn = document.createElement('button');
                moveRightBtn.className = 'photo-overlay-btn move-right-btn';
                moveRightBtn.innerHTML = '→';
                moveRightBtn.title = 'Move Photo Right';
                moveRightBtn.onclick = () => movePhotoRight(bookId, photo.id);
                thumbnail.appendChild(moveRightBtn);
            }

            photosDiv.appendChild(thumbnail);
        });
        return Promise.all(promises);
    } else {
        photosContainer.style.display = 'none';
        return Promise.resolve();
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
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to rotate photo counterclockwise: ' + error.message);
    }
}

async function rotatePhotoCW(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/rotate-cw`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
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
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to update photo: ' + error.message);
    }
}

async function movePhotoLeft(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/move-left`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to move photo left: ' + error.message);
    }
}

async function movePhotoRight(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/move-right`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to move photo right: ' + error.message);
    }
}

async function addPhoto() {
    document.getElementById('photo-upload').click();
}

async function handlePhotoUpload(event) {
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
        await displayBookPhotos(photos, bookId);
        if (photos && photos.length > 0) {
            document.getElementById('book-by-photo-btn').style.display = 'inline-block';
            const currentId = document.getElementById('current-book-id').value;
            if (currentId) {
                document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(currentId);
            }
        }
        event.target.value = ''; // Reset file input
        clearError('books');
        // On success, scroll to the bottom to show the new photo
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    } catch (error) {
        showError('books', 'Failed to add photo: ' + error.message);
        // On error, scroll to the top to show the error message
        window.scrollTo({ top: 0, behavior: 'smooth' });
        event.target.value = ''; // Reset file input
    } finally {
        document.body.style.cursor = 'default';
    }
}

document.getElementById('photo-upload').addEventListener('change', handlePhotoUpload);

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
