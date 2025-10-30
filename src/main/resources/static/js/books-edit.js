// (c) Copyright 2025 by Muczynski
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
    document.getElementById('new-book-loc').value = '';
    document.getElementById('new-book-status-reason').value = '';
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
        status: 'ACTIVE',
        locNumber: '',
        statusReason: ''
    };
    try {
        const createdBook = await postData('/api/books', initialData);
        document.getElementById('current-book-id').value = createdBook.id;
        // After save, make buttons available
        document.getElementById('add-photo-btn').style.display = 'inline-block';
        document.getElementById('cancel-book-btn').style.display = 'inline-block';
        document.getElementById('book-by-photo-btn').style.display = 'none'; // Hidden until photo added
        document.getElementById('book-by-photo-btn').onclick = null;

        // Change button to Update Book since the book is now created
        const btn = document.getElementById('add-book-btn');
        btn.textContent = 'Update Book';
        btn.onclick = () => updateBook(createdBook.id);

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
    const locNumber = document.getElementById('new-book-loc').value;
    const statusReason = document.getElementById('new-book-status-reason').value;
    const authorId = document.getElementById('book-author').value;
    const libraryId = document.getElementById('book-library').value;
    if (!title || !authorId || !libraryId) {
        showError('books', 'Title, author, and library are required.');
        return;
    }
    try {
        await postData('/api/books', { title, publicationYear, publisher, plotSummary, relatedWorks, detailedDescription, dateAddedToLibrary, status, locNumber, statusReason, authorId, libraryId });
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
    document.getElementById('new-book-loc').value = data.locNumber || '';
    document.getElementById('new-book-status-reason').value = data.statusReason || '';
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
        if (updatedBook.locNumber && updatedBook.locNumber.trim() !== '') document.getElementById('new-book-loc').value = updatedBook.locNumber;
        if (updatedBook.statusReason && updatedBook.statusReason.trim() !== '') document.getElementById('new-book-status-reason').value = updatedBook.statusReason;
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
    const locNumber = document.getElementById('new-book-loc').value;
    const statusReason = document.getElementById('new-book-status-reason').value;
    const authorId = document.getElementById('book-author').value;
    const libraryId = document.getElementById('book-library').value;
    if (!title || !authorId || !libraryId) {
        showError('books', 'Title, author, and library are required.');
        return;
    }
    try {
        await putData(`/api/books/${id}`, { title, publicationYear, publisher, plotSummary, relatedWorks, detailedDescription, dateAddedToLibrary, status, locNumber, statusReason, authorId, libraryId });
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

async function createBookByPhoto() {
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
    await prepareNewBookForPhoto(title);
}
