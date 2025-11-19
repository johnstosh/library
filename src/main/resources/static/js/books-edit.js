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
    document.getElementById('book-author').value = '';
    document.getElementById('book-author-id').value = '';
    document.getElementById('book-library').selectedIndex = 0;
    document.getElementById('current-book-id').value = '';

    const btn = document.getElementById('add-book-btn');
    btn.textContent = 'Add Book';
    btn.onclick = addBook;

    document.getElementById('add-photo-btn').style.display = 'none';
    document.getElementById('add-photo-google-btn').style.display = 'none';
    document.getElementById('cancel-book-btn').style.display = 'none';
    document.getElementById('clone-book-btn').style.display = 'none';
    document.getElementById('book-by-photo-btn').style.display = 'none';
    document.getElementById('book-by-photo-btn').onclick = null;
    document.getElementById('title-author-from-photo-btn').style.display = 'none';
    document.getElementById('book-from-title-author-btn').style.display = 'none';
    document.getElementById('book-photos-container').style.display = 'none';
    showBookList(true);

    clearSuccess('books');
}

async function prepareNewBookForPhoto(title) {
    // Enter edit mode for a new book
    showBookList(false);

    // Set title to timestamp
    document.getElementById('new-book-title').value = title;

    try {
        // Get first author or create "John Doe" if none exist
        let authors = await fetchData('/api/authors');
        let authorId;
        if (authors && authors.length > 0) {
            authorId = authors[0].id;
        } else {
            // Create a default author
            const defaultAuthor = await postData('/api/authors', {
                name: 'John Doe',
                dateOfBirth: '',
                dateOfDeath: '',
                religiousAffiliation: '',
                birthCountry: '',
                nationality: '',
                briefBiography: 'Default author created for book-by-photo'
            });
            authorId = defaultAuthor.id;
            // Reload the authors dropdown
            await populateBookDropdowns();
        }

        // Get first library or create a default if none exist
        let libraries = await fetchData('/api/libraries');
        let libraryId;
        if (libraries && libraries.length > 0) {
            libraryId = libraries[0].id;
        } else {
            // Create a default library
            const defaultLibrary = await postData('/api/libraries', {
                name: 'Default Library',
                hostname: window.location.hostname
            });
            libraryId = defaultLibrary.id;
            // Reload the libraries dropdown
            await populateBookDropdowns();
        }

        // Set author name and ID
        document.getElementById('book-author-id').value = authorId;
        const author = allAuthors.find(a => a.id == authorId);
        document.getElementById('book-author').value = author ? author.name : '';

        // Set library dropdown
        document.getElementById('book-library').value = libraryId;

    // Scroll to bottom
    window.scrollTo(0, document.body.scrollHeight);

    // Save initial data to backend (minimal, just title/author/library)
    const initialData = {
        title: title,
        authorId: authorId,
        libraryId: libraryId,
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

        const createdBook = await postData('/api/books', initialData);
        document.getElementById('current-book-id').value = createdBook.id;
        // After save, make buttons available
        document.getElementById('add-photo-btn').style.display = 'inline-block';
        document.getElementById('add-photo-google-btn').style.display = 'inline-block';
        document.getElementById('cancel-book-btn').style.display = 'inline-block';
        document.getElementById('clone-book-btn').style.display = 'inline-block';
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
    let authorId = document.getElementById('book-author-id').value;
    const authorName = document.getElementById('book-author').value.trim();
    const libraryId = document.getElementById('book-library').value;
    if (!title || !libraryId) {
        showError('books', 'Title and library are required.');
        return;
    }
    try {
        // If author name is provided but no ID, create a new author
        if (authorName && !authorId) {
            const newAuthor = await postData('/api/authors', {
                name: authorName,
                dateOfBirth: '',
                dateOfDeath: '',
                religiousAffiliation: '',
                birthCountry: '',
                nationality: '',
                briefBiography: ''
            });
            authorId = newAuthor.id;
            // Update the hidden field and refresh dropdowns
            document.getElementById('book-author-id').value = authorId;
            await populateBookDropdowns();
        }

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
    // Convert ISO datetime to datetime-local format (YYYY-MM-DDTHH:mm)
    document.getElementById('new-book-added').value = data.dateAddedToLibrary ? data.dateAddedToLibrary.substring(0, 16) : '';
    document.getElementById('new-book-status').value = data.status || 'ACTIVE';
    document.getElementById('new-book-loc').value = data.locNumber || '';
    document.getElementById('new-book-status-reason').value = data.statusReason || '';

    // Set author name and ID
    document.getElementById('book-author-id').value = data.authorId || '';
    if (data.authorId) {
        const author = allAuthors.find(a => a.id == data.authorId);
        document.getElementById('book-author').value = author ? author.name : '';
    } else {
        document.getElementById('book-author').value = '';
    }

    document.getElementById('book-library').value = data.libraryId || '';
    document.getElementById('current-book-id').value = id;
    const btn = document.getElementById('add-book-btn');
    btn.textContent = 'Update Book';
    btn.onclick = () => updateBook(id);
    document.getElementById('add-photo-btn').style.display = 'inline-block';
    document.getElementById('add-photo-google-btn').style.display = 'inline-block';
    document.getElementById('cancel-book-btn').style.display = 'inline-block';
    document.getElementById('clone-book-btn').style.display = 'inline-block';

    const photos = await fetchData(`/api/books/${id}/photos`);
    await displayBookPhotos(photos, id);
    if (photos && photos.length > 0) {
        document.getElementById('book-by-photo-btn').style.display = 'inline-block';
        document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(id);
        document.getElementById('title-author-from-photo-btn').style.display = 'inline-block';
    }
    // Always show "Book from Title and Author" when editing
    document.getElementById('book-from-title-author-btn').style.display = 'inline-block';
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
        // Convert ISO datetime to datetime-local format (YYYY-MM-DDTHH:mm)
        if (updatedBook.dateAddedToLibrary) document.getElementById('new-book-added').value = updatedBook.dateAddedToLibrary.substring(0, 16);
        if (updatedBook.status) document.getElementById('new-book-status').value = updatedBook.status;
        if (updatedBook.locNumber && updatedBook.locNumber.trim() !== '') document.getElementById('new-book-loc').value = updatedBook.locNumber;
        if (updatedBook.statusReason && updatedBook.statusReason.trim() !== '') document.getElementById('new-book-status-reason').value = updatedBook.statusReason;
        if (updatedBook.libraryId) document.getElementById('book-library').value = updatedBook.libraryId;

        // Repopulate dropdowns to include any new authors
        await populateBookDropdowns();

        // Set author name and ID after repopulating
        if (updatedBook.authorId) {
            document.getElementById('book-author-id').value = updatedBook.authorId;
            const author = allAuthors.find(a => a.id == updatedBook.authorId);
            document.getElementById('book-author').value = author ? author.name : '';
        }


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
    let authorId = document.getElementById('book-author-id').value;
    const authorName = document.getElementById('book-author').value.trim();
    const libraryId = document.getElementById('book-library').value;
    if (!title || !libraryId) {
        showError('books', 'Title and library are required.');
        return;
    }
    try {
        // If author name is provided but no ID, create a new author
        if (authorName && !authorId) {
            const newAuthor = await postData('/api/authors', {
                name: authorName,
                dateOfBirth: '',
                dateOfDeath: '',
                religiousAffiliation: '',
                birthCountry: '',
                nationality: '',
                briefBiography: ''
            });
            authorId = newAuthor.id;
            // Update the hidden field and refresh dropdowns
            document.getElementById('book-author-id').value = authorId;
            await populateBookDropdowns();
        }

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

async function cloneBook() {
    const bookId = document.getElementById('current-book-id').value;
    if (!bookId) {
        showError('books', 'No book selected to clone.');
        return;
    }

    try {
        document.body.style.cursor = 'wait';
        const clonedBook = await postData(`/api/books/${bookId}/clone`);

        // Reload the books list
        await loadBooks();
        await populateLoanDropdowns();

        // Open the cloned book in edit mode
        await editBook(clonedBook.id);

        showSuccess('books', `Book cloned successfully as "${clonedBook.title}"`);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to clone book: ' + error.message);
    } finally {
        document.body.style.cursor = 'default';
    }
}

async function getTitleAuthorFromPhoto() {
    const bookId = document.getElementById('current-book-id').value;
    if (!bookId) {
        showError('books', 'No book selected.');
        return;
    }

    const titleAuthorBtn = document.getElementById('title-author-from-photo-btn');
    titleAuthorBtn.disabled = true;
    document.body.style.cursor = 'wait';

    try {
        const result = await putData(`/api/books/${bookId}/title-author-from-photo`, {}, true);

        // Update form fields with title and author
        if (result.title && result.title.trim() !== '') {
            document.getElementById('new-book-title').value = result.title;
        }

        // Repopulate dropdowns to include any new authors
        await populateBookDropdowns();

        // Set author name and ID
        if (result.authorId) {
            document.getElementById('book-author-id').value = result.authorId;
            const author = allAuthors.find(a => a.id == result.authorId);
            document.getElementById('book-author').value = author ? author.name : '';
        }

        showSuccess('books', 'Title and author extracted from photo successfully');
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to extract title and author from photo: ' + error.message);
    } finally {
        titleAuthorBtn.disabled = false;
        document.body.style.cursor = 'default';
    }
}

async function getBookFromTitleAuthor() {
    const bookId = document.getElementById('current-book-id').value;
    if (!bookId) {
        showError('books', 'No book selected.');
        return;
    }

    const title = document.getElementById('new-book-title').value.trim();
    const authorName = document.getElementById('book-author').value.trim();

    if (!title) {
        showError('books', 'Please enter a title first.');
        return;
    }

    const bookFromTitleBtn = document.getElementById('book-from-title-author-btn');
    bookFromTitleBtn.disabled = true;
    document.body.style.cursor = 'wait';

    try {
        const result = await putData(`/api/books/${bookId}/book-from-title-author`, {
            title: title,
            authorName: authorName
        }, true);

        // Intelligently update form fields
        if (result.title && result.title.trim() !== '') document.getElementById('new-book-title').value = result.title;
        if (result.publicationYear) document.getElementById('new-book-year').value = result.publicationYear;
        if (result.publisher && result.publisher.trim() !== '') document.getElementById('new-book-publisher').value = result.publisher;
        if (result.plotSummary && result.plotSummary.trim() !== '') document.getElementById('new-book-summary').value = result.plotSummary;
        if (result.relatedWorks && result.relatedWorks.trim() !== '') document.getElementById('new-book-related').value = result.relatedWorks;
        if (result.detailedDescription && result.detailedDescription.trim() !== '') document.getElementById('new-book-description').value = result.detailedDescription;
        if (result.locNumber && result.locNumber.trim() !== '') document.getElementById('new-book-loc').value = result.locNumber;

        // Repopulate dropdowns to include any new authors
        await populateBookDropdowns();

        // Set author name and ID after repopulating
        if (result.authorId) {
            document.getElementById('book-author-id').value = result.authorId;
            const author = allAuthors.find(a => a.id == result.authorId);
            document.getElementById('book-author').value = author ? author.name : '';
        }

        showSuccess('books', 'Book metadata generated from title and author successfully');
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to generate book metadata: ' + error.message);
    } finally {
        bookFromTitleBtn.disabled = false;
        document.body.style.cursor = 'default';
    }
}

// Store all authors for autocomplete
let allAuthors = [];

async function populateBookDropdowns() {
    try {
        allAuthors = await fetchData('/api/authors');
        const authorDatalist = document.getElementById('author-datalist');
        authorDatalist.innerHTML = '';
        allAuthors.forEach(author => {
            const option = document.createElement('option');
            option.value = author.name;
            option.setAttribute('data-author-id', author.id);
            authorDatalist.appendChild(option);
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

// Initialize event listeners for author autocomplete
document.addEventListener('DOMContentLoaded', function() {
    const authorInput = document.getElementById('book-author');
    const authorIdInput = document.getElementById('book-author-id');
    const clearAuthorBtn = document.getElementById('clear-author-btn');

    // Handle author selection
    if (authorInput) {
        authorInput.addEventListener('input', function() {
            const inputValue = this.value.trim();

            // Find exact match first
            let matchedAuthor = allAuthors.find(author => author.name === inputValue);

            if (matchedAuthor) {
                authorIdInput.value = matchedAuthor.id;
            } else {
                // Clear the ID if no exact match
                authorIdInput.value = '';
            }
        });

        // Handle blur to validate selection
        authorInput.addEventListener('blur', function() {
            const inputValue = this.value.trim();
            if (inputValue === '') {
                authorIdInput.value = '';
                return;
            }

            // Check if the input matches an existing author
            const matchedAuthor = allAuthors.find(author => author.name === inputValue);
            if (!matchedAuthor) {
                // Allow custom author names (the backend might create them)
                authorIdInput.value = '';
            }
        });
    }

    // Handle clear button
    if (clearAuthorBtn) {
        clearAuthorBtn.addEventListener('click', function() {
            authorInput.value = '';
            authorIdInput.value = '';
            authorInput.focus();
        });
    }
});
